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

package org.terasology.worldviewer.layers.core;

import org.terasology.core.world.CoreBiome;
import org.terasology.core.world.generator.facets.BiomeFacet;
import org.terasology.worldviewer.layers.NominalFacetLayer;

/**
 * Maps {@link CoreBiome} facet to corresponding colors.
 * @author Martin Steiger
 */
public class CoreBiomeFacetLayer extends NominalFacetLayer<CoreBiome> {

    public CoreBiomeFacetLayer() {
        super(BiomeFacet.class, new CoreBiomeColors());
    }
}
