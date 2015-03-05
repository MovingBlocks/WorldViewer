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

package org.terasology.worldviewer;

import java.awt.BorderLayout;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.core.world.CoreBiome;
import org.terasology.core.world.generator.facets.BiomeFacet;
import org.terasology.core.world.generator.facets.TreeFacet;
import org.terasology.polyworld.biome.WhittakerBiome;
import org.terasology.polyworld.biome.WhittakerBiomeFacet;
import org.terasology.polyworld.voronoi.GraphFacet;
import org.terasology.world.generation.WorldFacet;
import org.terasology.world.generation.facets.base.FieldFacet2D;
import org.terasology.world.generator.WorldGenerator;
import org.terasology.worldviewer.config.Config;
import org.terasology.worldviewer.config.ConfigStore;
import org.terasology.worldviewer.core.CoreBiomeColors;
import org.terasology.worldviewer.core.FacetPanel;
import org.terasology.worldviewer.core.Viewer;
import org.terasology.worldviewer.core.WhittakerBiomeColors;
import org.terasology.worldviewer.env.TinyEnvironment;
import org.terasology.worldviewer.layers.FacetLayer;
import org.terasology.worldviewer.layers.FieldFacetLayer;
import org.terasology.worldviewer.layers.GraphFacetLayer;
import org.terasology.worldviewer.layers.NominalFacetLayer;
import org.terasology.worldviewer.layers.TreeFacetLayer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * The main MapViewer JFrame
 * @author Martin Steiger
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = -8474971565041036025L;

    private static final Logger logger = LoggerFactory.getLogger(MainFrame.class);

    private static final Path CONFIG_PATH = Paths.get(System.getProperty("user.home"), ".worldviewer.json");

    private final JPanel statusBar = new JPanel();

    private final Config config;

    private final Viewer viewer;

    private final TextField seedText;

    private Timer memoryTimer;

    public MainFrame(WorldGenerator worldGen) {

//        FullEnvironment.setup();
        TinyEnvironment.setup();

        config = ConfigStore.load(CONFIG_PATH);

        String seedString = "sdfsfdf";
        worldGen.setWorldSeed(seedString);
        worldGen.initialize();

        JPanel configPanel = new JPanel();
        BoxLayout layout = new BoxLayout(configPanel, BoxLayout.LINE_AXIS);
        configPanel.setLayout(layout);
        configPanel.setBorder(new EmptyBorder(2, 5, 2, 5));

        List<FacetLayer> facets = Lists.newArrayList();
        for (Class<? extends WorldFacet> facet : worldGen.getWorld().getAllFacets()) {
            facets.addAll(getLayers(facet));
        }

        viewer = new Viewer(worldGen, facets, config.getViewConfig());

        seedText = new TextField(seedString);

        configPanel.add(Box.createHorizontalGlue());
        configPanel.add(new JLabel("Seed"));
        configPanel.add(Box.createHorizontalStrut(5));
        configPanel.add(seedText);
        configPanel.add(Box.createHorizontalStrut(5));

        JButton refreshButton = new JButton("Reload");
        refreshButton.setFocusable(false);
        refreshButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                worldGen.setWorldSeed(seedText.getText());
                worldGen.initialize();
                viewer.invalidateWorld();
            }
        });
        configPanel.add(refreshButton);

        JPanel facetPanel = new FacetPanel(facets);
        add(facetPanel, BorderLayout.WEST);
        add(configPanel, BorderLayout.NORTH);
        add(viewer, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        JLabel memoryLabel = new JLabel();
        memoryTimer = new Timer(500, event -> {
            Runtime runtime = Runtime.getRuntime();
            long maxMem = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMem = runtime.freeMemory();
            long allocMemory = (totalMemory - freeMem);
            memoryLabel.setText(String.format("Memory: %d/%d MB", allocMemory / (1024 * 1024), maxMem / (1024 * 1024)));
        });
        memoryTimer.setInitialDelay(0);
        memoryTimer.start();

        statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.LINE_AXIS));
        statusBar.add(memoryLabel);
        statusBar.add(Box.createHorizontalGlue());
        statusBar.add(new JLabel("Use cursor arrows or drag with right mouse button to navigate"));
        statusBar.setBorder(new EmptyBorder(2, 5, 2, 5));
    }

    @SuppressWarnings("unchecked")
    private static Collection<FacetLayer> getLayers(Class<? extends WorldFacet> facetClass) {

        List<FacetLayer> result = Lists.newArrayList();

        Map<Class<?>, Function<Class<?>, FacetLayer>> mapping = Maps.newHashMap();

        mapping.put(FieldFacet2D.class,
                clazz -> new FieldFacetLayer((Class<FieldFacet2D>) clazz, 0, 5));

        mapping.put(WhittakerBiomeFacet.class,
                clazz -> new NominalFacetLayer<WhittakerBiome>((Class<WhittakerBiomeFacet>) clazz, new WhittakerBiomeColors()));

        mapping.put(BiomeFacet.class,
                clazz -> new NominalFacetLayer<CoreBiome>((Class<BiomeFacet>) clazz, new CoreBiomeColors()));

        mapping.put(GraphFacet.class,
                clazz -> new GraphFacetLayer());

        mapping.put(TreeFacet.class,
                clazz -> new TreeFacetLayer());

        for (Class<?> clazz : mapping.keySet()) {
            if (clazz.isAssignableFrom(facetClass)) {
                result.add(mapping.get(clazz).apply(facetClass));
            }
        }

//        if (ObjectFacet2D.class.isAssignableFrom(facetClass)) {
//            Class<ObjectFacet2D<Object>> cast = (Class<ObjectFacet2D<Object>>) facetClass;
//            result.add(new NominalFacetLayer<Object>(cast, new RandomObjectColors()));
//        }

        if (result.isEmpty()) {
            logger.warn("No layers found for facet {}", facetClass.getName());
        }

        return result;
    }

    @Override
    public void dispose() {
        super.dispose();

        memoryTimer.stop();

        viewer.close();

        ConfigStore.save(CONFIG_PATH, config);

        // just in case some other thread is still running
        System.exit(0);
    }

}
