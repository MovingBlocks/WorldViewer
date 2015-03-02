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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import org.terasology.worldviewer.gui.FacetListCellRenderer;
import org.terasology.worldviewer.gui.ListItemTransferHandler;
import org.terasology.worldviewer.gui.UIBindings;
import org.terasology.worldviewer.layers.FacetLayer;
import org.terasology.worldviewer.layers.FieldFacetLayer;
import org.terasology.worldviewer.layers.GraphFacetLayer;

/**
 * The facet layer configuration panel (at the left)
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
        CardLayout cardLayout = new CardLayout();
        configPanel.setLayout(cardLayout);
        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets.top = 10;
        add(configPanel, gbc.clone());

//        gbc.gridy++;
//        gbc.weighty = 1.0;
//        add(new JPanel(), gbc.clone());

        for (FacetLayer layer : facets) {
            configPanel.add(createConfigs(layer), Integer.toString(System.identityHashCode(layer)));
        }

        facetList.addListSelectionListener(e -> {
            FacetLayer layer = facetList.getSelectedValue();
            String id = Integer.toString(System.identityHashCode(layer));
            cardLayout.show(configPanel, id);
        });
    }

    protected JPanel createConfigs(FacetLayer layer) {
        JPanel panelWrap = new JPanel(new BorderLayout());
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 2));

        if (layer instanceof FieldFacetLayer) {
            FieldFacetLayer fieldLayer = (FieldFacetLayer) layer;
            UIBindings.createSpinner(panel, "Scale", 0, 0.1, 100, () -> fieldLayer.getScale(), v -> fieldLayer.setScale(v));
        }

        if (layer instanceof GraphFacetLayer) {
            GraphFacetLayer graphLayer = (GraphFacetLayer) layer;
            UIBindings.createCheckbox(panel, "Edges", () -> graphLayer.isShowEdges(), v -> graphLayer.setShowEdges(v));
            UIBindings.createCheckbox(panel, "Corners", () -> graphLayer.isShowCorners(), v -> graphLayer.setShowCorners(v));
            UIBindings.createCheckbox(panel, "Bounds", () -> graphLayer.isShowBounds(), v -> graphLayer.setShowBounds(v));
            UIBindings.createCheckbox(panel, "Sites", () -> graphLayer.isShowSites(), v -> graphLayer.setShowSites(v));
            UIBindings.createCheckbox(panel, "Triangles", () -> graphLayer.isShowTris(), v -> graphLayer.setShowTris(v));
        }

        panel.setBorder(new EmptyBorder(0, 5, 0, 0));
        panelWrap.add(panel, BorderLayout.NORTH);
        return panelWrap;
    }
}
