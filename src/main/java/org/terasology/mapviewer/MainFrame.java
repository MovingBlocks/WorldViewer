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

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JLabel;

import org.terasology.core.world.generator.worldGenerators.FlatWorldGenerator;
import org.terasology.engine.SimpleUri;
import org.terasology.mapviewer.camera.Camera;
import org.terasology.mapviewer.camera.CameraKeyController;
import org.terasology.mapviewer.camera.CameraListener;
import org.terasology.mapviewer.polyworld.WorldViewer;
import org.terasology.polyworld.IslandWorldGenerator;
import org.terasology.polyworld.elevation.ElevationProvider;
import org.terasology.world.generation.World;
import org.terasology.world.generation.WorldBuilder;
import org.terasology.world.generator.WorldGenerator;

/**
 * The main MapViewer JFrame
 * @author Martin Steiger
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = -8474971565041036025L;

    private final JLabel status = new JLabel();

    private final Camera camera = new Camera();

    public MainFrame() throws IOException {

        IslandWorldGenerator wg = new IslandWorldGenerator(new SimpleUri("polyworld:island"));
//        WorldGenerator wg = new FlatWorldGenerator(new SimpleUri("core:flat"));
        wg.setWorldSeed("sdfsfdf"); // 9782985378925l
        World world = wg.getWorld();

        WorldViewer viewer = new WorldViewer(world, camera);

//        SwingEnvironment.setup();
//        CitiesViewer viewer = new CitiesViewer("a", camera);

        add(viewer, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        KeyAdapter keyCameraController = new CameraKeyController(camera);
        camera.addListener(new CameraListener() {

            @Override
            public void onPosChange() {
                updateLabel();
            }

            @Override
            public void onZoomChange() {
                updateLabel();
            }
        });

        addKeyListener(keyCameraController);

        updateLabel();
    }

    protected void updateLabel() {
        status.setText("LEFT, RIGHT, UP, DOWN - Camera: " + camera.getPos().toString());
    }


}
