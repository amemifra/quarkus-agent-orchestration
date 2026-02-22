/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkiverse.agent.runtime.core;

import java.util.function.Function;

/**
 * A tool that an agent can invoke during execution.
 */
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record Tool(
        String name,
        String description,
        Function<String, String> executor
) {
    public static Tool of(String name, String description, Function<String, String> executor) {
        return new Tool(name, description, executor);
    }

    public String invoke(String input) {
        return executor.apply(input);
    }
}
