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

package org.terasology.worldviewer.layers;

import java.awt.image.BufferedImage;

import org.terasology.world.generation.Region;
import org.terasology.world.generation.WorldFacet;
import org.terasology.worldviewer.core.Observer;

/**
 * A visual representation of a facet class
 * @author Martin Steiger
 */
public interface FacetLayer  {

    Class<? extends WorldFacet> getFacetClass();

    void render(BufferedImage img, Region r);

    String getWorldText(Region r, int wx, int wy);

    /**
     * @return true if visible
     */
    boolean isVisible();

    void setVisible(boolean yesno);

    /**
     * @param obs the observer to add
     */
    void addObserver(Observer<FacetLayer> obs);

    /**
     * @param obs the observer to remove
     */
    void removeObserver(Observer<FacetLayer> obs);
}
