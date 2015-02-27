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

import java.awt.Checkbox;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.terasology.world.generation.WorldFacet;

/**
 * TODO Type description
 * @author Martin Steiger
 */
public class FacetPanel extends JPanel {

    private static final long serialVersionUID = -4395448394330407251L;

    public FacetPanel(FacetConfig facetMap) {
        setBorder(BorderFactory.createEtchedBorder());
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        for (FacetLayer facetLayer : facetMap.getLayers()) {
            Checkbox checkBox = new Checkbox(facetLayer.toString());
            checkBox.addItemListener(e -> facetMap.setVisible(facetLayer, e.getStateChange() == ItemEvent.SELECTED));
            add(checkBox);
        }

        JPanel configPanel = new JPanel();
        configPanel.setLayout(new GridLayout(0, 2));
        configPanel.setBorder(BorderFactory.createTitledBorder("Config"));
      final SpinnerNumberModel model = new SpinnerNumberModel(1.0, 0.0, 1000.0, 0.1);
      final JSpinner scaleSpinner = new JSpinner(model);
//      scaleSpinner.addChangeListener(new ChangeListener() {
//
//          @Override
//          public void stateChanged(ChangeEvent e) {
//              int index = facetCombo.getSelectedIndex();
//              FacetTrait item = facetCombo.getItemAt(index);
//              FieldFacetTrait trait = (FieldFacetTrait) item;
//              Double value = (Double) model.getValue();
//              trait.setScale(value.doubleValue());
//
//              FacetPanel.this.firePropertyChange("repaint", null, null);
//          }
//      });
      configPanel.add(new JLabel("Scale"));
      configPanel.add(scaleSpinner);
      add(configPanel);
    }
}
