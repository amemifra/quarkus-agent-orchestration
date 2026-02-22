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
package io.quarkiverse.agent.runtime.annotation;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registry of all @Agent-annotated classes discovered at build time.
 * Populated by the Recorder during Quarkus augmentation.
 */
@ApplicationScoped
public class AgentRegistry {

    private final List<AgentDefinition> agents = new ArrayList<>();

    public void register(AgentDefinition definition) {
        agents.add(definition);
    }

    public List<AgentDefinition> getAgents() {
        return Collections.unmodifiableList(agents);
    }

    public AgentDefinition findByClass(String className) {
        return agents.stream()
                .filter(a -> a.className().equals(className))
                .findFirst()
                .orElse(null);
    }
}
