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
package io.quarkiverse.agent.it;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;
import java.util.Set;

/**
 * Test profile for live Ollama tests.
 * Disables FakeLlmClient so the real OpenAiLlmClient connects to Ollama.
 */
public class OllamaTestProfile implements QuarkusTestProfile {

    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        // Return empty set — FakeLlmClient won't be activated
        return Set.of();
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.agent.base-url", "http://localhost:11434"
        );
    }

    @Override
    public String getConfigProfile() {
        return "ollama";
    }
}
