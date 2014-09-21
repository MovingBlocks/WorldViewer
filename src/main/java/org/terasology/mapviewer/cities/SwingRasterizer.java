/*
 * Copyright 2013 MovingBlocks
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

package org.terasology.mapviewer.cities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.vecmath.Point2i;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.cities.BlockTypes;
import org.terasology.cities.CityTerrainComponent;
import org.terasology.cities.WorldFacade;
import org.terasology.cities.model.City;
import org.terasology.cities.model.Lake;
import org.terasology.cities.model.Road;
import org.terasology.cities.raster.Brush;
import org.terasology.cities.raster.RasterRegistry;
import org.terasology.cities.raster.TerrainInfo;
import org.terasology.cities.raster.standard.RoadRasterizer;
import org.terasology.cities.raster.standard.StandardRegistry;
import org.terasology.commonworld.Orientation;
import org.terasology.commonworld.Sector;
import org.terasology.commonworld.Sectors;
import org.terasology.commonworld.contour.Contour;
import org.terasology.commonworld.heightmap.HeightMap;
import org.terasology.commonworld.heightmap.HeightMaps;
import org.terasology.commonworld.heightmap.NoiseHeightMap;
import org.terasology.commonworld.symmetry.Symmetries;
import org.terasology.math.TeraMath;
import org.terasology.world.chunks.ChunkConstants;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Uses world generation code to draw on a swing canvas
 * @author Martin Steiger
 */
public class SwingRasterizer {

    private static final Logger logger = LoggerFactory.getLogger(SwingRasterizer.class);

    private final WorldFacade facade;
    private final HeightMap heightMap;

    private final Map<BlockTypes, Color> themeMap = Maps.newConcurrentMap();
    private Function<BlockTypes, Color> colorFunc = new Function<BlockTypes, Color>() {

        @Override
        public Color apply(BlockTypes input) {
            Color color = themeMap.get(input);

            if (color == null) {
                color = Color.GRAY;
            }

            return color;
        }
    };

    private LoadingCache<String, Stopwatch> debugMap = CacheBuilder.newBuilder().build(
        new CacheLoader<String, Stopwatch>() {
            @Override
            public Stopwatch load(String key) {
                return Stopwatch.createUnstarted();
            }});

    /**
     * @param seed the seed value
     */
    public SwingRasterizer(String seed) {
        NoiseHeightMap noiseMap = new NoiseHeightMap();
        noiseMap.setSeed(seed);
        heightMap = HeightMaps.symmetric(noiseMap, Symmetries.alongNegativeDiagonal());
        
        facade = new WorldFacade(seed, heightMap);
        
        themeMap.put(BlockTypes.AIR, new Color(0, 0, 0, 0));
        themeMap.put(BlockTypes.ROAD_SURFACE, new Color(160, 40, 40));
        themeMap.put(BlockTypes.LOT_EMPTY, new Color(224, 224, 64));
        themeMap.put(BlockTypes.BUILDING_WALL, new Color(158, 158, 158));
        themeMap.put(BlockTypes.BUILDING_FLOOR, new Color(100, 100, 100));
        themeMap.put(BlockTypes.BUILDING_FOUNDATION, new Color(90, 60, 60));
        themeMap.put(BlockTypes.ROOF_FLAT, new Color(255, 60, 60));
        themeMap.put(BlockTypes.ROOF_HIP, new Color(255, 60, 60));
        themeMap.put(BlockTypes.ROOF_SADDLE, new Color(224, 120, 100));
        themeMap.put(BlockTypes.ROOF_DOME, new Color(160, 190, 190));
        themeMap.put(BlockTypes.ROOF_GABLE, new Color(180, 120, 100));

        themeMap.put(BlockTypes.TOWER_WALL, new Color(200, 100, 200));        
    }

    /**
     * @param g the graphics object
     * @param sector the sector to render
     */
    public void rasterizeSector(Graphics2D g, Sector sector) {

        Stopwatch sw = debugMap.getUnchecked(sector.toString());
        sw.start();
        
        drawCityNames(g, sector);
        drawLakes(g, sector);
        drawFrame(g, sector);
        drawSectorText(g, sector);
        
        sw.stop();
    }
    
    public void drawDebug(Graphics2D go) {
        Graphics2D g = (Graphics2D) go.create();
        g.setTransform(new AffineTransform());

        FontMetrics fm = g.getFontMetrics();
        g.setColor(Color.BLACK);

        int x = 10;
        int y = 10 + fm.getAscent();
        int dy = fm.getHeight();
        
        List<String> keys = new ArrayList<>(debugMap.asMap().keySet());
        Collections.sort(keys);
        
        for (String entry : keys) {
            long time = debugMap.getUnchecked(entry).elapsed(TimeUnit.MILLISECONDS);
            String str = String.format("%s: %dms.", entry, time);
            g.drawString(str, x, y);
            y += dy;
        }

        debugMap.invalidateAll();
        
        g.dispose();
    }

    private void drawLakes(Graphics2D g, Sector sector) {
        
        Set<Lake> lakes = facade.getLakes(sector);

        g.setStroke(new BasicStroke(2.0f));
        
        for (Lake l : lakes) {
            
            Contour cont = l.getContour();
            for (Point p : cont.getSimplifiedCurve()) {
                int r = 3;
                g.fillOval(p.x - r, p.y - r, 2 * r, 2 * r);
            }

            Polygon poly = cont.getPolygon();
            g.draw(poly);
            
            int cx = (int) poly.getBounds().getCenterX();
            int cy = (int) poly.getBounds().getCenterY();
            
            FontMetrics fm = g.getFontMetrics();
            int lw = fm.stringWidth(l.getName());
            int lh = fm.getHeight();
            g.drawString(l.getName(), cx - lw / 2, cy - lh / 2);
        }
        
        g.setStroke(new BasicStroke());
    }
    
    private void drawCityNames(Graphics2D g, Sector sector) {
        
        for (City city : facade.getCities(sector)) {
            drawCityName(g, city);
        }
    }

    public void rasterizeChunk(Graphics2D g, Point2i coord) {

        int chunkSizeX = ChunkConstants.SIZE_X;
        int chunkSizeZ = ChunkConstants.SIZE_Z;

        int wx = coord.getX() * chunkSizeX;
        int wz = coord.getY() * chunkSizeZ;

        Sector sector = Sectors.getSectorForBlock(wx, wz);
        
        if (g.hitClip(wx, wz, chunkSizeX, chunkSizeZ)) {

            Stopwatch swBK = debugMap.getUnchecked("RASTER Background");
            Stopwatch swCt = debugMap.getUnchecked("RASTER Cities");
            Stopwatch swRd = debugMap.getUnchecked("RASTER Roads");
            
            BufferedImage image = new BufferedImage(chunkSizeX, chunkSizeZ, BufferedImage.TYPE_INT_RGB);
            Brush brush = new SwingBrush(wx, wz, image, colorFunc);

            HeightMap cachedHm = HeightMaps.caching(heightMap, brush.getAffectedArea(), 8);
            TerrainInfo ti = new TerrainInfo(cachedHm);

            swBK.start();
            drawBackground(image, wx, wz, ti);
            swBK.stop();
            
            swCt.start();
            drawCities(sector, ti, brush);
            swCt.stop();
            
            swRd.start();
            drawRoads(sector, ti, brush);
            swRd.stop();

            int ix = wx;
            int iy = wz;
            g.drawImage(image, ix, iy, null);

        }


    }

    private void drawRoads(Sector sector, TerrainInfo ti, Brush brush) {
        Set<Road> roads = facade.getRoads(sector);
    
        RoadRasterizer rr = new RoadRasterizer();
        for (Road road : roads) {
            rr.raster(brush, ti, road);
        }
    }
    
    private void drawCities(Sector sector, TerrainInfo ti, Brush brush) {
        Set<City> cities = Sets.newHashSet(facade.getCities(sector));
    
        for (Orientation dir : Orientation.values()) {
            cities.addAll(facade.getCities(sector.getNeighbor(dir)));
        }
    
        RasterRegistry registry = StandardRegistry.getInstance();

        for (City city : cities) {
            registry.rasterize(brush, ti, city);
        }
    }
    
    private void drawCityName(Graphics2D g, City ci) {
        String text = ci.toString();

        int cx = ci.getPos().x;
        int cz = ci.getPos().y;

        Font font = g.getFont();
        FontMetrics fm = g.getFontMetrics(font);
        int width = fm.stringWidth(text);

        g.setColor(Color.BLACK);
        g.drawString(text, cx - width / 2, cz + (float) ci.getDiameter() * 0.5f + 10f);
    }

    private void drawBackground(BufferedImage image, int wx, int wz, TerrainInfo ti) {
        int width = image.getWidth();
        int height = image.getHeight();
        int maxHeight = 20;

        CityTerrainComponent terrainConfig = WorldFacade.getWorldEntity().getComponent(CityTerrainComponent.class);

        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                int gx = wx + x;
                int gz = wz + z;
                int y = ti.getHeightMap().apply(gx, gz);
                int b = TeraMath.clamp(255 - (maxHeight - y) * 5, 0, 255);

                Color c;
                if (y <= terrainConfig.getSeaLevel()) {
                    c = Color.BLUE; 
                } else {
                    c = new Color(b, b, b);
                }
                
                image.setRGB(x, z, c.getRGB());
            }
        }
    }

    
   private void drawSectorText(Graphics2D g, Sector sector) {
       int offX = Sector.SIZE * sector.getCoords().x;
       int offZ = Sector.SIZE * sector.getCoords().y;

       g.setColor(Color.BLUE);
       Font oldFont = g.getFont();
       g.setFont(oldFont.deriveFont(10f));
       g.drawString(sector.toString(), offX + 5, offZ + g.getFontMetrics().getAscent());
       g.setFont(oldFont);
   }
   
    private void drawFrame(Graphics2D g, Sector sector) {
        int offX = Sector.SIZE * sector.getCoords().x;
        int offZ = Sector.SIZE * sector.getCoords().y;

        g.setColor(Color.BLUE);
        g.setStroke(new BasicStroke(0.0f));
        g.drawRect(offX, offZ, Sector.SIZE, Sector.SIZE);
        g.setStroke(new BasicStroke());
    }
}
