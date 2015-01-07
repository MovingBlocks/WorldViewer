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

package org.terasology.worldviewer.overlay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Collection;
import java.util.function.Function;

import org.terasology.math.Rect2i;
import org.terasology.worldviewer.render.RandomObjectColors;

/**
 * Renders a collection of colored rectangles
 * @author Martin Steiger
 */
public class BoundsOverlay implements Overlay {

    private Function<Rect2i, Collection<Rect2i>> func;
    private Function<Object, Color> colorFunc;

    public BoundsOverlay(Function<Rect2i, Collection<Rect2i>> func) {
        this.func = func;
        colorFunc = new RandomObjectColors();
    }


    @Override
    public void render(Graphics2D g, Rect2i area) {

        int sw = 2;
        g.setStroke(new BasicStroke(sw));
        for (Rect2i rc : func.apply(area)) {
            Color color = colorFunc.apply(rc);
            g.setColor(color);

            g.drawRect(rc.minX(), rc.minY(), rc.width() - sw, rc.height() - sw);
        }
    }
}
