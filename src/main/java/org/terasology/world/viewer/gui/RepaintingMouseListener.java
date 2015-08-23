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

package org.terasology.world.viewer.gui;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * Triggers a repaint on any mouse interaction
 */
public class RepaintingMouseListener extends MouseAdapter {

    private final Component comp;

    /**
     * @param comp the component to repaint
     */
    public RepaintingMouseListener(Component comp) {
        this.comp = comp;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        comp.repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        comp.repaint();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        comp.repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        comp.repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        comp.repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        comp.repaint();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        comp.repaint();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        comp.repaint();
    }
}
