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

package org.terasology.worldviewer.config;

import org.terasology.worldviewer.layers.FacetLayer;

import com.google.gson.JsonElement;

class ConfigEntry {

    Class<? extends FacetLayer> facetClass;
    Class<? extends FacetConfig> configClass;
    JsonElement data;

    public ConfigEntry(FacetLayer layer, JsonElement data) {
        this.facetClass = layer.getClass();
        this.configClass = layer.getConfig() != null ? layer.getConfig().getClass() : null;
        this.data = data;
    }
}
