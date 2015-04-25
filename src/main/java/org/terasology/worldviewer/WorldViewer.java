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

import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.SimpleUri;
import org.terasology.engine.splash.SplashScreen;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.generator.UnresolvedWorldGeneratorException;
import org.terasology.world.generator.WorldGenerator;
import org.terasology.world.generator.internal.WorldGeneratorManager;
import org.terasology.worldviewer.config.Config;
import org.terasology.worldviewer.config.WorldConfig;
import org.terasology.worldviewer.env.TinyEnvironment;
import org.terasology.worldviewer.version.VersionInfo;

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

        CmdLineConfigs cmdLineOpts = new CmdLineConfigs();
        CmdLineParser parser = new CmdLineParser(cmdLineOpts);

        SplashScreen.getInstance().post("Loading ...");

        try {
//            FullEnvironment.setup();
            TinyEnvironment.setup();

            Config config = Config.load(CONFIG_PATH);

            parser.parseArgument(args);

            if (cmdLineOpts.help) {
                System.out.println("WorldViewer - Version " + VersionInfo.getVersion());
                parser.printUsage(System.out);
                return;
            }

            SplashScreen.getInstance().close();
            SwingUtilities.invokeLater(() -> {
                setupLookAndFeel();
                createAndShowGUI(config, cmdLineOpts);
            });
        } catch (CmdLineException e) {
            System.err.println("Could not parse command line arguments: " + e.getMessage());
            parser.printUsage(System.out);
            return;
        } catch (IOException e) {
            System.err.println("Could not load modules: " + e.getMessage());
            return;
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

      DateTimeFormatter dateFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
      logger.debug("Java: {} {} {}", System.getProperty("java.version"), System.getProperty("java.vendor"), System.getProperty("java.home"));
      logger.debug("Java VM: {} {} {}", System.getProperty("java.vm.name"), System.getProperty("java.vm.vendor"), System.getProperty("java.vm.version"));
      logger.debug("OS: {} {} {}", System.getProperty("os.name"), System.getProperty("os.arch"), System.getProperty("os.version"));
      logger.debug("Max. Memory: {} MB", Runtime.getRuntime().maxMemory() / (1024 * 1024));
      logger.debug("Version: {}", VersionInfo.getVersion());
      logger.debug("Built: {}", VersionInfo.getBuildTime().format(dateFormat));
      logger.debug("Commit: {}", VersionInfo.getBuildCommit());

      String classpath = System.getProperty("java.class.path");
      String[] cpEntries = classpath.split(File.pathSeparator);
      for (String cpEntry : cpEntries) {
          logger.debug("Classpath: " + cpEntry);
      }
    }

    private static void createAndShowGUI(Config config, CmdLineConfigs cmdLineOpts) {

        WorldConfig wgConfig = config.getWorldConfig();

        if (!cmdLineOpts.skipSelect && cmdLineOpts.worldGen == null && cmdLineOpts.seed == null) {
            SelectWorldGenDialog dialog = new SelectWorldGenDialog(wgConfig);
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
            dialog.dispose();
            if (dialog.getAnswer() != JOptionPane.OK_OPTION) {
                return;
            }
        }

        SimpleUri worldGenUri = wgConfig.getWorldGen();
        String worldSeed = wgConfig.getWorldSeed();

        if (cmdLineOpts.worldGen != null) {
            worldGenUri = new SimpleUri(cmdLineOpts.worldGen);
        }

        if (cmdLineOpts.seed != null) {
            worldSeed = cmdLineOpts.seed;
        }

        try {
            WorldGeneratorManager worldGeneratorManager = CoreRegistry.get(WorldGeneratorManager.class);
            WorldGenerator worldGen = worldGeneratorManager.createGenerator(worldGenUri);
            worldGen.setWorldSeed(worldSeed);
            worldGen.initialize();
            createAndShowMainFrame(worldGen, config);
        } catch (UnresolvedWorldGeneratorException ex) {
            String message = "<html>Could not create world generator<br>" + ex + "</html>";
            JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void createAndShowMainFrame(WorldGenerator worldGen, Config config) {
        JFrame frame = new MainFrame(worldGen, config);
        frame.setIconImages(loadIcons());
        frame.setTitle("WorldViewer " + VersionInfo.getVersion());
        frame.setSize(1280, 720);
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

    private static List<Image> loadIcons() {
        List<Image> icons = new ArrayList<Image>();
        int[] sizes = {16, 32, 64};
        for (int size : sizes) {
            String name = String.format("/icons/gooey_sweet_red_%d.png", size);
            URL resUrl = WorldViewer.class.getResource(name);
            try {
                BufferedImage iconImage = ImageIO.read(resUrl);
                icons.add(iconImage);
            } catch (IOException e) {
                logger.warn("Could not load icon: {}", name);
            }
        }
        return icons;
    }
}
