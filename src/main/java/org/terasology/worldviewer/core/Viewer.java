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

package org.terasology.worldviewer.core;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.JComponent;

import org.terasology.math.Rect2i;
import org.terasology.math.Region3i;
import org.terasology.math.TeraMath;
import org.terasology.math.Vector2i;
import org.terasology.math.geom.Vector3i;
import org.terasology.rendering.nui.HorizontalAlign;
import org.terasology.world.chunks.ChunkConstants;
import org.terasology.world.generation.Region;
import org.terasology.world.generation.World;
import org.terasology.world.generator.WorldGenerator;
import org.terasology.worldviewer.ThreadSafeRegion;
import org.terasology.worldviewer.camera.Camera;
import org.terasology.worldviewer.camera.CameraKeyController;
import org.terasology.worldviewer.camera.CameraMouseController;
import org.terasology.worldviewer.camera.RepaintingCameraListener;
import org.terasology.worldviewer.config.ViewConfig;
import org.terasology.worldviewer.gui.CursorPositionListener;
import org.terasology.worldviewer.gui.RepaintingMouseListener;
import org.terasology.worldviewer.gui.Tooltip;
import org.terasology.worldviewer.layers.FacetLayer;
import org.terasology.worldviewer.overlay.GridOverlay;
import org.terasology.worldviewer.overlay.Overlay;
import org.terasology.worldviewer.overlay.TextOverlay;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.math.IntMath;

/**
 * The main viewer component
 * @author Martin Steiger
 */
public final class Viewer extends JComponent implements AutoCloseable {

    private static final int TILE_SIZE_X = ChunkConstants.SIZE_X * 4;
    private static final int TILE_SIZE_Y = ChunkConstants.SIZE_Z * 4;

    private static final long serialVersionUID = 4178713176841691478L;

    private final BufferedImage dummyImg = new BufferedImage(TILE_SIZE_X, TILE_SIZE_Y, BufferedImage.TYPE_INT_RGB);

    private final int numThreads = Runtime.getRuntime().availableProcessors();
    private final LinkedBlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>();
    private final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(numThreads, numThreads * 2,
            1L, TimeUnit.SECONDS, workQueue, Executors.defaultThreadFactory());

    private final CacheLoader<Vector2i, Region> regionLoader = new CacheLoader<Vector2i, Region>() {

        @Override
        public Region load(final Vector2i tilePos) {
            Region region = createRegion(tilePos);
            return region;
        }
    };

    private final CacheLoader<Region, BufferedImage> imageLoader = new CacheLoader<Region, BufferedImage>() {

        @Override
        public BufferedImage load(Region region) throws Exception {
            threadPool.execute(new UpdateImageCache(region));
            return dummyImg;
        }
    };

    private final LoadingCache<Vector2i, Region> regionCache;
    private final LoadingCache<Region, BufferedImage> imageCache;

    private final Camera camera = new Camera();
    private final WorldGenerator worldGen;

    private final CursorPositionListener curPosListener;

    private final ViewConfig viewConfig;

    private final Deque<Overlay> worldOverlays = Lists.newLinkedList();
    private final Deque<Overlay> screenOverlays = Lists.newLinkedList();

    private final List<FacetLayer> facetLayers;

    /**
     * @param wg the world generator to use
     * @param facetLayers the facet config
     * @param viewConfig the view config
     * @param cacheSize maximum number of cached tiles
     */
    public Viewer(WorldGenerator wg, List<FacetLayer> facetLayers, ViewConfig viewConfig, int cacheSize) {
        this.worldGen = wg;
        this.viewConfig = viewConfig;
        this.facetLayers = facetLayers;

        regionCache = CacheBuilder.newBuilder().maximumSize(cacheSize).build(regionLoader);
        imageCache = CacheBuilder.newBuilder().maximumSize(cacheSize).build(imageLoader);

        camera.addListener(new RepaintingCameraListener(this));
        Vector2i camPos = viewConfig.getCamPos();
        camera.translate(camPos.getX(), camPos.getY());

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

        Graphics2D g = dummyImg.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, dummyImg.getWidth(), dummyImg.getHeight());
        g.dispose();

        // clear tile cache and repaint if any of the facet configs has changed
        for (FacetLayer layer : facetLayers) {
            layer.addObserver(l -> updateImageCache());
        }
    }

    /**
     * @return the number of tiles that is currently waiting for being processed
     */
    public int getPendingTiles() {
        return workQueue.size();
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

        Rect2i visWorld = getVisibleArea(camera, getWidth(), getHeight());
        Rect2i visChunks = toChunkArea(visWorld);

        g.scale(camera.getZoom(), camera.getZoom());
        g.translate(-visWorld.minX(), -visWorld.minY());

        drawTiles(g, visChunks);

        // draw world overlays
        for (Overlay ovly : worldOverlays) {
            if (ovly.isVisible()) {
                ovly.render(g, visWorld);
            }
        }

        g.setTransform(orgTrans);

        // draw screen overlays
        Rect2i windowRect = Rect2i.createFromMinAndSize(0, 0, getWidth(), getHeight());
        for (Overlay ovly : screenOverlays) {
            if (ovly.isVisible()) {
                ovly.render(g, windowRect);
            }
        }

        drawTooltip(g, visWorld);
    }

    public void invalidateWorld() {
        regionCache.invalidateAll();
        imageCache.invalidateAll();
        repaint();
     }

    @Override
    public void close() {
        int cx = (int) camera.getPos().getX();
        int cy = (int) camera.getPos().getY();

        // TODO: TeraMath compatibility fix
        viewConfig.setCamPos(new Vector2i(cx, cy));

        threadPool.shutdownNow();
    }

    // TODO: consider moving this to Camera (default method?)
    private static Rect2i getVisibleArea(Camera camera, int width, int height) {
        int cx = TeraMath.floorToInt(camera.getPos().getX());
        int cy = TeraMath.floorToInt(camera.getPos().getY());

        // Compensate rounding errors by adding 2px to the visible window size
        int w = (int) (width / camera.getZoom()) + 2;
        int h = (int) (height / camera.getZoom()) + 2;
        int minX = cx - w / 2;
        int minY = cy - h / 2;
        Rect2i visWorld = Rect2i.createFromMinAndSize(minX, minY, w, h);
        return visWorld;
    }

    private static Rect2i toChunkArea(Rect2i area) {
        int chunkMinX = IntMath.divide(area.minX(), TILE_SIZE_X, RoundingMode.FLOOR);
        int chunkMinZ = IntMath.divide(area.minY(), TILE_SIZE_Y, RoundingMode.FLOOR);

        int chunkMaxX = IntMath.divide(area.maxX(), TILE_SIZE_X, RoundingMode.CEILING);
        int chunkMaxZ = IntMath.divide(area.maxY(), TILE_SIZE_Y, RoundingMode.CEILING);

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

        for (int z = visChunks.minY(); z < visChunks.maxY(); z++) {
            for (int x = visChunks.minX(); x < visChunks.maxX(); x++) {
                Vector2i pos = new Vector2i(x, z);
                Region region = regionCache.getUnchecked(pos);
                BufferedImage image = imageCache.getUnchecked(region);
                g.drawImage(image, x * TILE_SIZE_X, z * TILE_SIZE_Y, null);
            }
        }

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    }

    private void drawTooltip(Graphics2D g, Rect2i area) {
        Point curPos = curPosListener.getCursorPosition();

        if (curPos != null) {
            int wx = area.minX() + TeraMath.floorToInt(curPos.x / camera.getZoom());
            int wy = area.minY() + TeraMath.floorToInt(curPos.y / camera.getZoom());

            int tileX = IntMath.divide(wx, TILE_SIZE_X, RoundingMode.FLOOR);
            int tileY = IntMath.divide(wy, TILE_SIZE_Y, RoundingMode.FLOOR);

            Vector2i tilePos = new Vector2i(tileX, tileY);
            Region region = regionCache.getUnchecked(tilePos);

            StringBuffer sb = new StringBuffer();
            for (FacetLayer layer : facetLayers) {
                if (layer.isVisible()) {
                    String layerText = layer.getWorldText(region, wx, wy);
                    if (layerText != null) {
                        sb.append("\n").append(layerText);
                    }
                }
            }

            String tooltip = String.format("%d / %d%s", wx, wy, sb.toString());
            Tooltip.draw(g, curPos.x, curPos.y, tooltip);
        }
    }

    private Region createRegion(Vector2i chunkPos) {

        int vertChunks = 4; // 4 chunks high (relevant for trees, etc)

        int minX = chunkPos.x * TILE_SIZE_X;
        int minZ = chunkPos.y * TILE_SIZE_Y;
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
    void updateImageCache() {
        workQueue.clear();

        // shuffle the order of new tasks
        // If the queue is cleared repeatedly before all tasks are run
        // some tiles will never be updated otherwise
        List<Region> inCache = new ArrayList<Region>(imageCache.asMap().keySet());
        Collections.shuffle(inCache);

        for (Region region : inCache) {
            threadPool.execute(new UpdateImageCache(region));
        }
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

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        for (FacetLayer layer : facetLayers) {
            if (layer.isVisible()) {
                layer.render(image, region);
            }
        }
        g.dispose();
        return image;
    }

    private class UpdateImageCache implements Runnable {

        private Region region;

        public UpdateImageCache(Region region) {
            this.region = region;
        }

        @Override
        public void run() {
            BufferedImage image = rasterize(region);
            imageCache.put(region, image);
            repaint();
        }
    }
}
