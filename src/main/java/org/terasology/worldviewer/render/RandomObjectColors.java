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

package org.terasology.worldviewer.render;

import java.util.List;

import org.terasology.rendering.nui.Color;
import org.terasology.rendering.nui.layers.mainMenu.CieCamColors;

import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.AtomicDouble;

/**
 * Assign a unique color to an object. This assignment is deterministic
 * and always returns the same color for the same object.
 * <br/><br/>
 * Brightness and saturation remain constant while the hue differs.
 * @author Martin Steiger
 */
public class RandomObjectColors implements Function<Object, Color> {

    private static final double GOLDEN_RATIO_CONJUGATE = 0.618033988749895;

    private final AtomicDouble atomicHue = new AtomicDouble(0);

    private final LoadingCache<Object, Color> colorCache = CacheBuilder.newBuilder().build(new CacheLoader<Object, Color>() {

        private final List<Color> colors = CieCamColors.L65C65;

        @Override
        public Color load(Object key) throws Exception {
            double hue = atomicHue.getAndAdd(GOLDEN_RATIO_CONJUGATE);
            int index = (int) (hue * (colors.size())) % colors.size();
            return colors.get(index);
        }

    });

    public RandomObjectColors() {
    }

    @Override
    public Color apply(Object biome) {
        Color color = colorCache.getUnchecked(biome);
        return color;
    }
}
