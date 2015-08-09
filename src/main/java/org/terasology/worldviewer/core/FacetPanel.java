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
import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.lang.reflect.Field;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.world.viewer.layers.FacetLayer;
import org.terasology.world.viewer.layers.FacetLayerConfig;
import org.terasology.worldviewer.gui.UIBindings;

/**
 * The facet layer configuration panel (at the left)
 */
public class FacetPanel extends JPanel {

    private static final long serialVersionUID = -4395448394330407251L;

    private static final Logger logger = LoggerFactory.getLogger(FacetPanel.class);

    private final JPanel configPanel;

    public FacetPanel(List<FacetLayer> facets) {
        setBorder(BorderFactory.createEtchedBorder());
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        TableModel listModel = new FacetTableModel(facets);
        JTable facetList = new JTable(listModel);

        for (FacetLayer facetLayer : facets) {
            facetLayer.addObserver(layer -> facetList.repaint());
        }

        facetList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        facetList.setTransferHandler(new TableRowTransferHandler(facetList));
        facetList.setDropMode(DropMode.INSERT_ROWS);
        facetList.setDragEnabled(true);
        facetList.getColumnModel().getColumn(0).setMaxWidth(25);
        facetList.getColumnModel().getColumn(0).setResizable(false);
        facetList.getTableHeader().setReorderingAllowed(false);
        add(facetList.getTableHeader(), gbc.clone());
        gbc.gridy++;
        add(facetList, gbc.clone());

        JLabel listInfoText = new JLabel("Drag layers to change rendering order");
        listInfoText.setAlignmentX(0.5f);
        gbc.gridy++;
        add(listInfoText, gbc.clone());

        configPanel = new JPanel();
        configPanel.setBorder(BorderFactory.createTitledBorder("Config"));
        CardLayout cardLayout = new CardLayout();
        configPanel.setLayout(cardLayout);
        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets.top = 10;
        add(configPanel, gbc.clone());

        for (FacetLayer layer : facets) {
            configPanel.add(createConfigs(layer), Integer.toString(System.identityHashCode(layer)));
        }

        facetList.getSelectionModel().addListSelectionListener(e -> {
            int selIdx = facetList.getSelectedRow();
            if (selIdx > -1) {
                FacetLayer layer = facets.get(selIdx);
                String id = Integer.toString(System.identityHashCode(layer));
                cardLayout.show(configPanel, id);
            }
        });
    }

    protected JPanel createConfigs(FacetLayer layer) {
        JPanel panelWrap = new JPanel(new BorderLayout());
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 2));

        FacetLayerConfig config = layer.getConfig();
        if (config != null) {
            for (Field field : config.getClass().getDeclaredFields()) {

                if (field.getAnnotations().length > 0) {
                    field.setAccessible(true);

                    processAnnotations(panel, layer, field);
                }
            }
        }

        panel.setBorder(new EmptyBorder(0, 5, 0, 0));
        panelWrap.add(panel, BorderLayout.NORTH);
        return panelWrap;
    }

    private void processAnnotations(JPanel panel, FacetLayer layer, Field field) {
        FacetLayerConfig config = layer.getConfig();
        JComponent comp = null;

        JSpinner spinner = UIBindings.processRangeAnnotation(config, field);
        if (spinner != null) {
            spinner.addChangeListener(event -> {
                Number v = (Number) spinner.getValue();
                try {
                    if (field.getType().equals(int.class) || field.getType().equals(Integer.class)) {
                        field.setInt(config, v.intValue());
                    } else {
                        field.setFloat(config, v.floatValue());
                    }
                    layer.notifyObservers();
                } catch (IllegalAccessException e) {
                    logger.warn("Could not set field '{}:{}'", layer, field, e);
                }
            });
            comp = spinner;
        }

        JCheckBox checkbox = UIBindings.processCheckboxAnnotation(config, field, "visible");
        if (checkbox != null) {
            checkbox.addChangeListener(event -> {
                try {
                    field.setBoolean(config, checkbox.isSelected());
                    layer.notifyObservers();
                } catch (IllegalAccessException e) {
                    logger.warn("Could not set field '{}:{}'", layer, field, e);
                }
            });
            comp = checkbox;
        }

        JComboBox<?> listCombo = UIBindings.processListAnnotation(config, field);
        if (listCombo != null) {
            listCombo.addActionListener(event -> {
                String v = listCombo.getSelectedItem().toString(); // this should be a String already
                try {
                    field.set(config, v);
                    layer.notifyObservers();
                } catch (IllegalAccessException e) {
                    logger.warn("Could not set field '{}:{}'", layer, field, e);
                }
            });
            comp = listCombo;
        }

        JComboBox<?> enumCombo = UIBindings.processEnumAnnotation(config, field);
        if (enumCombo != null) {
            enumCombo.addActionListener(event -> {
                String v = enumCombo.getSelectedItem().toString(); // this should be a String already
                try {
                    field.set(config, v);
                    layer.notifyObservers();
                } catch (IllegalAccessException e) {
                    logger.warn("Could not set field '{}:{}'", layer, field, e);
                }
            });
            comp = enumCombo;
        }

        if (comp != null) {
            JLabel label = new JLabel(comp.getName());
            label.setToolTipText(comp.getToolTipText());

            panel.add(label);
            panel.add(comp);
        }
    }
}
