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

package org.terasology.world.viewer.gui;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import org.terasology.world.generator.internal.WorldGeneratorInfo;

/**
 * It actually implements ListCellRenderer<WorldGeneratorInfo>, but since DefaultListCellRenderer
 * uses Object, this isn't allowed in Java.
 */
public class WorldGenCellRenderer extends DefaultListCellRenderer {

    private static final long serialVersionUID = -3375088206153260363L;

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        String text = ((WorldGeneratorInfo) value).getDisplayName();
        return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
    }
}
