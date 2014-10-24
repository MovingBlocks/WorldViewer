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

package org.terasology.mapviewer.core;

import java.math.RoundingMode;

import org.terasology.math.TeraMath;
import org.terasology.world.generation.Region;
import org.terasology.world.generation.facets.base.FieldFacet2D;

import com.google.common.math.DoubleMath;

/**
 * TODO Type description
 * @author Martin Steiger
 */
public class FieldFacetTrait implements FacetTrait {

    private final double offset;
    private final double scale;
    private Class<? extends FieldFacet2D> clazz;

    public FieldFacetTrait(Class<? extends FieldFacet2D> clazz, double offset, double scale) {
        this.clazz = clazz;
        this.offset = offset;
        this.scale = scale;
    }

    @Override
    public FacetInfo getFacetInfo(Region region) {
        FieldFacet2D facet = region.getFacet(clazz);

        return new FacetInfo() {

            @Override
            public String getWorldText(int wx, int wy) {
                double value = facet.getWorld(wx, wy);
                return String.format("%.2f", value);
            }

            @Override
            public int getRGB(int x, int z) {
                double value = facet.get(x, z);
                int round = DoubleMath.roundToInt(offset + scale * value, RoundingMode.HALF_UP);
                int g = TeraMath.clamp(round, 0, 255);
                return g | (g << 8) | (g << 16);
            }
        };
    }

    @Override
    public String toString() {
        return clazz.getSimpleName() + " - > " + offset + " + " + scale + " * value";
    }
}
