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
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;

import org.terasology.asset.AssetManager;
import org.terasology.engine.SimpleUri;
import org.terasology.engine.TerasologyConstants;
import org.terasology.module.Module;
import org.terasology.module.ModuleEnvironment;
import org.terasology.module.ModuleLoader;
import org.terasology.module.ModuleMetadataReader;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.generator.internal.WorldGeneratorInfo;
import org.terasology.world.generator.internal.WorldGeneratorManager;
import org.terasology.worldviewer.config.WorldConfig;
import org.terasology.worldviewer.env.TinyEnvironment;
import org.terasology.worldviewer.gui.WorldGenCellRenderer;

/**
 * A modal dialogs for the selection of a world generator.
 * @author Martin Steiger
 */
public class SelectWorldGenDialog extends JDialog {

    private static final long serialVersionUID = 257345717408006930L;

    private final JOptionPane optionPane;
    private final JComboBox<WorldGeneratorInfo> wgSelectCombo;
    private final TextField seedText;

    public SelectWorldGenDialog(WorldConfig wgConfig) {
        super(null, "Select World Generator", ModalityType.APPLICATION_MODAL);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(0, 10, 10, 10));
        seedText = new TextField(wgConfig.getWorldSeed());

        wgSelectCombo = new JComboBox<>();
        wgSelectCombo.setRenderer(new WorldGenCellRenderer());

        panel.add(wgSelectCombo, BorderLayout.NORTH);
        panel.add(new JLabel("Seed"), BorderLayout.WEST);
        panel.add(seedText, BorderLayout.CENTER);
        JButton importButton = new JButton("Import");
        panel.add(importButton, BorderLayout.SOUTH);
        importButton.addActionListener(e -> showImportModuleDialog());
//        panel.add(new JLabel("<html><b>Note: </b>You can skip this dialog by<br/>supplying the -skip cmd. line argument</html>"), BorderLayout.SOUTH);

        optionPane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        optionPane.addPropertyChangeListener(e -> {
            if (isVisible() && (e.getPropertyName().equals(JOptionPane.VALUE_PROPERTY))) {
                setVisible(false);

                updateConfig(wgConfig);
            }
        });

        updateWorldGenCombo();

        trySelect(wgConfig.getWorldGen());

        setContentPane(optionPane);
        setResizable(false);
    }

    private void updateWorldGenCombo() {
        List<WorldGeneratorInfo> worldGens = new WorldGeneratorManager().getWorldGenerators();
//        worldGensArrays.sort(worldGens, Comparator.comparing(clazz -> WorldGenerators.getAnnotatedDisplayName(clazz)));

        wgSelectCombo.removeAllItems();
        for (WorldGeneratorInfo wg : worldGens) {
            wgSelectCombo.addItem(wg);
        }
    }

    private void showImportModuleDialog() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.addChoosableFileFilter(new JarFileFilter());
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setDialogTitle("Select module JARs to import");
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            TinyEnvironment.addModules(Arrays.asList(fileChooser.getSelectedFiles()));
        }
        updateWorldGenCombo();
    }

    private void updateConfig(WorldConfig wgConfig) {
        int idx = wgSelectCombo.getSelectedIndex();
        if (idx >= 0) {
            WorldGeneratorInfo info = wgSelectCombo.getItemAt(idx);
            wgConfig.setWorldGenClass(info.getUri());
        }

        wgConfig.setWorldSeed(seedText.getText());
    }

    private void trySelect(SimpleUri worldGen) {
        for (int idx = 0; idx < wgSelectCombo.getItemCount(); idx++) {
            WorldGeneratorInfo wg = wgSelectCombo.getItemAt(idx);
            if (wg.getUri().equals(worldGen)) {
                wgSelectCombo.setSelectedIndex(idx);
            }
        }
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
     * A filter for jar files (case-insensitive).
     */
    private static class JarFileFilter extends FileFilter {
        @Override
        public boolean accept(File file) {
            return file.getName().toLowerCase().endsWith(".jar");
        }

        @Override
        public String getDescription()
        {
            return "(*.jar) Jar File";
        }
    }
}

