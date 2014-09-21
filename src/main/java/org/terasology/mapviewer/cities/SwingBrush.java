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

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.cities.BlockTypes;
import org.terasology.cities.raster.Brush;
import org.terasology.math.Side;
import org.terasology.math.TeraMath;

import com.google.common.base.Function;

/**
 * Converts model elements into blocks of of a chunk
 * @author Martin Steiger
 */
public class SwingBrush extends Brush {
    
    private static final Logger logger = LoggerFactory.getLogger(SwingBrush.class);
    
    private final Function<BlockTypes, Color> blockColor;
    private final Rectangle affectedArea;

    private final BufferedImage image;
    private final short[][] heightMap;      // [x][z] 

    private final int wz;
    private final int wx;
    
    /**
     * @param wx the world block x of the top-left corner
     * @param wz the world block z of the top-left corner
     * @param image the image to draw onto
     * @param blockColor a mapping String type -> block
     */
    public SwingBrush(int wx, int wz, BufferedImage image, Function<BlockTypes, Color> blockColor) {
        this.blockColor = blockColor;
        this.image = image;
        this.wx = wx;
        this.wz = wz;

        int width = image.getWidth();
        int height = image.getHeight();
        
        this.heightMap = new short[width][height];
        
        this.affectedArea = new Rectangle(wx, wz, width, height);
    }

    @Override
    public Rectangle getAffectedArea() {
        return affectedArea;
    }

    @Override
    public int getMaxHeight() {
        return 64;
    }
    
    @Override
    public int getMinHeight() {
        return 0;
    } 
    
    /**
     * @param x x in world coords
     * @param y y in world coords
     * @param z z in world coords
     * @param type the block type 
     */
    @Override
    public void setBlock(int x, int y, int z, BlockTypes type) {
        setBlock(x, y, z, blockColor.apply(type));
    }

    @Override
    public void setBlock(int x, int y, int z, BlockTypes type, Set<Side> side) {
        setBlock(x, y, z, blockColor.apply(type));
    }

    
    /**
     * @param x x in world coords
     * @param y y in world coords
     * @param z z in world coords
     * @param color the actual block color
     */
    protected void setBlock(int x, int y, int z, Color color) {
        
        int lx = x - wx;
        int lz = z - wz;

        // TODO: remove
        final boolean debugging = true;
        final boolean warnOnly = true;
        if (debugging) {
            boolean xOk = lx >= 0 && lx < image.getWidth();
            boolean yOk = lx >= getMinHeight() && lx < getMaxHeight();
            boolean zOk = lz >= 0 && lz < image.getHeight();
            
            if (warnOnly) {
                if (!xOk) {
                    logger.warn("X value of {} not in range [{}..{}]", x, wx, wx + image.getWidth() - 1);
                    return;
                }
                
                if (!yOk) {
                    logger.warn("Y value of {} not in range [{}..{}]", y, getMinHeight(), getMaxHeight() - 1);
                    return;
                }
                
                if (!zOk) {
                    logger.warn("Z value of {} not in range [{}..{}]", z, wz, wz + image.getHeight() - 1);
                    return;
                }
            } 
        }
        
        // this is a bit of a hack - alpha is 0 only for Block.AIR
        // if air is drawn at or below terrain level, then reduce height accordingly
        // The color remains unchanged which is wrong, but this information is not available in 2D
        if (color.getAlpha() == 0) {
            if (heightMap[lx][lz] >= y) {
                heightMap[lx][lz] = (short) (y - 1);
            }
            return;
        }
            
        if (heightMap[lx][lz] <= y) {
            heightMap[lx][lz] = (short) y;
            float[] hsb = new float[3];
            Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
            hsb[2] = hsb[2] * (0.5f + 0.5f * (float) TeraMath.clamp(y / 16f));
            image.setRGB(lx, lz, Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
        }
    }
}
