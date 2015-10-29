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
import java.awt.geom.Ellipse2D;

import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.geom.BaseVector2i;
import org.terasology.math.geom.ImmutableVector2i;
import org.terasology.math.geom.Rect2i;
import org.terasology.math.geom.SpiralIterable;
import org.terasology.math.geom.Vector2i;
import org.terasology.math.geom.Vector3f;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.generator.WorldGenerator;

/**
 * Shows the closest spawning point w.r.t. the mouse cursor.
 */
public class SpawningPointOverlay extends AbstractOverlay implements WorldOverlay {

    private final WorldGenerator worldGen;
    private EntityRef player;

    public SpawningPointOverlay(WorldGenerator worldGen) {
        this.worldGen = worldGen;
        player = CoreRegistry.get(EntityManager.class).create();
        player.addComponent(new LocationComponent(new Vector3f(0, 0, 0)));
    }

    @Override
    public void render(Graphics2D g, Rect2i area, ImmutableVector2i cursor) {
        if (cursor == null) {
            return;
        }

        LocationComponent location = player.getComponent(LocationComponent.class);
        location.setWorldPosition(new Vector3f(cursor.getX(), location.getWorldPosition().getY(), cursor.getY()));
        player.saveComponent(location);

        Vector3f pos = worldGen.getSpawnPosition(player);

        int searchRadius = 16;

        // try and find somewhere in this region a spot to land
        Rect2i spawnArea = Rect2i.createFromMinAndSize(cursor.getX() - searchRadius, cursor.getY() - searchRadius, searchRadius * 2, searchRadius * 2);

        g.setColor(new Color(0x964B00));
        g.drawRect(spawnArea.minX(), spawnArea.minY(), spawnArea.width(), spawnArea.height());

        int spiralRad = searchRadius / 2 - 1;
        SpiralIterable spiral = SpiralIterable.clockwise(new Vector2i(cursor)).maxRadius(spiralRad).scale(2).build();
        g.setColor(Color.RED);

        Vector2i test2 = null;
        for (BaseVector2i test : spiral) {
            if (test2 != null) {
                g.setColor(new Color(255, 0, 0, 128));
                g.drawLine(test2.getX(), test2.getY(), test.getX(), test.getY());
            }
            g.setColor(Color.RED);
            g.drawLine(test.getX(), test.getY(), test.getX(), test.getY());
            test2 = new Vector2i(test);
        }

        float rad = 3;
        g.create();
        g.setColor(Color.RED);
        Ellipse2D.Float shape = new Ellipse2D.Float(pos.getX() - rad, pos.getZ() - rad, rad * 2, rad * 2);
        g.fill(shape);


        // draw outline
//        g.setStroke(new BasicStroke(0.3f));
//        g.setColor(new Color(0x964B00)); // BROWN
//        g.draw(shape);
    }

}
