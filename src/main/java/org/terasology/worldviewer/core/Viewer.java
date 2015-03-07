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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;
import java.math.RoundingMode;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JComponent;

import org.terasology.math.Rect2i;
import org.terasology.math.Region3i;
import org.terasology.math.Vector2i;
import org.terasology.math.Vector3i;
import org.terasology.world.chunks.ChunkConstants;
import org.terasology.world.generation.Region;
import org.terasology.world.generation.World;
import org.terasology.world.generator.WorldGenerator;
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

    private final ExecutorService threadPool = Executors.newFixedThreadPool(4);
    private final CacheLoader<Vector2i, Region> regionLoader = new CacheLoader<Vector2i, Region>() {

        @Override
        public Region load(final Vector2i tilePos) {
            Region region = createRegion(tilePos);
            return region;
        }
    };

    /**
     * A cache for regions based on <b>soft-values</b>.
     */
    private final LoadingCache<Vector2i, Region> regionCache = CacheBuilder.newBuilder().softValues().build(regionLoader);

    private final CacheLoader<Region, BufferedImage> imageLoader = new CacheLoader<Region, BufferedImage>() {

        @Override
        public BufferedImage load(Region region) throws Exception {
            threadPool.execute(new UpdateImageCache(region));
            return dummyImg;
        }
    };

    private final LoadingCache<Region, BufferedImage> imageCache = CacheBuilder.newBuilder().build(imageLoader);

    private final Camera camera = new Camera();
    private final WorldGenerator worldGen;

    private final CursorPositionListener curPosListener;

    private final ViewConfig viewConfig;

    private final Deque<Overlay> overlays = Lists.newLinkedList();

    private final List<FacetLayer> facets;

    /**
     * @param wg the world generator to use
     * @param facetConfig the facet config
     * @param viewConfig the view config
     */
    public Viewer(WorldGenerator wg, List<FacetLayer> facetConfig, ViewConfig viewConfig) {
        this.worldGen = wg;
        this.viewConfig = viewConfig;
        this.facets = facetConfig;

        camera.addListener(new RepaintingCameraListener(this));
        Vector2i camPos = viewConfig.getCamPos();
        camera.translate(camPos.getX(), camPos.getY());

        GridOverlay gridOverlay = new GridOverlay(TILE_SIZE_X, TILE_SIZE_Y);
        overlays.addLast(gridOverlay);

        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        // add camera controls
        KeyAdapter keyCameraController = new CameraKeyController(camera);
        MouseAdapter mouseCameraController = new CameraMouseController(camera);
        addKeyListener(keyCameraController);
        addMouseListener(mouseCameraController);
        addMouseMotionListener(mouseCameraController);

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
        for (FacetLayer layer : facetConfig) {
            layer.addObserver(l -> updateImageCache());
        }
    }

    private BufferedImage rasterize(Region region) {
        Vector3i extent = region.getRegion().size();
        int width = extent.x;
        int height = extent.z;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        for (FacetLayer layer : facets) {
            if (layer.isVisible()) {
                layer.render(image, region);
            }
        }
        g.dispose();
        return image;
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    @Override
    public void paint(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;

        int cx = camera.getPos().getX();
        int cy = camera.getPos().getY();
        int w = getWidth();
        int h = getHeight();
        int minX = cx - w / 2;
        int minY = cy - h / 2;
        Rect2i area = Rect2i.createFromMinAndSize(minX, minY, w, h);

        g.translate(-minX, -minY);

        drawTiles(g, area);

        drawOverlays(g, area);

        drawTooltip(g, area);
    }

    private void drawTiles(Graphics2D g, Rect2i area) {
        int camChunkMinX = IntMath.divide(area.minX(), TILE_SIZE_X, RoundingMode.FLOOR);
        int camChunkMinZ = IntMath.divide(area.minY(), TILE_SIZE_Y, RoundingMode.FLOOR);

        int camChunkMaxX = IntMath.divide(area.maxX(), TILE_SIZE_X, RoundingMode.CEILING);
        int camChunkMaxZ = IntMath.divide(area.maxY(), TILE_SIZE_Y, RoundingMode.CEILING);

        for (int z = camChunkMinZ; z < camChunkMaxZ; z++) {
            for (int x = camChunkMinX; x < camChunkMaxX; x++) {
                Vector2i pos = new Vector2i(x, z);
                Region region = regionCache.getUnchecked(pos);
                BufferedImage image = imageCache.getUnchecked(region);
                g.drawImage(image, x * TILE_SIZE_X, z * TILE_SIZE_Y, null);
            }
        }
    }

    private void drawOverlays(Graphics2D g, Rect2i area) {
        for (Overlay ovly : overlays) {
            ovly.render(g, area);
        }
    }

    private void drawTooltip(Graphics2D g, Rect2i area) {
        Point curPos = curPosListener.getCursorPosition();

        if (curPos != null) {
            int wx = area.minX() + curPos.x;
            int wy = area.minY() + curPos.y;

            int tileX = IntMath.divide(wx, TILE_SIZE_X, RoundingMode.FLOOR);
            int tileY = IntMath.divide(wy, TILE_SIZE_Y, RoundingMode.FLOOR);

            Vector2i tilePos = new Vector2i(tileX, tileY);
            Region region = regionCache.getUnchecked(tilePos);

            StringBuffer sb = new StringBuffer();
            for (FacetLayer layer : facets) {
                if (layer.isVisible()) {
                    String layerText = layer.getWorldText(region, wx, wy);
                    if (layerText != null) {
                        sb.append("\n").append(layerText);
                    }
                }
            }

            String tooltip = String.format("%d / %d%s", wx, wy, sb.toString());
            Tooltip.draw(g, wx, wy, tooltip);
        }
    }

    private Region createRegion(Vector2i chunkPos) {

        int vertChunks = 4; // 4 chunks high (relevant for trees, etc)

        int minX = chunkPos.x * TILE_SIZE_X;
        int minZ = chunkPos.y * TILE_SIZE_Y;
        int height = vertChunks * ChunkConstants.SIZE_Y;
        Region3i area3d = Region3i.createFromMinAndSize(new Vector3i(minX, 0, minZ), new Vector3i(TILE_SIZE_X, height, TILE_SIZE_Y));
        World world = worldGen.getWorld();
        Region region = world.getWorldData(area3d);

        return region;
    }

    public void invalidateWorld() {
        regionCache.invalidateAll();
        imageCache.invalidateAll();
        repaint();
     }

    private void updateImageCache() {
        for (Region region : imageCache.asMap().keySet()) {
            threadPool.execute(new UpdateImageCache(region));
        }
    }

    @Override
    public void close() {
        // TODO: TeraMath compatibility fix
        viewConfig.setCamPos(new Vector2i(camera.getPos().getX(), camera.getPos().getY()));

        threadPool.shutdownNow();
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
