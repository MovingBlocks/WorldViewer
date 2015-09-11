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

package org.terasology.world.viewer;

import java.awt.Image;
import java.awt.Rectangle;
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
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.context.Context;
import org.terasology.splash.SplashScreen;
import org.terasology.splash.SplashScreenBuilder;
import org.terasology.splash.overlay.AnimatedBoxRowOverlay;
import org.terasology.splash.overlay.RectOverlay;
import org.terasology.splash.overlay.TextOverlay;
import org.terasology.world.viewer.config.Config;
import org.terasology.world.viewer.env.TinyEnvironment;
import org.terasology.world.viewer.version.VersionInfo;

/**
 * Preview generated world in Swing
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

        try {

            SplashScreen splashScreen = createSplashScreen();
            splashScreen.post("Loading ...");

//          FullEnvironment.setup();
            Context context = TinyEnvironment.createContext(splashScreen);

            Config config = Config.load(CONFIG_PATH);

            splashScreen.close();
            SwingUtilities.invokeLater(() -> {
                setupLookAndFeel();
                createAndShowMainFrame(context, config);
            });
        } catch (IOException e) {
            System.err.println("Could not load modules: " + e.getMessage());
            return;
        }
    }

    private static SplashScreen createSplashScreen() {
        SplashScreenBuilder builder = new SplashScreenBuilder();
        int imageHeight = 332;
        int maxTextWidth = 450;
        int width = 600;
        int height = 30;
        int left = 20;
        int top = imageHeight - height - 20;

        Rectangle rectRc = new Rectangle(left, top, width, height);
        Rectangle textRc = new Rectangle(left + 10, top + 5, maxTextWidth, height);
        Rectangle boxRc = new Rectangle(left + maxTextWidth + 10, top, width - maxTextWidth - 20, height);
        return builder
                .add(new RectOverlay(rectRc))
                .add(new TextOverlay(textRc))
                .add(new AnimatedBoxRowOverlay(boxRc))
                .build();
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

    private static void createAndShowMainFrame(Context context, Config config) {
        JFrame frame = new MainFrame(context, config);
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
