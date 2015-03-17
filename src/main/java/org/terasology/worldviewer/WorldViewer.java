/*
 * Copyright 2014 MovingBlocks
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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.world.generator.WorldGenerator;
import org.terasology.worldviewer.config.Config;
import org.terasology.worldviewer.config.WorldConfig;
import org.terasology.worldviewer.env.TinyEnvironment;

import version.GitVersion;

import com.jgoodies.looks.plastic.PlasticLookAndFeel;

/**
 * Preview generated world in Swing
 * @author Martin Steiger
 */
public final class WorldViewer {

    private static final Logger logger = LoggerFactory.getLogger(WorldViewer.class);

    private static final Path CONFIG_PATH = Paths.get(System.getProperty("user.home"), ".worldviewer.json");

    private WorldViewer() {
        // don't create instances
    }

    /**
     * @param args (ignored)
     */
    public static void main(String[] args) {

        logStatus();

        Config config = Config.load(CONFIG_PATH);

//      FullEnvironment.setup();
        TinyEnvironment.setup();

        WorldConfig wgConfig = config.getWorldConfig();

        SelectWorldGenDialog dialog = new SelectWorldGenDialog(wgConfig);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        if (dialog.getAnswer() == JOptionPane.CANCEL_OPTION) {
            dialog.dispose();
            return;
        }

        final WorldGenerator worldGen = dialog.getSelectedWorldGen();

        dialog.dispose();

        if (worldGen != null) {
            worldGen.setWorldSeed(wgConfig.getWorldSeed());

            SwingUtilities.invokeLater(() -> {
                setupLookAndFeel();
                createAndShowGUI(worldGen, config);
            });
        } else {
            String message = "Could not load any world generator class";
            JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new PlasticLookAndFeel());
        } catch (Exception e) {
            // we don't really care about l&f that much, so we just eat the exception
            logger.error("Cannot set look & feel", e);
        }
    }

    private static void logStatus() {
      logger.info("Starting ...");

      logger.debug("Java: {} {} {}", System.getProperty("java.version"), System.getProperty("java.vendor"), System.getProperty("java.home"));
      logger.debug("Java VM: {} {} {}", System.getProperty("java.vm.name"), System.getProperty("java.vm.vendor"), System.getProperty("java.vm.version"));
      logger.debug("OS: {} {} {}", System.getProperty("os.name"), System.getProperty("os.arch"), System.getProperty("os.version"));
      logger.debug("Max. Memory: {} MB", Runtime.getRuntime().maxMemory() / (1024 * 1024));
    }

    private static void createAndShowGUI(WorldGenerator worldGen, Config config) {
        JFrame frame = new MainFrame(worldGen, config);

        frame.setTitle("MapViewer " + GitVersion.getVersion());
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                Config.save(CONFIG_PATH, config);

                // just in case some other thread is still running
                System.exit(0);
            }
        });

//        frame.setAlwaysOnTop(true);

        // align right border at the right border of the default screen
//        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
//        int screenWidth = gd.getDisplayMode().getWidth();
//        frame.setLocation(screenWidth - frame.getWidth(), 40);
    }
}
