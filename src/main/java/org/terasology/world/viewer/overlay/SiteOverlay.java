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

package org.terasology.world.viewer.overlay;

import java.awt.Color;
import java.awt.Graphics2D;
import java.math.RoundingMode;
import java.util.function.Function;

import org.terasology.cities.sites.Site;
import org.terasology.cities.sites.SiteFacet;
import org.terasology.math.geom.BaseVector2i;
import org.terasology.math.geom.ImmutableVector2i;
import org.terasology.math.geom.Rect2i;
import org.terasology.world.generation.Region;

import com.google.common.math.IntMath;

public class SiteOverlay extends AbstractOverlay implements WorldOverlay {

    private Function<ImmutableVector2i, Region> regions;

    public SiteOverlay(Function<ImmutableVector2i, Region> regionFunc) {
        regions = regionFunc;
    }

    @Override
    public void render(Graphics2D g, Rect2i area, ImmutableVector2i cursor) {
        if (cursor == null) {
            return;
        }

        g.setColor(Color.GRAY);
        g.drawRect(128 * IntMath.divide(cursor.getX(), 128, RoundingMode.FLOOR), 128 * IntMath.divide(cursor.getY(), 128, RoundingMode.FLOOR), 128, 128);

        Color frameColor = new Color(128, 32, 32, 224);

        Region r = regions.apply(cursor);
        SiteFacet facet = r.getFacet(SiteFacet.class);

        g.setColor(Color.BLACK);
        Rect2i wr = facet.getWorldRegion();
        g.drawRect(wr.minX(), wr.minY(), wr.width(), wr.height());

        Rect2i sr = facet.getCertainWorldRegion();
        g.drawRect(sr.minX(), sr.minY(), sr.width(), sr.height());

        for (Site site : facet.getSettlements()) {
            BaseVector2i pos = site.getPos();
            int rad = (int) site.getRadius();
//            if (pos.distance(cursor) < rad) {
                g.setColor(frameColor);
//                g.drawOval(pos.getX() - rad, pos.getY() - rad, rad * 2, rad * 2);
                rad += 128;
                g.drawOval(pos.getX() - rad, pos.getY() - rad, rad * 2, rad * 2);
//            }


        }
    }
}
