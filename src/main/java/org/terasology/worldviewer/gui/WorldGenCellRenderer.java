package org.terasology.worldviewer.gui;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import org.terasology.world.generator.WorldGenerator;

/**
 * It actually implements ListCellRenderer<WorldGenerator>, but since DefaultListCellRenderer
 * uses Object, this isn't allowed in Java.
 * @author Martin Steiger
 */
public class WorldGenCellRenderer extends DefaultListCellRenderer
{
    private static final long serialVersionUID = -3375088206153260363L;

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
    {
        String text = ((WorldGenerator)value).getClass().getSimpleName();
        return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
    }
}