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
import java.awt.Image;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import javax.swing.JComponent;

import org.terasology.math.Rect2i;
import org.terasology.math.Region3i;
import org.terasology.math.Vector2i;
import org.terasology.math.Vector3i;
import org.terasology.polyworld.voronoi.Graph;
import org.terasology.polyworld.voronoi.GraphFacet;
import org.terasology.world.chunks.ChunkConstants;
import org.terasology.world.generation.Region;
import org.terasology.world.generation.World;
import org.terasology.world.generator.WorldGenerator;
import org.terasology.worldviewer.camera.Camera;
import org.terasology.worldviewer.camera.CameraKeyController;
import org.terasology.worldviewer.camera.CameraMouseController;
import org.terasology.worldviewer.camera.RepaintingCameraListener;
import org.terasology.worldviewer.config.ViewConfig;
import org.terasology.worldviewer.overlay.BoundsOverlay;
import org.terasology.worldviewer.overlay.GridOverlay;
import org.terasology.worldviewer.overlay.Overlay;

import com.google.common.base.Objects;
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

    private final TraitRenderer rasterizer = new TraitRenderer();

    private final LoadingCache<Vector2i, CacheEntry> tileCache = CacheBuilder.newBuilder().build(new CacheLoader<Vector2i, CacheEntry>() {

        @Override
        public CacheEntry load(final Vector2i tilePos) throws Exception {
            // msteiger: there should be a more elegent way based on CacheLoader.refresh()
            // that loads missing cache values asynchronously
            threadPool.execute(new Runnable() {

                @Override
                public void run() {
                    Region region = createRegion(tilePos);
                    BufferedImage image = (facetTrait == null) ? dummyImg : rasterizer.raster(region, facetTrait);
                    CacheEntry entry = new CacheEntry(image, region);
                    tileCache.put(tilePos, entry);
                    repaint();
                }
            });

            return new CacheEntry(dummyImg, createRegion(tilePos));
        }
    });

    private final Camera camera = new Camera();
    private final WorldGenerator worldGen;

    private final CursorPositionListener curPosListener;

    private final ViewConfig viewConfig;

    private final Deque<Overlay> overlays = Lists.newLinkedList();

    private FacetTrait facetTrait;

    /**
     * @param wg the world generator to use
     * @param viewConfig
     */
    public Viewer(WorldGenerator wg, ViewConfig viewConfig) {
        this.worldGen = wg;
        this.viewConfig = viewConfig;

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
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, dummyImg.getWidth(), dummyImg.getHeight());
        g.setColor(Color.WHITE);
        g.drawRect(0, 0, dummyImg.getWidth() - 1, dummyImg.getHeight() - 1);
        g.dispose();
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
                CacheEntry entry = tileCache.getUnchecked(pos);
                g.drawImage(entry.getImage(), x * TILE_SIZE_X, z * TILE_SIZE_Y, null);
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
        if (facetTrait != null && curPos != null) {
            int wx = area.minX() + curPos.x;
            int wy = area.minY() + curPos.y;

            int tileX = IntMath.divide(wx, TILE_SIZE_X, RoundingMode.FLOOR);
            int tileY = IntMath.divide(wy, TILE_SIZE_Y, RoundingMode.FLOOR);

            CacheEntry entry = tileCache.getUnchecked(new Vector2i(tileX, tileY));
            Region region = entry.getRegion();
            String facetVal = facetTrait.getFacetInfo(region).getWorldText(wx, wy);

            String text = String.format("%d / %d\n%s", wx, wy, facetVal);
            Tooltip.draw(g, wx, wy, text);
        }
    }

    private Region createRegion(Vector2i chunkPos) {

        int minX = chunkPos.x * TILE_SIZE_X;
        int minZ = chunkPos.y * TILE_SIZE_Y;
        Region3i area3d = Region3i.createFromMinAndSize(new Vector3i(minX, 0, minZ), new Vector3i(TILE_SIZE_X, 1, TILE_SIZE_Y));
        World world = worldGen.getWorld();
        Region region = world.getWorldData(area3d);

        return region;
    }

    /**
     * @param facetTrait the facet to show
     */
    public void setFacetTrait(FacetTrait facetTrait) {
        if (Objects.equal(this.facetTrait, facetTrait)) {
            return;
        }

        this.facetTrait = facetTrait;

//        Function<Rect2i, Collection<Rect2i>> func = rc -> {
//            Vector3i min = new Vector3i(rc.minX(), 0, rc.minY());
//            Vector3i size = new Vector3i(rc.width(), 1, rc.height());
//            Region3i area3d = Region3i.createFromMinAndSize(min, size);
//            World world = worldGen.getWorld();
//            Region region = world.getWorldData(area3d);
//            GraphFacet graphFacet = region.getFacet(GraphFacet.class);
//
//            List<Rect2i> rcs = Lists.newArrayList();
//
//            if (graphFacet != null) {
//                for (Graph g : graphFacet.getAllGraphs()) {
//                    rcs.add(g.getBounds());
//                }
//            }
//
//            return rcs;
//        };
//
//        overlays.addFirst(new BoundsOverlay(func));

        tileCache.invalidateAll();
        repaint();
    }

    /**
     * Destroy the world and create a new one. Also reloads all tiles
     */
    public void reload() {
       worldGen.setWorldSeed("sdfsfdf");
       worldGen.getWorld();   // force world generation now (and in a single thread)

       tileCache.invalidateAll();
       repaint();
   }

    @Override
    public void close() {
        // TODO: TeraMath compatibility fix
        viewConfig.setCamPos(new Vector2i(camera.getPos().getX(), camera.getPos().getY()));

        threadPool.shutdownNow();
    }

    private static class CacheEntry {
        private final BufferedImage image;
        private final Region region;

        public CacheEntry(BufferedImage image, Region region) {
            this.image = image;
            this.region = region;
        }

        public Image getImage() {
            return image;
        }

        public Region getRegion() {
            return region;
        }
    }

}
