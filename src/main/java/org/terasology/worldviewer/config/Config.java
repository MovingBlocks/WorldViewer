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

package org.terasology.worldviewer.config;

/**
 * The root class for all configs
 * @author Martin Steiger
 */
public class Config {

    private ViewConfig viewConfig = new ViewConfig();

    private WorldConfig worldConfig = new WorldConfig();

    public ViewConfig getViewConfig() {
        return viewConfig;
    }

    public WorldConfig getWorldConfig() {
        return worldConfig;
    }
}
