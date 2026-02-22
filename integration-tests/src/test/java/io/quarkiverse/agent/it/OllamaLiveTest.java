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

import io.quarkiverse.agent.runtime.AgentOrchestrationService;
import io.quarkiverse.agent.runtime.core.AgentConfig;
import io.quarkiverse.agent.runtime.core.AgentResult;
import io.quarkiverse.agent.runtime.llm.LlmClient;
import io.quarkiverse.agent.runtime.llm.ChatRequest;
import io.quarkiverse.agent.runtime.llm.ChatResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live Ollama integration tests.
 * Requires Ollama running at localhost:11434 with the tinyllama model.
 * Run with: mvn test -pl integration-tests -Dgroups=ollama
 */
@QuarkusTest
@TestProfile(OllamaTestProfile.class)
@Tag("ollama")
class OllamaLiveTest {

    @Inject
    LlmClient llmClient;

    @Inject
    AgentOrchestrationService agents;

    @Test
    void llmClientConnectsToOllama() {
        var request = ChatRequest.builder()
                .model("tinyllama")
                .userMessage("Reply with exactly: hello")
                .temperature(0.0)
                .maxTokens(20)
                .build();

        ChatResponse response = llmClient.chat(request).await().indefinitely();

        assertNotNull(response);
        assertNotNull(response.content());
        assertFalse(response.content().isBlank(), "Ollama should return content");
    }

    @Test
    void reflectArchitectureWithRealLlm() {
        var config = AgentConfig.builder()
                .model("tinyllama")
                .temperature(0.3)
                .maxIterations(2)
                .build();

        AgentResult result = agents.reflect(config, "Write a one-line haiku about Java.");

        assertNotNull(result);
        assertFalse(result.content().isBlank(), "Reflection should produce content");
        assertFalse(result.steps().isEmpty(), "Should have execution steps");
        assertTrue(result.durationMs() > 0, "Should track duration");
    }
}
