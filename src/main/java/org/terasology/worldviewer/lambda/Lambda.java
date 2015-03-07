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

package org.terasology.worldviewer.lambda;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Martin Steiger
 */
public final class Lambda {

    private Lambda() {
        // no instances
    }

    public static <T> Supplier<T> toRuntime(CheckedSupplier<T> checkedSupp) {
        return () -> {
            try {
                return checkedSupp.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static <T> Consumer<T> toRuntime(CheckedConsumer<T> checkedConsumer) {
        return (t) -> {
            try {
                checkedConsumer.accept(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
