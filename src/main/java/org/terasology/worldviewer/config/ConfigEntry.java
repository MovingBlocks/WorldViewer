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

import org.terasology.world.viewer.layers.FacetLayer;
import org.terasology.world.viewer.layers.FacetLayerConfig;

import com.google.gson.JsonElement;

class ConfigEntry {

    private Class<? extends FacetLayer> facetClass;
    private Class<? extends FacetLayerConfig> configClass;
    private JsonElement data;
    private boolean visible;

    public ConfigEntry(FacetLayer layer, JsonElement data, boolean visible) {
        this.facetClass = layer.getClass();
        this.configClass = layer.getConfig() != null ? layer.getConfig().getClass() : null;
        this.data = data;
        this.visible = visible;
    }

    public Class<? extends FacetLayer> getFacetClass() {
        return facetClass;
    }

    public Class<? extends FacetLayerConfig> getConfigClass() {
        return configClass;
    }

    public JsonElement getData() {
        return data;
    }

    public boolean isVisible() {
        return visible;
    }
}
