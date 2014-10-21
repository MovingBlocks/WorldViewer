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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.terasology.engine.SimpleUri;
import org.terasology.mapviewer.core.Viewer;
import org.terasology.polyworld.IslandWorldGenerator;
import org.terasology.world.generation.facets.SurfaceHeightFacet;
import org.terasology.world.generation.facets.base.FieldFacet2D;

/**
 * The main MapViewer JFrame
 * @author Martin Steiger
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = -8474971565041036025L;

    private final JPanel statusBar = new JPanel();

    private Viewer viewer;

    public MainFrame() {

        IslandWorldGenerator wg = new IslandWorldGenerator(new SimpleUri("polyworld:island"));
//        WorldGenerator wg = new FlatWorldGenerator(new SimpleUri("core:flat"));

        viewer = new Viewer(wg);

//        SwingEnvironment.setup();
//        CitiesViewer viewer = new CitiesViewer("a", camera);

        JPanel config = new JPanel();
        BoxLayout layout = new BoxLayout(config, BoxLayout.LINE_AXIS);
        config.setLayout(layout);
        config.setBorder(new EmptyBorder(2, 5, 2, 5));

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
        facetCombo.setSelectedIndex(facetCombo.getItemCount() - 1);
        config.add(facetCombo);

        config.add(Box.createHorizontalGlue());

        JButton refreshButton = new JButton("Reload");
        refreshButton.setFocusable(false);
        refreshButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                viewer.reload();
            }
        });
        config.add(refreshButton);

        add(config, BorderLayout.NORTH);
        add(viewer, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.LINE_AXIS));
        statusBar.add(new JLabel("Ready"));
        statusBar.add(Box.createHorizontalGlue());
        statusBar.add(new JLabel("Use cursor arrows or drag with right mouse button to navigate"));
        statusBar.setBorder(new EmptyBorder(2, 5, 2, 5));
    }

    @Override
    public void dispose() {
        super.dispose();

        viewer.close();
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
