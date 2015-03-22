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

import org.kohsuke.args4j.Option;


/**
 * Command-line configuration options.
 * @author Martin Steiger
 */
public class CmdLineConfigs {

    @Option(name = "-h", aliases = "-help", usage = "print this message")
    boolean help;

    @Option(name = "-skip", usage = "skip the initial selection dialog and use defaults")
    boolean skipSelect;

    @Option(name = "-worldGen", metaVar = "<class>", usage = "The world generator to use")
    String worldGen;

    @Option(name = "-seed", metaVar = "<string>", usage = "The seed value to use")
    String seed;
}
