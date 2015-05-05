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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.TextField;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableModel;

import org.terasology.engine.SimpleUri;
import org.terasology.engine.module.ModuleManager;
import org.terasology.module.Module;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.generator.internal.WorldGeneratorInfo;
import org.terasology.world.generator.internal.WorldGeneratorManager;
import org.terasology.worldviewer.config.WorldConfig;
import org.terasology.worldviewer.env.TinyEnvironment;
import org.terasology.worldviewer.gui.WorldGenCellRenderer;

import com.google.common.collect.Lists;

/**
 * A modal dialogs for the selection of a world generator.
 * @author Martin Steiger
 */
public class SelectWorldGenDialog extends JDialog {

    private static final long serialVersionUID = 257345717408006930L;

    private final JOptionPane optionPane;
    private final JComboBox<WorldGeneratorInfo> wgSelectCombo;
    private final TextField seedText;

    private JTable moduleList;

    private JFileChooser jarFileChooser;

    private JFileChooser folderChooser;

    public SelectWorldGenDialog(WorldConfig wgConfig) {
        super(null, "Select World Generator", ModalityType.APPLICATION_MODAL);

        // since file choosers are fields, the LRU folder is kept
        jarFileChooser = new JFileChooser();
        JarFileFilter filter = new JarFileFilter();
        jarFileChooser.addChoosableFileFilter(filter);
        jarFileChooser.setFileFilter(filter);
        jarFileChooser.setMultiSelectionEnabled(true);
        jarFileChooser.setDialogTitle("Select module JARs to import");

        folderChooser = new JFileChooser();
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        folderChooser.setMultiSelectionEnabled(true);
        folderChooser.setDialogTitle("Select module folders to import");

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        panel.setBorder(new EmptyBorder(0, 10, 10, 10));

        gbc.gridy = 0;
        panel.add(new JLabel("World Generator"), gbc.clone());
        wgSelectCombo = new JComboBox<>();
        wgSelectCombo.setRenderer(new WorldGenCellRenderer());
        panel.add(wgSelectCombo, gbc.clone());

        gbc.gridy = 1;
        panel.add(new JLabel("Seed"), gbc.clone());
        seedText = new TextField(wgConfig.getWorldSeed());
        panel.add(seedText, gbc.clone());

        gbc.gridwidth = 2;

        gbc.gridy = 2;
        panel.add(new JLabel("Loaded modules:"), gbc.clone());

        gbc.gridy = 3;
        moduleList = new JTable() {

            private static final long serialVersionUID = 3315774652323052959L;

            @Override
            public boolean getScrollableTracksViewportHeight() {
                return getPreferredSize().height < getParent().getHeight();
            }
        };
        moduleList.setBorder(BorderFactory.createEtchedBorder());
        moduleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        moduleList.getTableHeader().setReorderingAllowed(false);
        moduleList.setBorder(BorderFactory.createEmptyBorder());
        JScrollPane tableScrollPane = new JScrollPane(moduleList);
        tableScrollPane.setPreferredSize(new Dimension(250, 150));
        panel.add(tableScrollPane, gbc.clone());

        gbc.gridy = 4;
        JButton importJarButton = new JButton("Import Module JAR");
        importJarButton.addActionListener(e -> showImportModuleJarDialog());
        panel.add(importJarButton, gbc.clone());

        gbc.gridy = 5;
        JButton importFolderButton = new JButton("Import Module Folder");
        importFolderButton.addActionListener(e -> showImportModuleFolderDialog());
        panel.add(importFolderButton, gbc.clone());


        gbc.gridy = 6;
        String infoText = "<html><b>Note: </b>You can skip this dialog by<br/>supplying the -skip cmd. line argument</html>";
        panel.add(new JLabel(infoText), gbc.clone());

        optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        optionPane.addPropertyChangeListener(e -> {
            if (isVisible() && (e.getPropertyName().equals(JOptionPane.VALUE_PROPERTY))) {
                setVisible(false);

                updateConfig(wgConfig);
            }
        });

        updateWorldGenCombo();
        updateModuleList();

        trySelect(wgConfig.getWorldGen());

        setContentPane(optionPane);
        setResizable(false);
    }

    private void updateModuleList() {
        Iterable<Module> modules = CoreRegistry.get(ModuleManager.class).getEnvironment();
        TableModel dataModel = new ModuleTableModel(Lists.newArrayList(modules));
        moduleList.setModel(dataModel);
        moduleList.getColumnModel().getColumn(1).setPreferredWidth(10);
    }

    private void updateWorldGenCombo() {
        List<WorldGeneratorInfo> worldGens = CoreRegistry.get(WorldGeneratorManager.class).getWorldGenerators();

        wgSelectCombo.removeAllItems();
        for (WorldGeneratorInfo wg : worldGens) {
            wgSelectCombo.addItem(wg);
        }
    }

    private void showImportModuleJarDialog() {
        if (jarFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            TinyEnvironment.addModules(Arrays.asList(jarFileChooser.getSelectedFiles()));
        }
        updateWorldGenCombo();
        updateModuleList();
    }

    private void showImportModuleFolderDialog() {
        if (folderChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            TinyEnvironment.addModules(Arrays.asList(folderChooser.getSelectedFiles()));
        }
        updateWorldGenCombo();
        updateModuleList();
    }

    private void updateConfig(WorldConfig wgConfig) {
        int idx = wgSelectCombo.getSelectedIndex();
        if (idx >= 0) {
            WorldGeneratorInfo info = wgSelectCombo.getItemAt(idx);
            wgConfig.setWorldGen(info.getUri());
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
            // show directories, too
            return file.isDirectory() || file.getName().toLowerCase().endsWith(".jar");
        }

        @Override
        public String getDescription() {
            return "(*.jar) Jar File";
        }
    }
}

