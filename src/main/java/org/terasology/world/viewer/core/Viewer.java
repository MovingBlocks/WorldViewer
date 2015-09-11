/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.world.viewer.core;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.math.Region3i;
import org.terasology.math.TeraMath;
import org.terasology.math.geom.BaseVector2i;
import org.terasology.math.geom.ImmutableVector2i;
import org.terasology.math.geom.Rect2i;
import org.terasology.math.geom.Vector2i;
import org.terasology.math.geom.Vector3i;
import org.terasology.rendering.nui.HorizontalAlign;
import org.terasology.world.chunks.ChunkConstants;
import org.terasology.world.generation.Region;
import org.terasology.world.generation.World;
import org.terasology.world.generator.WorldGenerator;
import org.terasology.world.viewer.ThreadSafeRegion;
import org.terasology.world.viewer.camera.Camera;
import org.terasology.world.viewer.camera.CameraKeyController;
import org.terasology.world.viewer.camera.CameraMouseController;
import org.terasology.world.viewer.camera.RepaintingCameraListener;
import org.terasology.world.viewer.color.ColorModels;
import org.terasology.world.viewer.config.ViewConfig;
import org.terasology.world.viewer.gui.CursorPositionListener;
import org.terasology.world.viewer.gui.RepaintingMouseListener;
import org.terasology.world.viewer.layers.FacetLayer;
import org.terasology.world.viewer.overlay.GridOverlay;
import org.terasology.world.viewer.overlay.Overlay;
import org.terasology.world.viewer.overlay.ScreenOverlay;
import org.terasology.world.viewer.overlay.TextOverlay;
import org.terasology.world.viewer.overlay.TooltipOverlay;
import org.terasology.world.viewer.overlay.WorldOverlay;

import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.math.IntMath;

/**
 * The main viewer component
 */
public final class Viewer extends JComponent {

    private static final Logger logger = LoggerFactory.getLogger(Viewer.class);

    private static final int TILE_SIZE_X = ChunkConstants.SIZE_X * 4;
    private static final int TILE_SIZE_Y = ChunkConstants.SIZE_Z * 4;

    private static final long serialVersionUID = 4178713176841691478L;

    private final BufferedImage dummyImg;
    private final BufferedImage failedImg;

    /**
     * Contains both queued tasks and those that are in progress.
     */
    private final Collection<Future<BufferedImage>> taskList;
    private final ThreadPoolExecutor threadPool;

    private final LoadingCache<ImmutableVector2i, Region> regionCache;
    private final LoadingCache<ImmutableVector2i, BufferedImage> imageCache;

    private final Camera camera = new Camera();

    private final CursorPositionListener curPosListener;

    private final ViewConfig viewConfig;

    private final Deque<WorldOverlay> worldOverlays = Lists.newLinkedList();
    private final Deque<ScreenOverlay> screenOverlays = Lists.newLinkedList();

    private WorldGenerator worldGen;
    private List<FacetLayer> facetLayers;

    /**
     * @param viewConfig the view config
     * @param cacheSize maximum number of cached tiles
     */
    public Viewer(ViewConfig viewConfig, int cacheSize) {
        this.viewConfig = viewConfig;

        int minThreads = Runtime.getRuntime().availableProcessors() * 2;
        int maxThreads = Runtime.getRuntime().availableProcessors() * 2;

        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(); // unlimited queue size
        TileThreadFactory threadFactory = new TileThreadFactory();
        threadPool = new ThreadPoolExecutor(minThreads, maxThreads, 60, TimeUnit.SECONDS, queue, threadFactory);
        taskList = Sets.newSetFromMap(new ConcurrentHashMap<>(cacheSize)); // estimated size

        CacheLoader<ImmutableVector2i, Region> regionLoader = new CacheLoader<ImmutableVector2i, Region>() {

            @Override
            public Region load(ImmutableVector2i tilePos) {
                Region region = createRegion(tilePos);
                return region;
            }
        };

        CacheLoader<ImmutableVector2i, BufferedImage> imageLoader = new CacheLoader<ImmutableVector2i, BufferedImage>() {

            @Override
            public BufferedImage load(ImmutableVector2i pos) throws Exception {
                enqueueTile(pos);
                return dummyImg;
            }
        };

        regionCache = CacheBuilder.newBuilder().maximumSize(cacheSize).build(regionLoader);
        imageCache = CacheBuilder.newBuilder().maximumSize(cacheSize).build(imageLoader);

        Vector2i camPos = viewConfig.getCamPos();
        camera.translate(camPos.getX(), camPos.getY());
        camera.setZoom(viewConfig.getZoomFactor());
        camera.addListener(new RepaintingCameraListener(this));

        GridOverlay gridOverlay = new GridOverlay(TILE_SIZE_X, TILE_SIZE_Y);
        worldOverlays.addLast(gridOverlay);

        TextOverlay zoomOverlay = new TextOverlay(() -> String.format("Zoom: %3d%%", (int) (camera.getZoom() * 100)));
        zoomOverlay.setHorizontalAlign(HorizontalAlign.RIGHT);
        zoomOverlay.setMargins(5, 5, 5, 5);
        zoomOverlay.setInsets(8, 5, 5, 5);
        zoomOverlay.setFont(new Font("Dialog", Font.BOLD, 15));
        zoomOverlay.setFrame(new Color(192, 192, 192, 128));
        zoomOverlay.setBackground(new Color(92, 92, 92, 160));
        zoomOverlay.setVisible(false);
        camera.addListener(new ZoomOverlayUpdater(this, zoomOverlay));
        screenOverlays.add(zoomOverlay);
        ScreenOverlay tooltipOverlay = new TooltipOverlay(screen -> {
            Rect2i visWorld = camera.getVisibleArea(getWidth(), getHeight());
            return getTooltip(toWorld(visWorld, screen));
            });
        screenOverlays.add(tooltipOverlay);

        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        // add camera controls
        KeyAdapter keyCameraController = new CameraKeyController(camera);
        MouseAdapter mouseCameraController = new CameraMouseController(camera);
        addKeyListener(keyCameraController);
        addMouseListener(mouseCameraController);
        addMouseMotionListener(mouseCameraController);
        addMouseWheelListener(mouseCameraController);

        // add tooltip mouse listeners
        curPosListener = new CursorPositionListener();
        addMouseMotionListener(curPosListener);
        addMouseListener(curPosListener);

        // TODO: the listener should be attached to the cursor position Point
        MouseAdapter repaintListener = new RepaintingMouseListener(this);
        addMouseListener(repaintListener);
        addMouseMotionListener(repaintListener);

        dummyImg = createStaticImage(TILE_SIZE_X, TILE_SIZE_Y, null);
        failedImg = createStaticImage(TILE_SIZE_X, TILE_SIZE_Y, "FAILED");
    }

    private static BufferedImage createStaticImage(int width, int height, String text) {

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        if (text != null) {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 20f));
            FontMetrics fm = g.getFontMetrics();
            int ty = height / 2 - (fm.getHeight() / 2 - fm.getAscent());
            int tx = width / 2 - fm.stringWidth(text) / 2;
            g.drawString(text, tx, ty);
        }
        g.setColor(Color.GRAY);
        g.drawRect(0, 0, width - 1, height - 1);
        g.dispose();
        return image;
    }

    /**
     * @return the number of tiles that is currently waiting for being processed
     */
    public int getPendingTiles() {
        return taskList.size();
    }

    /**
     * @return the number of tile images in the cache
     */
    public int getCachedTiles() {
        return (int) imageCache.size();
    }

    public Camera getCamera() {
        return camera;
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    @Override
    public void paint(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;
        AffineTransform orgTrans = g.getTransform();

        Rect2i visWorld = camera.getVisibleArea(getWidth(), getHeight());
        Rect2i visTiles = worldToTileArea(visWorld);

        g.scale(camera.getZoom(), camera.getZoom());
        g.translate(-visWorld.minX(), -visWorld.minY());

        drawTiles(g, visTiles);

        Point curPos = curPosListener.getCursorPosition();

        ImmutableVector2i screenCursor = null;
        ImmutableVector2i worldCursor = null;
        if (curPos != null) {
            screenCursor = new ImmutableVector2i(curPos.x, curPos.y);
            worldCursor = toWorld(visWorld, screenCursor);
        }

        // draw world overlays
        for (Overlay ovly : worldOverlays) {
            if (ovly.isVisible()) {
                ovly.render(g, visWorld, worldCursor);
            }
        }

        g.setTransform(orgTrans);

        // draw screen overlays
        Rect2i windowRect = Rect2i.createFromMinAndSize(0, 0, getWidth(), getHeight());
        for (Overlay ovly : screenOverlays) {
            if (ovly.isVisible()) {
                ovly.render(g, windowRect, screenCursor);
            }
        }
    }

    private ImmutableVector2i toWorld(Rect2i visWorld, BaseVector2i screen) {
        int wx = visWorld.minX() + TeraMath.floorToInt(screen.getX() / camera.getZoom());
        int wy = visWorld.minY() + TeraMath.floorToInt(screen.getY() / camera.getZoom());
        return new ImmutableVector2i(wx, wy);
    }

    /**
     * @param wg the world generator to use
     * @param newLayers the facet config
     */
    public void setWorldGen(WorldGenerator wg, List<FacetLayer> newLayers) {
        this.worldGen = wg;
        this.facetLayers = newLayers;

        // clear tile cache and repaint if any of the facet configs has changed
        for (FacetLayer layer : newLayers) {
            layer.addObserver(l -> updateImageCache());
        }

        invalidateWorld();
    }

    public void invalidateWorld() {
        worldGen.initialize();

        regionCache.invalidateAll();
        updateImageCache();
     }

    public void close() {
        int cx = (int) camera.getPos().getX();
        int cy = (int) camera.getPos().getY();

        viewConfig.setCamPos(new Vector2i(cx, cy));
        viewConfig.setZoomFactor(camera.getZoom());

        threadPool.shutdownNow();
    }

    private static Rect2i worldToTileArea(Rect2i area) {
        int chunkMinX = IntMath.divide(area.minX(), TILE_SIZE_X, RoundingMode.FLOOR);
        int chunkMinZ = IntMath.divide(area.minY(), TILE_SIZE_Y, RoundingMode.FLOOR);

        int chunkMaxX = IntMath.divide(area.maxX(), TILE_SIZE_X, RoundingMode.FLOOR);
        int chunkMaxZ = IntMath.divide(area.maxY(), TILE_SIZE_Y, RoundingMode.FLOOR);

        return Rect2i.createFromMinAndMax(chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ);
    }

    private void drawTiles(Graphics2D g, Rect2i visChunks) {

        Object hint;

        if (camera.getZoom() < 1) {
            // Render with bi-linear interpolation when zoomed out
            hint = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
        } else {
            // Render with nearest neighbor interpolation when zoomed in (or at 100%)
            hint = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
        }
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);

        for (int z = visChunks.minY(); z <= visChunks.maxY(); z++) {
            for (int x = visChunks.minX(); x <= visChunks.maxX(); x++) {
                ImmutableVector2i pos = new ImmutableVector2i(x, z);
                BufferedImage image = imageCache.getUnchecked(pos);
                g.drawImage(image, x * TILE_SIZE_X, z * TILE_SIZE_Y, null);
            }
        }

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    }

    private Region getRegion(BaseVector2i pos) {

        int tileX = IntMath.divide(pos.getX(), TILE_SIZE_X, RoundingMode.FLOOR);
        int tileY = IntMath.divide(pos.getY(), TILE_SIZE_Y, RoundingMode.FLOOR);

        ImmutableVector2i tilePos = new ImmutableVector2i(tileX, tileY);
        Region region = regionCache.getUnchecked(tilePos);
        return region;
    }

    private String getTooltip(BaseVector2i world) {
        Region region = getRegion(world);

        StringBuffer sb = new StringBuffer();
        for (FacetLayer layer : facetLayers) {
            if (layer.isVisible()) {
                try {
                    String layerText = layer.getWorldText(region, world.getX(), world.getY());
                    if (layerText != null) {
                        sb.append("\n").append(layerText);
                    }
                } catch (Exception e) {
                    sb.append("\n<failed>");
                }
            }
        }

        String tooltip = String.format("%d / %d%s", world.getX(), world.getY(), sb.toString());
        return tooltip;
    }

    private Region createRegion(ImmutableVector2i chunkPos) {

        int vertChunks = 4; // 4 chunks high (relevant for trees, etc)

        int minX = chunkPos.getX() * TILE_SIZE_X;
        int minZ = chunkPos.getY() * TILE_SIZE_Y;
        int height = vertChunks * ChunkConstants.SIZE_Y;
        Region3i area3d = Region3i.createFromMinAndSize(new Vector3i(minX, 0, minZ), new Vector3i(TILE_SIZE_X, height, TILE_SIZE_Y));
        World world = worldGen.getWorld();

        // The region needs to be thread-safe, since the rendering of the tooltip
        // might access Region.getFacet() at the same time as a thread from the thread pool
        // that uses it to render to a BufferedImage.
        // This is often irrelevant, but composed facets such as Perlin's surface height facet,
        // which consists of the ground layer plus hills and mountains plus rivers
        // the method could return a partly created facet if accessed in parallel.
        Region region = new ThreadSafeRegion(world.getWorldData(area3d));

        return region;
    }

    /**
     * Called whenever a facet layer configuration changes
     */
    private void updateImageCache() {
        for (Future<?> task : taskList) {
            task.cancel(true);
        }

        Set<ImmutableVector2i> cachedTiles = imageCache.asMap().keySet();
        Set<ImmutableVector2i> oldTiles = new HashSet<>(cachedTiles);

        Rect2i visWorld = camera.getVisibleArea(getWidth(), getHeight());
        Rect2i visTileArea = worldToTileArea(visWorld);

        List<ImmutableVector2i> visTiles = new ArrayList<>(visTileArea.area());

        // the iterator vector cannot be used for permanent storage - it must be copied
        for (BaseVector2i pos : visTileArea.contents()) {
            visTiles.add(new ImmutableVector2i(pos));
        }

        // shuffle the order of new tasks
        // If the queue is cleared repeatedly before all tasks are run
        // some tiles will updated only in the last iteration
        // Plus, it looks nicer :-)
        Collections.shuffle(visTiles);

        for (ImmutableVector2i tile : visTiles) {
            oldTiles.remove(tile);
            enqueueTile(tile);
        }

        imageCache.invalidateAll(oldTiles);
    }

    private void enqueueTile(ImmutableVector2i pos) {
        RunnableFuture<BufferedImage> task = new FutureTask<BufferedImage>(new UpdateImageCache(pos)) {

            @Override
            protected void done() {
                if (!isCancelled()) {
                    BufferedImage result;
                    try {
                        result = get();
                    } catch (ExecutionException | InterruptedException e) {
                        logger.error("Could not rasterize tile {}", pos, e);
                        result = failedImg;
                    }
                    imageCache.put(pos, result);
                    repaint();
                }
                taskList.remove(this);
            }
        };
        taskList.add(task);
        threadPool.execute(task);
    }

    /**
     * Note: this method must be thread-safe!
     * @param region the thread-safe region
     * @return an image of that region
     */
    BufferedImage rasterize(Region region) {

        Vector3i extent = region.getRegion().size();
        int width = extent.x;
        int height = extent.z;

        DirectColorModel colorModel = ColorModels.ARGB;

        int[] masks = colorModel.getMasks();
        DataBufferInt imageBuffer = new DataBufferInt(width * height);
        WritableRaster raster = Raster.createPackedRaster(imageBuffer, width, height, width, masks, null);
        BufferedImage image = new BufferedImage(colorModel, raster, false, null);

        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        try {
            Stopwatch sw = Stopwatch.createStarted();

            for (FacetLayer layer : facetLayers) {
                if (layer.isVisible()) {
                    layer.render(image, region);
                }
            }

            if (logger.isTraceEnabled()) {
                logger.trace("Rendered region in {}ms.", sw.elapsed(TimeUnit.MILLISECONDS));
            }
        } finally {
            g.dispose();
        }

        return image;
    }

    private class UpdateImageCache implements Callable<BufferedImage> {

        private final ImmutableVector2i pos;

        public UpdateImageCache(ImmutableVector2i pos) {
            this.pos = pos;
        }

        @Override
        public BufferedImage call() {
            Region region = regionCache.getUnchecked(pos);
            BufferedImage image;
            image = rasterize(region);
            return image;
        }
    }
}
