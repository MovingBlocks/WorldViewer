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

package org.terasology.world.viewer.core;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.terasology.world.viewer.layers.FacetLayer;

import com.google.common.collect.ImmutableList;

/**
 * A {@link javax.swing.table.TableModel} that works on a list of {@link FacetLayer}s.
 */
public class FacetTableModel extends AbstractTableModel implements Reorderable {

    private static final long serialVersionUID = -585013620986986118L;

    private final List<FacetLayer> layers;

    private final ImmutableList<String> columnNames = ImmutableList.of("On", "Name");

    /**
     * A list of layers to display. <b>It will be reordered</b>!
     * @param layers the list of layers
     */
    public FacetTableModel(List<FacetLayer> layers) {
        this.layers = layers;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames.get(column);
      }

    @Override
    public int getRowCount() {
        return layers.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        FacetLayer layer = layers.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return Boolean.valueOf(layer.isVisible());

            case 1:
                return layer.toString();

            default:
                return null;
        }
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return getValueAt(0, column).getClass();
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        FacetLayer layer = layers.get(rowIndex);
        switch (columnIndex) {
            case 0:
                layer.setVisible((Boolean) value);
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0;
    }

    @Override
    public void reorder(int fromIndex, int toIndex) {

        FacetLayer layer = layers.get(fromIndex);

        layers.add(toIndex, layer);
        fireTableRowsInserted(toIndex, toIndex);

        if (toIndex < fromIndex) {
            layers.remove(fromIndex + 1);
            fireTableRowsDeleted(fromIndex + 1, fromIndex + 1);
        } else {
            layers.remove(fromIndex);
            fireTableRowsDeleted(fromIndex, fromIndex);
        }

        layer.notifyObservers();
    }

}
