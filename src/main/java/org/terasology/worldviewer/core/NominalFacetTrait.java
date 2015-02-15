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

import org.terasology.rendering.nui.Color;
import org.terasology.world.generation.Region;
import org.terasology.world.generation.WorldFacet;
import org.terasology.world.generation.facets.base.ObjectFacet2D;

import com.google.common.base.Function;

/**
 * Provides info about an {@link ObjectFacet2D}.
 * @param <E> the object type
 * @author Martin Steiger
 */
public class NominalFacetTrait<E> implements FacetTrait {

    private final Function<? super E, Color> colorMap;
    private final Class<? extends ObjectFacet2D<E>> facetClass;

    public NominalFacetTrait(Class<? extends ObjectFacet2D<E>> clazz, Function<? super E, Color> colorMap) {
        this.colorMap = colorMap;
        this.facetClass = clazz;
    }

    @Override
    public FacetInfo getFacetInfo(Region r) {
        ObjectFacet2D<E> facet = r.getFacet(facetClass);

        return new FacetInfo() {

            @Override
            public String getWorldText(int wx, int wy) {
                E val = facet.getWorld(wx, wy);
                return val.toString();
            }

            @Override
            public int getRGB(int x, int z) {
                E val = facet.get(x, z);
                return colorMap.apply(val).rgba() >> 8;
            }
        };
    }

    @Override
    public Class<? extends WorldFacet> getFacetClass() {
        return facetClass;
    }

    @Override
    public String toString() {
        return facetClass.getSimpleName() + " - > Color using " + colorMap.getClass().getSimpleName();
    }
}
