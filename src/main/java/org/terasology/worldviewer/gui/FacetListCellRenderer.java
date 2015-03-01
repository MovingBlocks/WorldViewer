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

package org.terasology.worldviewer.gui;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.terasology.worldviewer.layers.FacetLayer;

/**
 * Renders cells as checkboxes. Interaction does not seem to be possible.
 * @author Martin Steiger
 */
public class FacetListCellRenderer implements ListCellRenderer<FacetLayer> {

    private final JCheckBox checkBox = new JCheckBox();

    @Override
    public Component getListCellRendererComponent(JList<? extends FacetLayer> list, FacetLayer layer, int index, boolean isSelected, boolean cellHasFocus) {
        checkBox.setText(layer.toString());
        checkBox.setSelected(layer.isVisible());
        checkBox.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
        return checkBox;
    }
}
