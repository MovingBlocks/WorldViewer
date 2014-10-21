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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.terasology.engine.SimpleUri;
import org.terasology.mapviewer.camera.Camera;
import org.terasology.mapviewer.camera.CameraKeyController;
import org.terasology.mapviewer.camera.CameraListener;
import org.terasology.mapviewer.camera.CameraMouseController;
import org.terasology.mapviewer.polyworld.WorldViewer;
import org.terasology.polyworld.IslandWorldGenerator;
import org.terasology.world.generation.World;
import org.terasology.world.generation.facets.SurfaceHeightFacet;
import org.terasology.world.generation.facets.base.FieldFacet2D;

/**
 * The main MapViewer JFrame
 * @author Martin Steiger
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = -8474971565041036025L;

    private final JLabel status = new JLabel();

    private final Camera camera = new Camera();

    private WorldViewer viewer;

    public MainFrame() {

        IslandWorldGenerator wg = new IslandWorldGenerator(new SimpleUri("polyworld:island"));
//        WorldGenerator wg = new FlatWorldGenerator(new SimpleUri("core:flat"));
        wg.setWorldSeed("sdfsfdf"); // 9782985378925l
        World world = wg.getWorld();

        viewer = new WorldViewer(world, camera);

//        SwingEnvironment.setup();
//        CitiesViewer viewer = new CitiesViewer("a", camera);

        JPanel config = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        final JComboBox<FacetEntry<FieldFacet2D>> facetCombo = new JComboBox<FacetEntry<FieldFacet2D>>();
        facetCombo.addItem(new EmptyFacetEntry<FieldFacet2D>());
        facetCombo.addItem(new FacetEntry<FieldFacet2D>(SurfaceHeightFacet.class));
        facetCombo.setFocusable(false);
        facetCombo.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int index = facetCombo.getSelectedIndex();
                FacetEntry<FieldFacet2D> itemAt = facetCombo.getItemAt(index);
                viewer.setFacet(itemAt.getFacet());
            }
        });
        config.add(facetCombo);

        add(config, BorderLayout.NORTH);
        add(viewer, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        KeyAdapter keyCameraController = new CameraKeyController(camera);
        MouseAdapter mouseCameraController = new CameraMouseController(camera);
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
        addMouseListener(mouseCameraController);
        addMouseMotionListener(mouseCameraController);

        updateLabel();
    }

    @Override
    public void dispose() {
        super.dispose();

        viewer.close();
    }

    protected void updateLabel() {
        status.setText("LEFT, RIGHT, UP, DOWN - Camera: " + camera.getPos().toString());
    }


    private static class FacetEntry<T> {

        private final Class<? extends T> clazz;

        public FacetEntry(Class<? extends T> clazz) {
            this.clazz = clazz;
        }

        @Override
        public String toString() {
            return clazz.getSimpleName();
        }

        public Class<? extends T> getFacet() {
            return clazz;
        }
    }

    private static class EmptyFacetEntry<T> extends FacetEntry<T> {

        public EmptyFacetEntry() {
            super(null);
        }

        @Override
        public String toString() {
            return "Empty";
        }
    }

}
