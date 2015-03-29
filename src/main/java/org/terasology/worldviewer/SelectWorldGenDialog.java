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

import java.awt.BorderLayout;
import java.awt.TextField;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.terasology.worldviewer.config.WorldConfig;
import org.terasology.worldviewer.gui.WorldGenCellRenderer;

/**
 * A modal dialogs for the selection of a world generator.
 * @author Martin Steiger
 */
public class SelectWorldGenDialog extends JDialog {

    private static final long serialVersionUID = 257345717408006930L;

    private final JOptionPane optionPane;
    private final JComboBox<Class<?>> wgSelectCombo;
    private final TextField seedText;

    public SelectWorldGenDialog(WorldConfig wgConfig) {
        super(null, "Select World Generator", ModalityType.APPLICATION_MODAL);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(0, 10, 10, 10));
        seedText = new TextField(wgConfig.getWorldSeed());

        wgSelectCombo = new JComboBox<>();

        Set<Class<?>> worldGenSet = WorldGenerators.findOnClasspath();
        Class<?>[] worldGens = worldGenSet.toArray(new Class<?>[0]);
        Arrays.sort(worldGens, Comparator.comparing(clazz -> WorldGenerators.getAnnotatedDisplayName(clazz)));

        for (Class<?> wg : worldGens) {
            wgSelectCombo.addItem(wg);
        }

        int idx = findWorldGenIndex(worldGens, wgConfig.getWorldGenClass());
        wgSelectCombo.setSelectedIndex(idx >= 0 ? idx : 0);
        wgSelectCombo.setRenderer(new WorldGenCellRenderer());

        panel.add(wgSelectCombo, BorderLayout.NORTH);
        panel.add(new JLabel("Seed"), BorderLayout.WEST);
        panel.add(seedText, BorderLayout.CENTER);
        panel.add(new JLabel("<html><b>Note: </b>You can skip this dialog by<br/>supplying the -skip cmd. line argument</html>"), BorderLayout.SOUTH);

        optionPane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        optionPane.addPropertyChangeListener(e -> {
            if (isVisible() && (e.getPropertyName().equals(JOptionPane.VALUE_PROPERTY))) {
                setVisible(false);

                updateConfig(wgConfig);
            }
        });

        setContentPane(optionPane);
        setResizable(false);
    }

    private void updateConfig(WorldConfig wgConfig) {
        int idx = wgSelectCombo.getSelectedIndex();
        if (idx >= 0) {
            Class<?> clazz = wgSelectCombo.getItemAt(idx);
            wgConfig.setWorldGenClass(clazz.getCanonicalName());
        }

        wgConfig.setWorldSeed(seedText.getText());
    }

    private int findWorldGenIndex(Class<?>[] worldGens, String worldGenClass) {
        for (int idx = 0; idx < worldGens.length; idx++) {
            Class<?> wg = worldGens[idx];
            if (wg.getName().equals(worldGenClass)) {
                return idx;
            }
        }

        return -1;
    }

    /**
     * @return JOptionPane.OK_OPTION or JOptionPane.CANCEL_OPTION
     */
    public int getAnswer() {
        if (optionPane.getValue() == null) {
            return JOptionPane.CANCEL_OPTION;
        }

        if (optionPane.getValue().equals(JOptionPane.UNINITIALIZED_VALUE)) {
            return JOptionPane.CANCEL_OPTION;
        }

        return (Integer) optionPane.getValue();
    }

}

