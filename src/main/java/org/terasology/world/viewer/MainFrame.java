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

package org.terasology.world.viewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.context.Context;
import org.terasology.engine.Observer;
import org.terasology.engine.module.ModuleManager;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.generation.WorldFacet;
import org.terasology.world.generator.WorldGenerator;
import org.terasology.world.viewer.config.Config;
import org.terasology.world.viewer.core.ConfigPanel;
import org.terasology.world.viewer.core.FacetPanel;
import org.terasology.world.viewer.core.Viewer;
import org.terasology.world.viewer.layers.FacetLayer;
import org.terasology.world.viewer.layers.FacetLayers;
import org.terasology.world.viewer.camera.Camera;

import com.google.common.collect.Lists;

/**
 * The main MapViewer JFrame
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = -8474971565041036025L;

    private static final Logger logger = LoggerFactory.getLogger(MainFrame.class);

    private static final int MAX_TILES = 3000;

    private final Config config;
    private final Timer statusBarTimer;

    /**
     * A thread-safe list (required for parallel tile rendering)
     */
    private List<FacetLayer> layerList;

    private final Viewer viewer;
    private final FacetPanel layerPanel;
    private final ConfigPanel configPanel;
    private final JPanel statusBar = new JPanel();


    public MainFrame(Context context, Config config) {

        this.config = config;

        configPanel = new ConfigPanel(context, config);
        WorldGenerator worldGen = configPanel.getWorldGen();

        configPanel.addObserver(new Observer<WorldGenerator>() {

            private WorldGenerator oldWorldGen = worldGen;

            @Override
            public void update(WorldGenerator wg) {
                if (wg != oldWorldGen) {
                    config.storeLayers(oldWorldGen.getUri(), layerList);
                    reload(wg);
                    oldWorldGen = wg;
                }
            }
        });

        layerPanel = new FacetPanel();

        viewer = new Viewer(config.getViewConfig(), MAX_TILES);

        reload(worldGen);

        configPanel.addObserver(wg -> viewer.invalidateWorld());

        add(layerPanel, BorderLayout.EAST);
        add(configPanel, BorderLayout.WEST);
        add(viewer, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        JLabel cameraLabel = new JLabel();
        cameraLabel.setPreferredSize(new Dimension(170, 0));
        JLabel tileCountLabel = new JLabel();
        tileCountLabel.setPreferredSize(new Dimension(220, 0));
        JLabel memoryLabel = new JLabel();
        memoryLabel.setPreferredSize(new Dimension(140, 0));
        statusBarTimer = new Timer(50, event -> {
            Camera camera = viewer.getCamera();
            int camX = (int) camera.getPos().getX();
            int camZ = (int) camera.getPos().getY();
            int zoom = (int) (camera.getZoom() * 100);
            cameraLabel.setText(String.format("Camera: %d/%d at %d%%", camX, camZ, zoom));

            int pendingTiles = viewer.getPendingTiles();
            int cachedTiles = viewer.getCachedTiles();
            tileCountLabel.setText(String.format("Tiles: %d/%d cached, %d queued", cachedTiles, MAX_TILES, pendingTiles));

            Runtime runtime = Runtime.getRuntime();
            long maxMem = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMem = runtime.freeMemory();
            long allocMemory = totalMemory - freeMem;
            long oneMeg = 1024 * 1024;
            memoryLabel.setText(String.format("Memory: %d/%d MB", allocMemory / oneMeg, maxMem / oneMeg));
        });
        statusBarTimer.setInitialDelay(0);
        statusBarTimer.start();

        statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.LINE_AXIS));
        statusBar.add(new JLabel("Drag with right mouse button to pan, mouse wheel to zoom"));
        statusBar.add(Box.createHorizontalGlue());
        statusBar.add(cameraLabel);
        statusBar.add(Box.createHorizontalGlue());
        statusBar.add(tileCountLabel);
        statusBar.add(Box.createHorizontalStrut(20));
        statusBar.add(memoryLabel);
        statusBar.setBorder(new EmptyBorder(2, 5, 2, 5));

        setMinimumSize(new Dimension(850, 530));
    }

    private void reload(WorldGenerator worldGen) {

        Set<Class<? extends WorldFacet>> facets = worldGen.getWorld().getAllFacets();

        ModuleManager moduleManager = CoreRegistry.get(ModuleManager.class);

        // Create with default values first
        List<FacetLayer> loadedLayers = FacetLayers.createLayersFor(facets, moduleManager.getEnvironment());

        // Then try to replace them with those from the config file
        try {
            loadedLayers = config.loadLayers(worldGen.getUri(), loadedLayers);
        } catch (RuntimeException e) {
            logger.warn("Could not load layers - using default", e);
        }

        // assign to thread-safe implementation
        layerList = Lists.newCopyOnWriteArrayList(loadedLayers);

        viewer.setWorldGen(worldGen, layerList);

        layerPanel.setLayers(layerList);
    }

    @Override
    public void dispose() {
        super.dispose();

        statusBarTimer.stop();

        viewer.close();

        config.storeLayers(config.getWorldConfig().getWorldGen(), layerList);
    }

}
