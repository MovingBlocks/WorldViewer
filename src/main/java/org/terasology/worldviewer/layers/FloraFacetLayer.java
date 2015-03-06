/*
 * Copyright 2015 MovingBlocks
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

package org.terasology.worldviewer.layers;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map.Entry;
import java.util.function.Function;

import org.terasology.core.world.generator.facets.FloraFacet;
import org.terasology.core.world.generator.rasterizers.FloraType;
import org.terasology.math.Region3i;
import org.terasology.math.Vector3i;
import org.terasology.rendering.nui.Color;
import org.terasology.world.generation.Region;
import org.terasology.world.generation.WorldFacet;
import org.terasology.worldviewer.core.CoreFloraColors;

/**
 * Renders the flora coverage based on {@link FloraFacet}.
 * @author Martin Steiger
 */
public class FloraFacetLayer extends AbstractFacetLayer {

    private Function<FloraType, Color> colorFunc = new CoreFloraColors();
    private Function<FloraType, String> labelFunc = Object::toString;

    @Override
    public Class<? extends WorldFacet> getFacetClass() {
        return FloraFacet.class;
    }

    @Override
    public void render(BufferedImage img, Region region) {
        FloraFacet treeFacet = region.getFacet(FloraFacet.class);

        Graphics2D g = img.createGraphics();

        for (Entry<Vector3i, FloraType> entry : treeFacet.getRelativeEntries().entrySet()) {
            FloraType treeGen = entry.getValue();
            int wx = entry.getKey().getX();
            int wz = entry.getKey().getZ();
            Color color = colorFunc.apply(treeGen);

            int mix = (color.rgba() >> 8) | (color.a() << 24);
            img.setRGB(wx, wz, mix);
        }

        g.dispose();
    }

    @Override
    public String getWorldText(Region region, int wx, int wy) {
        FloraFacet floraFacet = region.getFacet(FloraFacet.class);

        Region3i worldRegion = floraFacet.getWorldRegion();
        Region3i relativeRegion = floraFacet.getRelativeRegion();

        int rx = wx - worldRegion.minX() + relativeRegion.minX();
        int rz = wy - worldRegion.minZ() + relativeRegion.minZ();

        for (Entry<Vector3i, FloraType> entry : floraFacet.getRelativeEntries().entrySet()) {
            Vector3i treePos = entry.getKey();

            if (treePos.getX() == rx && treePos.getZ() == rz) {
                FloraType flora = entry.getValue();
                return labelFunc.apply(flora);
            }
        }

        return "-no vegetation-";
    }

}
