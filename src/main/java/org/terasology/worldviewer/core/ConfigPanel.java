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

package org.terasology.worldviewer.core;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.Observer;
import org.terasology.entitySystem.Component;
import org.terasology.world.generator.WorldConfigurator;
import org.terasology.world.generator.WorldGenerator;
import org.terasology.worldviewer.WorldGenerators;
import org.terasology.worldviewer.config.Config;
import org.terasology.worldviewer.gui.UIBindings;

import com.google.common.collect.Lists;

public class ConfigPanel extends JPanel {

    private static final long serialVersionUID = -2350103799660220648L;

    private static final Logger logger = LoggerFactory.getLogger(ConfigPanel.class);

    private final List<Observer<WorldGenerator>> observers = Lists.newArrayList();

    private final WorldGenerator worldGen;

    public ConfigPanel(WorldGenerator worldGen, Config config) {

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(5, 5, 5, 5));

        JPanel wgSelectPanel = new JPanel(new GridBagLayout());
        wgSelectPanel.setBorder(BorderFactory.createTitledBorder("World Generator"));

        this.worldGen = worldGen;

        String worldSeed = worldGen.getWorldSeed();
        String wgName = WorldGenerators.getAnnotatedDisplayName(worldGen.getClass());
        int seaLevel = worldGen.getWorld().getSeaLevel();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 0;
        wgSelectPanel.add(new JLabel("Generator Type:"), gbc.clone());
        wgSelectPanel.add(new JLabel(wgName), gbc.clone());
        gbc.gridy = 1;
        wgSelectPanel.add(new JLabel("World Seed:"), gbc.clone());
        wgSelectPanel.add(new JLabel(worldSeed), gbc.clone());
        gbc.gridy = 2;
        wgSelectPanel.add(new JLabel("Sea Level Height:"), gbc.clone());
        wgSelectPanel.add(new JLabel(seaLevel + " blocks"), gbc.clone());

        add(wgSelectPanel, BorderLayout.NORTH);

        JPanel configPanel = createConfigPanel();
        add(configPanel, BorderLayout.CENTER);
    }

    /**
     * Adds an observer
     * @param obs the observer to add
     */
    public void addObserver(Observer<WorldGenerator> obs) {
        observers.add(obs);
    }

    public void removeObserver(Observer<WorldGenerator> obs) {
        observers.remove(obs);
    }

    private void notifyObservers() {
        for (Observer<WorldGenerator> obs : observers) {
            obs.update(worldGen);
        }
    }

    private JPanel createConfigPanel() {
        JPanel configPanel = new JPanel();
        configPanel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.insets.top = 10;
        gbc.insets.bottom = 5;

        WorldConfigurator configurator = worldGen.getConfigurator();
        for (Entry<String, Component> entry : configurator.getProperties().entrySet()) {
            String label = entry.getKey();
            Component ccomp = entry.getValue();

            JLabel caption = new JLabel(" " + label, SwingConstants.LEADING); // add a little space for the label text
            caption.setFont(caption.getFont().deriveFont(Font.BOLD));
            caption.setBorder(new MatteBorder(0, 0, 1, 0, Color.GRAY));
            configPanel.add(caption, gbc.clone());

            processComponent(configPanel, ccomp);
        }

        return configPanel;
    }

    private void processComponent(Container panel, Component ccomp) {
        for (Field field : ccomp.getClass().getDeclaredFields()) {
            Annotation[] anns = field.getAnnotations();
            // check only on annotated fields
            if (anns.length > 0) {
                try {
                    field.setAccessible(true);
                    process(panel, ccomp, field);
                } catch (IllegalArgumentException e) {
                    logger.warn("Could not access field \"{}-{}\"", ccomp.getClass(), field.getName(), e);
                }
            }
        }
    }

    private void process(Container parent, Object obj, Field field) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JComponent comp = null;

        JSpinner spinner = UIBindings.processRangeAnnotation(obj, field);
        if (spinner != null) {
            spinner.addChangeListener(event -> notifyObservers());
            comp = spinner;
        }

        JCheckBox checkbox = UIBindings.processCheckboxAnnotation(obj, field, "active");
        if (checkbox != null) {
            checkbox.addChangeListener(event -> notifyObservers());
            comp = checkbox;
        }

        JComboBox<?> listCombo = UIBindings.processListAnnotation(obj, field);
        if (listCombo != null) {
            listCombo.addActionListener(event -> notifyObservers());
            comp = listCombo;
        }

        JComboBox<?> enumCombo = UIBindings.processEnumAnnotation(obj, field);
        if (enumCombo != null) {
            enumCombo.addActionListener(event -> notifyObservers());
            comp = enumCombo;
        }

        if (comp != null) {
            gbc.insets.left = 5;
            gbc.insets.right = 5;
            gbc.gridx = 0;
            JLabel label = new JLabel(comp.getName());
            label.setToolTipText(comp.getToolTipText());
            parent.add(label, gbc.clone());
            gbc.insets.left = 5;
            gbc.insets.right = 5;
            gbc.gridx = 1;
            parent.add(comp, gbc.clone());
        }
    }
}
