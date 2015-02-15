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

import java.lang.reflect.Constructor;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.SimpleUri;
import org.terasology.world.generator.WorldGenerator;

import version.GitVersion;

import com.google.common.collect.ImmutableList;

/**
 * Preview generated world in Swing
 * @author Martin Steiger
 */
public final class WorldViewer {

    private static final Logger logger = LoggerFactory.getLogger(WorldViewer.class);

    private WorldViewer() {
        // don't create instances
    }

    /**
     * @param args (ignored)
     */
    public static void main(String[] args) {

        logStatus();

        setupLookAndFeel();

        List<String> worldGenClassNames = ImmutableList.of(
                "org.terasology.polyworld.IslandWorldGenerator",
                "org.terasology.core.world.generator.worldGenerators.FlatWorldGenerator",
                "org.terasology.core.world.generator.worldGenerators.PerlinFacetedWorldGenerator");

        final WorldGenerator worldGen = loadFirst(worldGenClassNames, new SimpleUri());

        if (worldGen != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    createAndShowGUI(worldGen);
                }
            });
        } else {
            String message = "Could not load any world generator class";
            JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // we don't really care about l&f that much, so we just eat the exception
            logger.error("Cannot set look & feel", e);
        }
    }

    private static void logStatus() {
        // TODO: logback must be a compile-time dependency for this to work -> either change that or comment it out
//      LoggerContext loggerContext = ((ch.qos.logback.classic.Logger) logger).getLoggerContext();
//      StatusPrinter.print(loggerContext);

      logger.info("Starting ...");

      logger.debug("Java: {} {} {}", System.getProperty("java.version"), System.getProperty("java.vendor"), System.getProperty("java.home"));
      logger.debug("Java VM: {} {} {}", System.getProperty("java.vm.name"), System.getProperty("java.vm.vendor"), System.getProperty("java.vm.version"));
      logger.debug("OS: {} {} {}", System.getProperty("os.name"), System.getProperty("os.arch"), System.getProperty("os.version"));
      logger.debug("Max. Memory: {} MB", Runtime.getRuntime().maxMemory() / (1024 * 1024));
    }

    private static void createAndShowGUI(WorldGenerator worldGen) {
        final JFrame frame = new MainFrame(worldGen);

        frame.setTitle("MapViewer " + GitVersion.getVersion());
        frame.setVisible(true);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

//        frame.setAlwaysOnTop(true);

        // align right border at the right border of the default screen
//        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
//        int screenWidth = gd.getDisplayMode().getWidth();
//        frame.setLocation(screenWidth - frame.getWidth(), 40);
    }

    private static WorldGenerator loadFirst(List<String> classNames, SimpleUri simpleUri) {

        for (String className : classNames) {
            try {
                Class<?> worldGenClazz = Class.forName(className);
                Constructor<?> constructor = worldGenClazz.getConstructor(SimpleUri.class);
                return (WorldGenerator) constructor.newInstance(simpleUri);
            } catch (ClassNotFoundException e) {
                logger.info("Class not found: {}", className);
            } catch (LinkageError e) {
                logger.warn("Class not loadable: {}", className, e);
            } catch (NoSuchMethodException e) {
                logger.warn("Class does not have a constructor with SimpleUri parameter", className);
            } catch (SecurityException e) {
                logger.warn("Security violation while loading class {}", className, e);
            } catch (Exception e) {
                logger.warn("Could not instantiate class {}", className);
            }
        }

        return null;
    }
}
