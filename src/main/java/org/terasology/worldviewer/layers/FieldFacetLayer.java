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

package org.terasology.worldviewer.layers;

import java.awt.image.BufferedImage;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.math.TeraMath;
import org.terasology.rendering.nui.Color;
import org.terasology.world.generation.Region;
import org.terasology.world.generation.WorldFacet;
import org.terasology.world.generation.facets.base.FieldFacet2D;

import com.google.common.base.Stopwatch;
import com.google.common.math.DoubleMath;

/**
 * TODO Type description
 * @author Martin Steiger
 */
public class FieldFacetLayer extends AbstractFacetLayer {

    private static final List<Color> GRAYS = IntStream
            .range(0, 256)
            .mapToObj(i -> new Color(i, i, i))
            .collect(Collectors.toList());

    private static final Color MISSING = Color.MAGENTA;

    private static final Logger logger = LoggerFactory.getLogger(FieldFacetLayer.class);

    private final Class<? extends FieldFacet2D> clazz;

    private double offset;
    private double scale;

    public FieldFacetLayer(Class<? extends FieldFacet2D> clazz, double offset, double scale) {
        this.clazz = clazz;
        this.offset = offset;
        this.scale = scale;
    }

    @Override
    public String getWorldText(Region region, int wx, int wy) {
        FieldFacet2D facet = region.getFacet(clazz);
        double value = facet.getWorld(wx, wy);
        return String.format("%.2f", value);
    }

    @Override
    public void render(BufferedImage img, Region region) {
        FieldFacet2D facet = region.getFacet(clazz);

        Stopwatch sw = Stopwatch.createStarted();

        int width = img.getWidth();
        int height = img.getHeight();

        for (int z = 0; z < width; z++) {
            for (int x = 0; x < height; x++) {
                Color src = getColor(facet, x, z);
                int mix = (src.rgba() >> 8) | (src.a() << 24);
                img.setRGB(x, z, mix);
            }
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Rendered regions in {}ms.", sw.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    private Color getColor(FieldFacet2D facet, int x, int z) {
        double value = facet.get(x, z);
        if (Double.isFinite(value)) {
            int round = DoubleMath.roundToInt(offset + scale * value, RoundingMode.HALF_UP);
            int idx = TeraMath.clamp(round, 0, 255);
            return GRAYS.get(idx);
        } else {
            return MISSING;
        }
    }

    @Override
    public Class<? extends WorldFacet> getFacetClass() {
        return clazz;
    }

    public double getOffset() {
        return offset;
    }

    public double getScale() {
        return scale;
    }

    @Override
    public String toString() {
        return clazz.getSimpleName() + " - > linear function";
    }

    /**
     * @param scale the new scale factor
     */
    public void setScale(double scale) {
        if (scale != this.scale) {
            this.scale = scale;
            notifyObservers();
        }
    }

    /**
     * @param offset the new offset
     */
    public void setOffset(double offset) {
        if (offset != this.offset) {
            this.offset = offset;
            notifyObservers();
        }
    }


}
