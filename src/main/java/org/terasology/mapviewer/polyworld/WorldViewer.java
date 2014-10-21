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

package org.terasology.mapviewer.polyworld;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.math.RoundingMode;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.mapviewer.camera.Camera;
import org.terasology.mapviewer.camera.CameraKeyController;
import org.terasology.mapviewer.camera.CameraListener;
import org.terasology.mapviewer.camera.CameraMouseController;
import org.terasology.math.Rect2i;
import org.terasology.math.Region3i;
import org.terasology.math.TeraMath;
import org.terasology.math.Vector2i;
import org.terasology.math.Vector3i;
import org.terasology.world.chunks.ChunkConstants;
import org.terasology.world.generation.Region;
import org.terasology.world.generation.World;
import org.terasology.world.generation.facets.SurfaceHeightFacet;
import org.terasology.world.generation.facets.base.FieldFacet2D;

import com.google.common.base.Objects;
import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.math.IntMath;

/**
 * TODO Type description
 * @author Martin Steiger
 */
public final class WorldViewer extends JComponent implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(WorldViewer.class);

    private static final int TILE_SIZE_X = ChunkConstants.SIZE_X * 4;
    private static final int TILE_SIZE_Y = ChunkConstants.SIZE_Z * 4;

    private static final long serialVersionUID = 4178713176841691478L;

    private final BufferedImage dummyImg = new BufferedImage(TILE_SIZE_X, TILE_SIZE_Y, BufferedImage.TYPE_INT_RGB);

    private ExecutorService threadPool = Executors.newCachedThreadPool();

    private final LoadingCache<Vector2i, CacheEntry> tileCache = CacheBuilder.newBuilder().build(new CacheLoader<Vector2i, CacheEntry>() {

        @Override
        public CacheEntry load(final Vector2i tilePos) throws Exception {
            // msteiger: there should be a more elegent way based on CacheLoader.refresh()
            // that loads missing cache values asynchronously
            threadPool.execute(new Runnable() {

                @Override
                public void run() {
                    Region region = createRegion(tilePos);
                    BufferedImage image = createImage(region);
                    CacheEntry entry = new CacheEntry(image, region);
                    tileCache.put(tilePos, entry);
                    repaint();
                }
            });

            return new CacheEntry(dummyImg, createRegion(tilePos));
        }
    });

    private final Camera camera = new Camera();
    private final World world;

    private Class<? extends FieldFacet2D> facetClass;

    Point curPos;

    /**
     * @param world
     * @param camera
     */
    public WorldViewer(World world) {
        this.world = world;
        camera.addListener(new CameraBasedRepaint(this));
        camera.translate(512, 512);

        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        KeyAdapter keyCameraController = new CameraKeyController(camera);
        MouseAdapter mouseCameraController = new CameraMouseController(camera);
        addKeyListener(keyCameraController);
        addMouseListener(mouseCameraController);
        addMouseMotionListener(mouseCameraController);
        addMouseMotionListener(new MouseMotionListener() {

            @Override
            public void mouseMoved(MouseEvent e) {
                curPos = e.getPoint();
                updateLabel();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                curPos = e.getPoint();
                updateLabel();
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                curPos = null;
                updateLabel();
            }
        });

        camera.addListener(new CameraListener() {

            @Override
            public void onPosChange() {
                updateLabel();
            }

            @Override
            public void onZoomChange() {
                updateLabel();
            }
        });

        Graphics2D g = dummyImg.createGraphics();
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, dummyImg.getWidth(), dummyImg.getHeight());
        g.setColor(Color.LIGHT_GRAY);
        g.drawRect(0, 0, dummyImg.getWidth() - 1, dummyImg.getHeight() - 1);
        g.dispose();
    }

    protected void updateLabel() {
        repaint();
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

        // drawGrid
        Color majorGrid = new Color(128, 128, 128, 192);
        Color minorGrid = new Color(128, 128, 128, 64);
        g.setColor(minorGrid);
        g.setStroke(new BasicStroke(0));

        for (int z = camChunkMinZ; z < camChunkMaxZ; z++) {
            g.setColor((z % 8 == 0) ? majorGrid : minorGrid);
            g.drawLine(camChunkMinX * TILE_SIZE_X, z * TILE_SIZE_Y, camChunkMaxX * TILE_SIZE_X, z * TILE_SIZE_Y);
        }

        for (int x = camChunkMinX; x < camChunkMaxX; x++) {
            g.setColor((x % 8 == 0) ? majorGrid : minorGrid);
            g.drawLine(x * TILE_SIZE_X, camChunkMinZ * TILE_SIZE_Y, x * TILE_SIZE_X, camChunkMaxZ * TILE_SIZE_Y);
        }


        // draw tooltip
        if (curPos != null) {
            int wx = minX + curPos.x;
            int wy = minY + curPos.y;

            int tileX = IntMath.divide(wx, TILE_SIZE_X, RoundingMode.FLOOR);
            int tileY = IntMath.divide(wy, TILE_SIZE_Y, RoundingMode.FLOOR);

            CacheEntry entry = tileCache.getUnchecked(new Vector2i(tileX, tileY));
            Region region = entry.getRegion();
            FieldFacet2D facet = region.getFacet(facetClass);
            float value = facet.getWorld(wx, wy);

            String text = String.format("%d / %d\n%.2f", wx, wy, value);
            drawTooltip(g, wx, wy, text);
        }

    }

    private void drawTooltip(Graphics2D g, int wx, int wy, String text) {
        int offX = 5;
        int offY = 5;

        String[] lines = text.split("\n");

        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();

        int x = wx + offX;
        int y = wy + offY + fm.getAscent();

        int maxHeight = lines.length * fm.getHeight();
        int maxWidth = 0;
        for (String line : lines) {
            int width = fm.stringWidth(line);
            if (width > maxWidth)
                maxWidth = width;
        }

        int inset = 2;
        g.setColor(new Color(64, 64, 64, 128));
        g.fillRect(wx + offX - inset, wy + offY - inset, maxWidth + 2 * inset, maxHeight + 2 * inset);

        g.setColor(new Color(192, 192, 192, 128));
        g.drawRect(wx + offX - inset, wy + offY - inset, maxWidth + 2 * inset, maxHeight + 2 * inset);

        g.setColor(Color.WHITE);

        for (String line : lines) {
            g.drawString(line, x, y);
            y += fm.getHeight();
        }

    }

    private Region createRegion(Vector2i chunkPos) {

        int minX = chunkPos.x * TILE_SIZE_X;
        int minZ = chunkPos.y * TILE_SIZE_Y;
        Region3i area3d = Region3i.createFromMinAndSize(new Vector3i(minX, 0, minZ), new Vector3i(TILE_SIZE_X, 1, TILE_SIZE_Y));
        Region region = world.getWorldData(area3d);

        return region;
    }

    private BufferedImage createImage(Region region) {
        if (facetClass == null)
            return dummyImg;

        FieldFacet2D facet = region.getFacet(facetClass);

        if (facet == null)
            return dummyImg;

        Stopwatch sw = Stopwatch.createStarted();

        BufferedImage img = new BufferedImage(TILE_SIZE_X, TILE_SIZE_Y, BufferedImage.TYPE_INT_RGB);

        for (int z = 0; z < TILE_SIZE_Y; z++) {
            for (int x = 0; x < TILE_SIZE_X; x++) {
                float val = facet.get(x, z);
                int c = mapFloat(val);
                img.setRGB(x, z, c);
            }
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Rendered regions in {}ms.", sw.elapsed(TimeUnit.MILLISECONDS));
        }

        return img;
    }

    private int mapFloat(float val) {
        int g = TeraMath.clamp((int)(val * 4), 0, 255);
        return g | (g << 8) | (g << 16);
    }

    public void setFacet(Class<? extends FieldFacet2D> clazz)
    {
        if (Objects.equal(facetClass, clazz)) {
            return;
        }

        this.facetClass = clazz;
        tileCache.invalidateAll();
        repaint();
    }

    @Override
    public void close() {
        threadPool.shutdownNow();
    }

    private static class CacheEntry {
        private final BufferedImage image;
        private final Region region;

        /**
         * @param image
         * @param region
         */
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
