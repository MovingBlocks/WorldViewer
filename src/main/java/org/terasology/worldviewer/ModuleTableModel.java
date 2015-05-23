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

package org.terasology.worldviewer;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.terasology.module.ArchiveModule;
import org.terasology.module.Module;
import org.terasology.module.PathModule;

import com.google.common.collect.ImmutableList;

/**
 * A swing-based table model for modules.
 * @author Martin Steiger
 */
public class ModuleTableModel extends AbstractTableModel {

    private static final long serialVersionUID = -2702157486079272558L;
    private final List<Module> modules;

    private final List<String> columnNames = ImmutableList.of("Name", "Version", "Type");

    public ModuleTableModel(List<Module> modules) {
        this.modules = modules;
    }

    @Override
    public int getRowCount() {
        return modules.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames.get(column);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Module module = modules.get(rowIndex);

        switch (columnIndex) {
            case 0:
                return module.getId();

            case 1:
                return module.getVersion();

            case 2:
                if (module instanceof ArchiveModule) {
                    return "jar";
                }
                if (module instanceof PathModule) {
                    return "path";
                }
                return "unknown";

            default:
                throw new UnsupportedOperationException("Invalid column index");

        }
    }
}
