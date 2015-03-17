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
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import org.terasology.world.generator.WorldGenerator;
import org.terasology.worldviewer.config.WorldConfig;
import org.terasology.worldviewer.gui.WorldGenCellRenderer;

/**
 * A modal dialogs for the selection of a world generator.
 * @author Martin Steiger
 */
public class SelectWorldGenDialog extends JDialog {

    private static final long serialVersionUID = 257345717408006930L;

    private final JOptionPane optionPane;
    private final JComboBox<WorldGenerator> wgSelectCombo;
    private final TextField seedText;

    public SelectWorldGenDialog(WorldConfig wgConfig) {
        super(null, "Select World Generator", ModalityType.APPLICATION_MODAL);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(0, 10, 10, 10));
        seedText = new TextField(wgConfig.getWorldSeed());

        wgSelectCombo = new JComboBox<>();
        ListCellRenderer<? super WorldGenerator> wgTextRenderer = new WorldGenCellRenderer();
        wgSelectCombo.setRenderer(wgTextRenderer);

        SwingWorker<List<WorldGenerator>, Void> swingWorker = new SwingWorker<List<WorldGenerator>, Void>() {

            @Override
            protected List<WorldGenerator> doInBackground() {
                return WorldGenerators.findOnClasspath();
            }

            @Override
            protected void done() {
                int idx;
                try {
                    List<WorldGenerator> worldGens = get();
                    for (WorldGenerator wg : worldGens) {
                        wgSelectCombo.addItem(wg);
                    }
                    idx = findWorldGenIndex(worldGens, wgConfig.getWorldGenClass());
                    wgSelectCombo.setSelectedIndex(idx >= 0 ? idx : 0);
                } catch (InterruptedException | ExecutionException e) {
                    // not going to happen
                }
            }
        };
        swingWorker.execute();

        panel.add(wgSelectCombo, BorderLayout.NORTH);
        panel.add(new JLabel("Seed"), BorderLayout.WEST);
        panel.add(seedText, BorderLayout.CENTER);

        optionPane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);

        optionPane.addPropertyChangeListener(e -> {
            if (isVisible() && (e.getPropertyName().equals(JOptionPane.VALUE_PROPERTY))) {
                setVisible(false);

                updateConfig(wgConfig);
            }
        });

        setContentPane(optionPane);
    }

    private void updateConfig(WorldConfig wgConfig) {
        wgConfig.setWorldGenClass(getSelectedWorldGen().getClass().getCanonicalName());
        wgConfig.setWorldSeed(seedText.getText());
    }

    private int findWorldGenIndex(List<WorldGenerator> worldGens, String worldGenClass) {
        for (int idx = 0; idx < worldGens.size(); idx++) {
            WorldGenerator wg = worldGens.get(idx);
            if (wg.getClass().getName().equals(worldGenClass)) {
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

    /**
     * @return the selected world generator or <code>null</code>.
     */
    public WorldGenerator getSelectedWorldGen() {
        int idx = wgSelectCombo.getSelectedIndex();
        if (idx == -1) {
            return null;
        }
        return wgSelectCombo.getItemAt(idx);
    }

    public String getWorldSeed() {
        return seedText.getText();
    }
}

