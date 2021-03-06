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

import java.awt.Graphics2D;

import org.terasology.math.geom.ImmutableVector2i;
import org.terasology.math.geom.Rect2i;

/**
 * Overlays are rendered live on top of the 2D world.
 */
public interface Overlay {

    void render(Graphics2D g, Rect2i area, ImmutableVector2i cursor);

    void setVisible(boolean yesno);

    boolean isVisible();
}
