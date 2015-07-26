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

package org.terasology.worldviewer.gui;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Stores the last known cursor position
 */
public class CursorPositionListener extends MouseAdapter {

    private Point curPos;

    /**
     * @return the cursor position or <code>null</code> if outside
     */
    public Point getCursorPosition() {
        return curPos;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        curPos = e.getPoint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        curPos = e.getPoint();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        curPos = null;
    }
}
