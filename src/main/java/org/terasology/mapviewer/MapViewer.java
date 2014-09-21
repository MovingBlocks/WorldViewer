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

package org.terasology.mapviewer;

import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

/**
 * Preview generated world in Swing
 * @author Martin Steiger
 */
final class MapViewer {

    private MapViewer() {

    }

    public static void main(String[] args) throws IOException {

        SwingEnvironment.setup();

        final JFrame frame = new MainFrame();

        frame.setTitle("MapViewer");
        frame.setVisible(true);
        frame.setSize(2 * 512 + 40, 512 + 60);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        frame.setAlwaysOnTop(true);

        // align right border at the right border of the default screen
//        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
//        int screenWidth = gd.getDisplayMode().getWidth();
//        frame.setLocation(screenWidth - frame.getWidth(), 40);

        // repaint every second to see changes while debugging
//        int updateInteval = 1000;
//        Timer timer = new Timer();
//        timer.schedule(new TimerTask() {
//
//            @Override
//            public void run() {
//                frame.repaint();
//            }
//
//        }, updateInteval, updateInteval);

    }
}
