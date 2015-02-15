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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.SimpleUri;
import org.terasology.world.generation.WorldFacet;
import org.terasology.world.generation.facets.base.FieldFacet2D;
import org.terasology.world.generation.facets.base.ObjectFacet2D;
import org.terasology.world.generator.WorldGenerator;
import org.terasology.worldviewer.config.Config;
import org.terasology.worldviewer.config.ConfigStore;
import org.terasology.worldviewer.core.FacetTrait;
import org.terasology.worldviewer.core.FieldFacetTrait;
import org.terasology.worldviewer.core.NominalFacetTrait;
import org.terasology.worldviewer.core.Viewer;
import org.terasology.worldviewer.env.TinyEnvironment;
import org.terasology.worldviewer.render.RandomObjectColors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

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

    private final JComboBox<FacetTrait> facetCombo;

    public MainFrame(WorldGenerator worldGen) {

//        FullEnvironment.setup();
        TinyEnvironment.setup();

        config = ConfigStore.load(CONFIG_PATH);

        worldGen.setWorldSeed("sdfsfdf");
        worldGen.initialize();

        viewer = new Viewer(worldGen, config.getViewConfig());

        JPanel configPanel = new JPanel();
        BoxLayout layout = new BoxLayout(configPanel, BoxLayout.LINE_AXIS);
        configPanel.setLayout(layout);
        configPanel.setBorder(new EmptyBorder(2, 5, 2, 5));

        facetCombo = createFacetCombo(worldGen.getWorld().getAllFacets());

//        facetCombo.addItem(new NominalFacetTrait<WhittakerBiome>(WhittakerBiomeFacet.class, new WhittakerBiomeColors()));
//        facetCombo.addItem(new NominalFacetTrait<CoreBiome>(BiomeFacet.class, new CoreBiomeColors()));

        facetCombo.setFocusable(false);
        facetCombo.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int index = facetCombo.getSelectedIndex();
                FacetTrait item = facetCombo.getItemAt(index);
                viewer.setFacetTrait(item);
            }
        });
        configPanel.add(facetCombo);

        configPanel.add(Box.createHorizontalGlue());

        final SpinnerNumberModel model = new SpinnerNumberModel(1.0, 0.0, 1000.0, 0.1);
        final JSpinner scaleSpinner = new JSpinner(model);
        scaleSpinner.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                int index = facetCombo.getSelectedIndex();
                FacetTrait item = facetCombo.getItemAt(index);
                FieldFacetTrait trait = (FieldFacetTrait) item;
                Double value = (Double) model.getValue();
                trait.setScale(value.doubleValue());
                viewer.reload();
            }
        });

        configPanel.add(scaleSpinner);
        configPanel.add(Box.createHorizontalStrut(5));

        facetCombo.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int index = facetCombo.getSelectedIndex();
                FacetTrait item = facetCombo.getItemAt(index);
                if (item instanceof FieldFacetTrait) {
                    FieldFacetTrait trait = (FieldFacetTrait) item;
                    scaleSpinner.setValue(trait.getScale());
                    scaleSpinner.setEnabled(true);
                } else {
                    scaleSpinner.setEnabled(false);
                }
            }
        });

        String defFacet = config.getWorldConfig().getDefaultFacetClass();
        int defIndex = findTraitFor(facetCombo, defFacet);

        facetCombo.setSelectedIndex(defIndex >= 0 ? defIndex : facetCombo.getItemCount() - 1);

        JButton refreshButton = new JButton("Reload");
        refreshButton.setFocusable(false);
        refreshButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                viewer.reload();
            }
        });
        configPanel.add(refreshButton);

        add(configPanel, BorderLayout.NORTH);
        add(viewer, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.LINE_AXIS));
        statusBar.add(new JLabel("Ready"));
        statusBar.add(Box.createHorizontalGlue());
        statusBar.add(new JLabel("Use cursor arrows or drag with right mouse button to navigate"));
        statusBar.setBorder(new EmptyBorder(2, 5, 2, 5));
    }

    private static int findTraitFor(JComboBox<FacetTrait> facetCombo, String defFacet) {
        for (int i = 0; i < facetCombo.getItemCount(); i++) {
            Class<?> facetClass = facetCombo.getItemAt(i).getFacetClass();
            if (facetClass.getName().equals(defFacet)) {
                return i;
            }
        }

        return  -1;
    }

    /**
     * @param allFacets
     * @return
     */
    private static JComboBox<FacetTrait> createFacetCombo(Set<Class<? extends WorldFacet>> facets) {
        JComboBox<FacetTrait> facetCombo = new JComboBox<FacetTrait>();

        List<FacetTrait> traits = Lists.newArrayList();
        for (Class<? extends WorldFacet> facetClass : facets) {
            FacetTrait trait = getTrait(facetClass);
            if (trait != null) {
                traits.add(trait);
            } else {
                logger.info("Could not find mapping for {}", facetClass);
            }
        }

        // sort alphabetically with respect to facet class name
        Collections.sort(traits, new Comparator<FacetTrait>() {

            @Override
            public int compare(FacetTrait o1, FacetTrait o2) {
                return o1.getFacetClass().getName().compareTo(o2.getFacetClass().getName());
            }
        });

        for (FacetTrait trait : traits) {
            facetCombo.addItem(trait);
        }

        return facetCombo;
    }

    @SuppressWarnings("unchecked")
    private static FacetTrait getTrait(Class<? extends WorldFacet> facetClass) {
        if (FieldFacet2D.class.isAssignableFrom(facetClass)) {
            Class<FieldFacet2D> cast = (Class<FieldFacet2D>) facetClass;
            return new FieldFacetTrait(cast, 0, 3);
        }

        if (ObjectFacet2D.class.isAssignableFrom(facetClass)) {
            Class<ObjectFacet2D<Object>> cast = (Class<ObjectFacet2D<Object>>) facetClass;
            return new NominalFacetTrait<Object>(cast, new RandomObjectColors());
        }

        return null;
    }

    @Override
    public void dispose() {
        super.dispose();

        viewer.close();

        int selectedIndex = facetCombo.getSelectedIndex();
        if (selectedIndex >= 0) {
            FacetTrait trait = facetCombo.getItemAt(selectedIndex);
            String name = trait.getFacetClass().getName();
            config.getWorldConfig().setDefaultFacetClass(name);
        }

        ConfigStore.save(CONFIG_PATH, config);

        // just in case some other thread is still running
        System.exit(0);
    }

}
