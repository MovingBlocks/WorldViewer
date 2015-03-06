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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.MatteBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.Component;
import org.terasology.rendering.nui.properties.Range;
import org.terasology.world.generator.WorldConfigurator;
import org.terasology.world.generator.WorldGenerator;
import org.terasology.worldviewer.config.Config;
import org.terasology.worldviewer.gui.UIBindings;
import org.terasology.worldviewer.gui.WorldGenCellRenderer;
import org.terasology.worldviewer.lambda.Lambda;

import com.google.common.base.Optional;

public class ConfigPanel extends JPanel {

    private static final long serialVersionUID = -2350103799660220648L;

    private static final Logger logger = LoggerFactory.getLogger(ConfigPanel.class);

    private final TextField seedText;

    public ConfigPanel(WorldGenerator worldGen, Config config) {

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(5, 5, 5, 5));

        JPanel wgSelectPanel = new JPanel();
        wgSelectPanel.setBorder(BorderFactory.createTitledBorder("World Generator"));
        wgSelectPanel.setLayout(new BorderLayout(5, 5));
        String seedString = "sdfsfdf";

        seedText = new TextField(seedString);
        JComboBox<WorldGenerator> wgSelectCombo = new JComboBox<>(new WorldGenerator[] { worldGen } );
        ListCellRenderer<? super WorldGenerator> wgTextRenderer = new WorldGenCellRenderer();
        wgSelectCombo.setRenderer(wgTextRenderer);
        wgSelectPanel.add(wgSelectCombo, BorderLayout.NORTH);
        wgSelectPanel.add(new JLabel("Seed"), BorderLayout.WEST);
        wgSelectPanel.add(seedText, BorderLayout.CENTER);

        add(wgSelectPanel, BorderLayout.NORTH);

        JButton refreshButton = new JButton("Reload");
        refreshButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                worldGen.setWorldSeed(seedText.getText());
                worldGen.initialize();
//                viewer.invalidateWorld();
            }
        });
        refreshButton.setPreferredSize(new Dimension(100, 40));
        add(refreshButton, BorderLayout.SOUTH);

        // trigger an initial refresh
        refreshButton.doClick();

        JPanel configPanel = createConfigPanel(worldGen);
        add(configPanel, BorderLayout.CENTER);
    }

    private JPanel createConfigPanel(WorldGenerator worldGen)
    {
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

        Optional<WorldConfigurator> configOpt = worldGen.getConfigurator();
        if (configOpt.isPresent()) {
            WorldConfigurator configurator = configOpt.get();
            for (Entry<String, Component> entry : configurator.getProperties().entrySet()) {
                String label = entry.getKey();
                Component ccomp = entry.getValue();

                JLabel caption = new JLabel(" " + label, SwingConstants.LEADING); // add a little space for the label text
                caption.setFont(caption.getFont().deriveFont(Font.BOLD));
                caption.setBorder(new MatteBorder(0, 0, 1, 0, Color.GRAY));
                configPanel.add(caption, gbc.clone());

                processComponent(configPanel, ccomp);
            }
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

        Range range = field.getAnnotation(Range.class);
        if (range != null) {
            gbc.insets.left = 5;
            gbc.insets.right = 5;
            gbc.gridx = 0;
            JLabel label = new JLabel(range.label().isEmpty() ? "<undefined>" : range.label());
            parent.add(label, gbc.clone());
            label.setToolTipText(range.description());
            double min = range.min();
            double max = range.max();
            double stepSize = range.increment();
            Supplier<Double> getter = Lambda.toRuntime(() -> field.getDouble(obj));
            Consumer<Double> setter = Lambda.toRuntime(v -> field.setFloat(obj, v.floatValue()));
            gbc.insets.left = 5;
            gbc.insets.right = 5;
            gbc.gridx = 1;
            JSpinner spinner = UIBindings.createSpinner(min, stepSize, max, getter, setter);
            spinner.setToolTipText(range.description());
            parent.add(spinner, gbc.clone());
        }
    }
}
