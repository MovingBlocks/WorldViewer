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

import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
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

/**
 * TODO Type description
 * @author Martin Steiger
 */
public class FacetPanel extends JPanel {

    private static final long serialVersionUID = -4395448394330407251L;

    private final JPanel configPanel;

    private final FacetConfig facetConfig;

    public FacetPanel(FacetConfig facetConfig) {
        setBorder(BorderFactory.createEtchedBorder());
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        this.facetConfig = facetConfig;

        DefaultListModel<FacetLayer> listModel = new DefaultListModel<FacetLayer>();

        for (FacetLayer facetLayer : facetConfig.getLayers()) {
            listModel.addElement(facetLayer);
        }

        JList<FacetLayer> facetList = new JList<>(listModel);
        facetList.setBorder(BorderFactory.createEtchedBorder());
        facetList.setCellRenderer(new FacetListCellRenderer());
        add(facetList);

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

        facetConfig.addObserver(layer -> facetList.repaint());
        facetList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        facetList.setTransferHandler(new ListItemTransferHandler<FacetLayer>());
        facetList.setDropMode(DropMode.INSERT);
        facetList.setDragEnabled(true);

        JLabel listInfoText = new JLabel("Double-click to toggle; drag to reorder");
        listInfoText.setAlignmentX(0.5f);
        add(listInfoText);

        add(Box.createVerticalStrut(20));

        configPanel = new JPanel();
        configPanel.setBorder(BorderFactory.createTitledBorder("Config"));
        configPanel.setLayout(new GridLayout(0, 2));
        add(configPanel);

        facetList.addListSelectionListener(e -> updateConfigs(facetList.getSelectedValue()));
    }

    protected void updateConfigs(FacetLayer layer) {
        configPanel.removeAll();

        if (layer instanceof FieldFacetTrait) {
            FieldFacetTrait fieldLayer = (FieldFacetTrait) layer;
            double scale = fieldLayer.getScale();
            final SpinnerNumberModel model = new SpinnerNumberModel(scale, 0.0, 1000.0, 0.1);
            final JSpinner scaleSpinner = new JSpinner(model);
            scaleSpinner.addChangeListener(new ChangeListener() {

                @Override
                public void stateChanged(ChangeEvent e) {
                    Double value = (Double) model.getValue();
                    fieldLayer.setScale(value.doubleValue());
                    facetConfig.notifyObservers(layer);
                }
            });
            configPanel.add(new JLabel("Scale"));
            configPanel.add(scaleSpinner);
        }

        configPanel.revalidate();
    }
}
