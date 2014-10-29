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

import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.AtomicDouble;

/**
 * TODO Type description
 * @author Martin Steiger
 */
public class RandomObjectColors implements Function<Object, Color> {

    private static final double GOLDEN_RATIO_CONGJUGATE = 0.618033988749895;

    private final AtomicDouble atomicHue = new AtomicDouble(0);

    private final LoadingCache<Object, Color> colors = CacheBuilder.newBuilder().build(new CacheLoader<Object, Color>() {

        @Override
        public Color load(Object key) throws Exception {
            float hue = (float) atomicHue.getAndAdd(GOLDEN_RATIO_CONGJUGATE);
            float saturation = 0.6f;
            float brightness = 0.9f;
            int rgb = Color.HSBtoRGB(hue, saturation, brightness);
            Color color = new Color(rgb);
            return color;
        }

    });

    public RandomObjectColors() {
    }

    @Override
    public Color apply(Object biome) {
        Color color = colors.getUnchecked(biome);
        return color;
    }
}
