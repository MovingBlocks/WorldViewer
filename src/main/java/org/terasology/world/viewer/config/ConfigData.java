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

package org.terasology.world.viewer.config;

import java.util.Map;

import org.terasology.engine.SimpleUri;
import org.terasology.naming.Version;

import com.google.common.collect.Maps;

/**
 * The root class for all configs
 */
class ConfigData {

    Version version = new Version(0, 0, 0);

    ViewConfig viewConfig = new ViewConfig();

    WorldConfig worldConfig = new WorldConfig();

    Map<SimpleUri, WorldGenConfigData> worldGenConfigs = Maps.newHashMap();
}


