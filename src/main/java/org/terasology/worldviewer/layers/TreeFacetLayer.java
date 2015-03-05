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

package org.terasology.worldviewer.layers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map.Entry;

import org.terasology.core.world.generator.facets.TreeFacet;
import org.terasology.core.world.generator.trees.TreeGenerator;
import org.terasology.math.Vector3i;
import org.terasology.world.generation.Region;
import org.terasology.world.generation.WorldFacet;

/**
 * TODO Type description
 * @author Martin Steiger
 */
public class TreeFacetLayer extends AbstractFacetLayer {

    @Override
    public Class<? extends WorldFacet> getFacetClass() {
        return TreeFacet.class;
    }

    @Override
    public void render(BufferedImage img, Region region) {
        TreeFacet treeFacet = region.getFacet(TreeFacet.class);

        Graphics2D g = img.createGraphics();

        for (Entry<Vector3i, TreeGenerator> entry : treeFacet.getWorldEntries().entrySet()) {
            int wx = entry.getKey().getX();
            int wz = entry.getKey().getZ();
            int r = 3;

            g.setColor(Color.GREEN.brighter());
            g.fillOval(wx - r, wz - r, r * 2, r * 2);
            g.setColor(Color.GREEN);
            g.drawOval(wx - r, wz - r, r * 2, r * 2);
        }

        g.dispose();
    }

    @Override
    public String getWorldText(Region r, int wx, int wy) {
        return "";
    }

}
