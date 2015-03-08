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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.terasology.core.world.generator.facets.TreeFacet;
import org.terasology.core.world.generator.trees.TreeGenerator;
import org.terasology.math.Region3i;
import org.terasology.math.Vector3i;
import org.terasology.world.generation.Region;
import org.terasology.world.generation.WorldFacet;

import com.google.common.collect.Lists;

/**
 * Renders the tree coverage based on {@link TreeFacet}
 * and provides aggregating tool tips.
 * @author Martin Steiger
 */
public class TreeFacetLayer extends AbstractFacetLayer {

    private Function<TreeGenerator, Integer> radiusFunc = ignore -> 3;
    private Function<TreeGenerator, Color> colorFunc = ignore -> Color.GREEN;
    private Function<TreeGenerator, String> labelFunc = ignore -> "Tree";

    @Override
    public Class<? extends WorldFacet> getFacetClass() {
        return TreeFacet.class;
    }

    @Override
    public void render(BufferedImage img, Region region) {
        TreeFacet treeFacet = region.getFacet(TreeFacet.class);

        Graphics2D g = img.createGraphics();

        for (Entry<Vector3i, TreeGenerator> entry : treeFacet.getRelativeEntries().entrySet()) {
            TreeGenerator treeGen = entry.getValue();
            int wx = entry.getKey().getX();
            int wz = entry.getKey().getZ();
            int r = radiusFunc.apply(treeGen);
            Color color = colorFunc.apply(treeGen);

            g.setColor(color);
            g.fillOval(wx - r, wz - r, r * 2 + 1, r * 2 + 1);
            g.setColor(color.darker());
            g.drawOval(wx - r, wz - r, r * 2 + 1, r * 2 + 1);
        }

        g.dispose();
    }

    @Override
    public String getWorldText(Region region, int wx, int wy) {
        TreeFacet treeFacet = region.getFacet(TreeFacet.class);

        Region3i worldRegion = treeFacet.getWorldRegion();
        Region3i relativeRegion = treeFacet.getRelativeRegion();

        int rx = wx - worldRegion.minX() + relativeRegion.minX();
        int rz = wy - worldRegion.minZ() + relativeRegion.minZ();

        List<String> labels = Lists.newArrayList();
        for (Entry<Vector3i, TreeGenerator> entry : treeFacet.getRelativeEntries().entrySet()) {
            TreeGenerator treeGen = entry.getValue();
            Vector3i treePos = entry.getKey();

            int dx = treePos.getX() - rx;
            int dz = treePos.getZ() - rz;
            int rad = radiusFunc.apply(treeGen);

            if (dx * dx + dz * dz < rad * rad) {
                labels.add(labelFunc.apply(treeGen));
            }
        }

        // try to exit early first

        if (labels.isEmpty()) {
            return null;
        }

        if (labels.size() == 1) {
            return labels.get(0);
        }

        // TODO: treat 1x occurrences like above (e.g. Tree instead of 1x Tree)

        // convert to a stream, collect identical String elements and collect the count in a map
        Map<String, Long> counters = labels.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // define a mapping from a map entry to a String representation
        Function<Entry<String, Long>, String> toStringFunc = e -> String.format("%dx %s", e.getValue(), e.getKey());

        // apply that mapping and join the Strings with a comma
        return counters.entrySet().stream().map(toStringFunc).collect(Collectors.joining(", "));
    }

}
