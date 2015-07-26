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

package org.terasology.worldviewer;

import org.terasology.math.Region3i;
import org.terasology.world.generation.Region;
import org.terasology.world.generation.WorldFacet;

/**
 * A thread-safe wrapping class for {@link Region} that
 * synchronizes access to {@link #getFacet(Class)}. It assumes that
 * {@link #getRegion()} does not need synchronizing.
 */
public class ThreadSafeRegion implements Region {

    private final Region base;

    /**
     * @param base the underlying original region this implementation uses
     */
    public ThreadSafeRegion(Region base) {
        this.base = base;
    }

    @Override
    public synchronized <T extends WorldFacet> T getFacet(Class<T> dataType) {
        return base.getFacet(dataType);
    }

    @Override
    public Region3i getRegion() {
        return base.getRegion();
    }

    @Override
    public String toString() {
        return "ThreadSafeRegion [" + getRegion() + "]";
    }
}
