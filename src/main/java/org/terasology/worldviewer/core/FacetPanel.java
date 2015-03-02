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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.terasology.worldviewer.gui.FacetListCellRenderer;
import org.terasology.worldviewer.gui.ListItemTransferHandler;
import org.terasology.worldviewer.layers.FacetLayer;
import org.terasology.worldviewer.layers.FieldFacetLayer;
import org.terasology.worldviewer.layers.GraphFacetLayer;

/**
 * TODO Type description
 * @author Martin Steiger
 */
public class FacetPanel extends JPanel {

    private static final long serialVersionUID = -4395448394330407251L;

    private final JPanel configPanel;

    public FacetPanel(List<FacetLayer> facets) {
        setBorder(BorderFactory.createEtchedBorder());
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        add(new JLabel("Layers"), gbc.clone());

        DefaultListModel<FacetLayer> listModel = new DefaultListModel<FacetLayer>();
        JList<FacetLayer> facetList = new JList<>(listModel);

        for (FacetLayer facetLayer : facets) {
            listModel.addElement(facetLayer);
            facetLayer.addObserver(layer -> facetList.repaint());
        }

        facetList.setBorder(BorderFactory.createEtchedBorder());
        facetList.setCellRenderer(new FacetListCellRenderer());

        facetList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                // respond to double clicks only
                if (e.getClickCount() == 2) {
                    int index = facetList.locationToIndex(e.getPoint());
                    if (index != -1) {
                        FacetLayer layer = facetList.getModel().getElementAt(index);
                        layer.setVisible(!layer.isVisible());
                    }
                }
            }
        });

        facetList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        facetList.setTransferHandler(new ListItemTransferHandler<FacetLayer>());
        facetList.setDropMode(DropMode.INSERT);
        facetList.setDragEnabled(true);
        gbc.gridy++;
        add(facetList, gbc.clone());

        JLabel listInfoText = new JLabel("Double-click to toggle; drag to reorder");
        listInfoText.setAlignmentX(0.5f);
        gbc.gridy++;
        add(listInfoText, gbc.clone());

        configPanel = new JPanel();
        configPanel.setBorder(BorderFactory.createTitledBorder("Config"));
        configPanel.setLayout(new GridLayout(0, 2));
        gbc.gridy++;
        gbc.insets.top = 10;
        add(configPanel, gbc.clone());

        gbc.gridy++;
        gbc.weighty = 1.0;
        add(new JPanel(), gbc.clone());

        facetList.addListSelectionListener(e -> updateConfigs(facetList.getSelectedValue()));
    }

    protected void updateConfigs(FacetLayer layer) {
        configPanel.removeAll();

        if (layer instanceof FieldFacetLayer) {
            FieldFacetLayer fieldLayer = (FieldFacetLayer) layer;
            createSpinner("Scale", 0, 0.1, 100, () -> fieldLayer.getScale(), v -> fieldLayer.setScale(v));
        }

        if (layer instanceof GraphFacetLayer) {
            GraphFacetLayer graphLayer = (GraphFacetLayer) layer;
            createCheckbox("Edges", () -> graphLayer.isShowEdges(), v -> graphLayer.setShowEdges(v));
            createCheckbox("Corners", () -> graphLayer.isShowCorners(), v -> graphLayer.setShowCorners(v));
            createCheckbox("Bounds", () -> graphLayer.isShowBounds(), v -> graphLayer.setShowBounds(v));
            createCheckbox("Sites", () -> graphLayer.isShowSites(), v -> graphLayer.setShowSites(v));
            createCheckbox("Triangles", () -> graphLayer.isShowTris(), v -> graphLayer.setShowTris(v));
        }

        configPanel.revalidate();
    }

    private void createCheckbox(String name, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        JCheckBox checkBox = new JCheckBox("visible");
        checkBox.setSelected(getter.get());
        checkBox.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentShown(ComponentEvent e) {
                checkBox.setSelected(getter.get());
            }
        });
        checkBox.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                setter.accept(checkBox.isSelected());
            }
        });

        configPanel.add(new JLabel(name));
        configPanel.add(checkBox);
    }

    private void createSpinner(String name, double min, double stepSize, double max, Supplier<Double> getter, Consumer<Double> setter) {
        double initValue = getter.get().doubleValue();

        final SpinnerNumberModel model = new SpinnerNumberModel(initValue, min, max, stepSize);
        final JSpinner spinner = new JSpinner(model);
        spinner.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentShown(ComponentEvent e) {
                spinner.setValue(getter.get());
            }
        });
        spinner.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                Double value = (Double) model.getValue();
                setter.accept(value);
            }
        });
        configPanel.add(new JLabel(name));
        configPanel.add(spinner);
    }
}
