/*
 * Copyright 2015 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License"){ }
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

import java.awt.Graphics2D;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.terasology.context.Context;
import org.terasology.engine.SimpleUri;
import org.terasology.engine.module.ModuleManager;
import org.terasology.registry.CoreRegistry;
import org.terasology.splash.SplashScreenBuilder;
import org.terasology.world.generation.WorldFacet;
import org.terasology.world.generator.UnresolvedWorldGeneratorException;
import org.terasology.world.generator.WorldGenerator;
import org.terasology.world.generator.internal.WorldGeneratorManager;
import org.terasology.world.viewer.layers.FacetLayer;
import org.terasology.world.viewer.layers.FacetLayers;
import org.terasology.worldviewer.config.ViewConfig;
import org.terasology.worldviewer.env.TinyEnvironment;

public class ViewerTest {

    private Context context;

    @Before
    public void setup() throws IOException {
        context = TinyEnvironment.createContext(new SplashScreenBuilder().build());
    }

    @Test
    public void testViewer() throws UnresolvedWorldGeneratorException {
        WorldGeneratorManager worldGeneratorManager = CoreRegistry.get(WorldGeneratorManager.class);
        WorldGenerator worldGen = worldGeneratorManager.createGenerator(new SimpleUri("core:facetedperlin"), context);
        String worldSeed = "asdf";
        worldGen.setWorldSeed(worldSeed);
        worldGen.initialize();

        ModuleManager moduleManager = CoreRegistry.get(ModuleManager.class);

        Set<Class<? extends WorldFacet>> facets = worldGen.getWorld().getAllFacets();
        List<FacetLayer> loadedLayers = FacetLayers.createLayersFor(facets, moduleManager.getEnvironment());

        Viewer viewer = new Viewer(worldGen, loadedLayers, new ViewConfig(), 100);
        viewer.setSize(300, 300);
        Graphics2D g = Mockito.mock(Graphics2D.class);
        viewer.paint(g);
    }
}
