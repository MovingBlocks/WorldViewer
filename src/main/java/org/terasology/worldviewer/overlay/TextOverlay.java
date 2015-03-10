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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.util.function.Supplier;

import org.terasology.math.Rect2i;
import org.terasology.math.geom.Vector2i;
import org.terasology.rendering.nui.HorizontalAlign;
import org.terasology.rendering.nui.VerticalAlign;

/**
 * Renders a grid that is aligned along tile borders
 * @author Martin Steiger
 */
public class TextOverlay extends AbstractOverlay {

    private final Supplier<String> textSupp;

    private Color color = Color.WHITE;

    private int inLeft;
    private int inTop;
    private int inRight;
    private int inBottom;

    private int mgLeft;
    private int mgTop;
    private int mgRight;
    private int mgBottom;

    private Font font;

    private VerticalAlign alignVert = VerticalAlign.TOP;
    private HorizontalAlign alignHorz = HorizontalAlign.LEFT;

    private Paint background;
    private Paint frame;

    /**
     * @param textSupp the text supplier
     */
    public TextOverlay(Supplier<String> textSupp) {
        this.textSupp = textSupp;
    }

    public VerticalAlign getVerticalAlign() {
        return alignVert;
    }

    public TextOverlay setVerticalAlign(VerticalAlign alignV) {
        this.alignVert = alignV;
        return this;
    }

    public HorizontalAlign getHorizontalAlign() {
        return alignHorz;
    }

    public TextOverlay setHorizontalAlign(HorizontalAlign alignH) {
        this.alignHorz = alignH;
        return this;
    }

    public TextOverlay setMargins(int left, int top, int right, int bottom) {
        this.mgLeft = left;
        this.mgTop = top;
        this.mgRight = right;
        this.mgBottom = bottom;
        return this;
    }

    public TextOverlay setInsets(int left, int top, int right, int bottom) {
        this.inLeft = left;
        this.inTop = top;
        this.inRight = right;
        this.inBottom = bottom;
        return this;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    @Override
    public void render(Graphics2D g, Rect2i area) {

        Rect2i mgArea = Rect2i.createFromMinAndMax(
                area.minX() + mgLeft,
                area.minY() + mgTop,
                area.maxX() - mgRight,
                area.maxY() - mgBottom);

        String text = textSupp.get();
        if (text == null) {
            return;
        }

        Font oldFont = g.getFont();
        g.setFont(font);        // null fonts are silenty ignored

        FontMetrics fm = g.getFontMetrics();
        String[] lines = text.split("\n");

        if (background != null || frame != null) {
            Vector2i bbox = getBBox(fm, lines);
            bbox.addX(inLeft + inRight);
            bbox.addY(inTop + inBottom);
            int x = mgArea.minX() + alignHorz.getOffset(bbox.getX(), mgArea.width());
            int y = mgArea.minY() + alignVert.getOffset(bbox.getY(), mgArea.height());

            if (background != null) {
                g.setPaint(background);
                g.fillRect(x, y, bbox.getX(), bbox.getY());
            }

            if (frame != null) {
                g.setPaint(frame);
                g.drawRect(x, y, bbox.getX(), bbox.getY());
            }
        }

        int y = 0;

        Rect2i textArea = Rect2i.createFromMinAndMax(
                mgArea.minX() + inLeft,
                mgArea.minY() + inTop,
                mgArea.maxX() - inRight,
                mgArea.maxY() - inBottom);

        switch (alignVert) {
        case TOP:
            y = textArea.minY() + fm.getAscent();
            break;

        case MIDDLE:
            double lineCenter = fm.getHeight() / 2 - fm.getAscent();
            double textCenter = (lines.length - 1) * 0.5 * fm.getHeight() + lineCenter;
            double centerY = (textArea.maxY() + textArea.minY()) * 0.5;
            y = (int) (centerY - textCenter);
            break;

        case BOTTOM:
            int textHeight = (lines.length - 1) * fm.getHeight();
            y = textArea.maxY() - textHeight;
            break;
        }

        g.setColor(color);

        for (String line : lines) {
            int x = 0;
            int textWidth = fm.stringWidth(line);

            switch (alignHorz) {
            case LEFT:
                x = textArea.minX();
                break;

            case CENTER:
                double centerX = (textArea.maxX() + textArea.minX()) * 0.5;
                x = (int) (centerX - textWidth);
                break;

            case RIGHT:
                x = textArea.maxX() - textWidth;
                break;
            }

            g.drawString(line, x, y);

            y += fm.getHeight();
        }

        g.setFont(oldFont);
    }

    public void setBackground(Paint background) {
        this.background = background;
    }

    public void setFrame(Paint frame) {
        this.frame = frame;
    }

    private Vector2i getBBox(FontMetrics fm, String[] lines) {

        int maxWidth = 0;
        int height = 0;

        for (String line : lines) {
            int textWidth = fm.stringWidth(line);
            if (textWidth > maxWidth) {
                maxWidth = textWidth;
            }
            height += fm.getHeight();
        }

        return new Vector2i(maxWidth, height);
    }
}


