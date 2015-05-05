/*
 * Copyright 2015 MovingBlocks
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

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.function.Function;

import org.terasology.math.geom.ImmutableVector2i;
import org.terasology.math.geom.Rect2i;

/**
 * Shows the tooltip
 */
public class TooltipOverlay extends AbstractOverlay implements ScreenOverlay {

    private Function<? super ImmutableVector2i, String> tooltipTextFunc;

    public TooltipOverlay(Function<? super ImmutableVector2i, String> tooltipTextFunc) {
        this.tooltipTextFunc = tooltipTextFunc;
    }

    @Override
    public void render(Graphics2D g, Rect2i area, ImmutableVector2i cursor) {
        if (cursor == null) {
            return;
        }

        String text = tooltipTextFunc.apply(cursor);
        int wx = cursor.getX();
        int wy = cursor.getY();
        int offX = 5;
        int offY = 5;

        String[] lines = text.split("\n");

        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();

        int x = wx + offX;
        int y = wy + offY + fm.getAscent();

        int maxHeight = lines.length * fm.getHeight();
        int maxWidth = 0;
        for (String line : lines) {
            int width = fm.stringWidth(line);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }

        int inset = 2;
        g.setColor(new Color(64, 64, 64, 128));
        g.fillRect(wx + offX - inset, wy + offY - inset, maxWidth + 2 * inset, maxHeight + 2 * inset);

        g.setColor(new Color(192, 192, 192, 128));
        g.drawRect(wx + offX - inset, wy + offY - inset, maxWidth + 2 * inset, maxHeight + 2 * inset);

        g.setColor(Color.WHITE);

        for (String line : lines) {
            g.drawString(line, x, y);
            y += fm.getHeight();
        }

        g.dispose();
    }
}
