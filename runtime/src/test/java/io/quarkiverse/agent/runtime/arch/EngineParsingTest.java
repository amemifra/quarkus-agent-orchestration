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
package io.quarkiverse.agent.runtime.arch;

import io.quarkiverse.agent.runtime.core.AgentConfig;
import io.quarkiverse.agent.runtime.core.AgentResult;
import io.quarkiverse.agent.runtime.core.Tool;
import io.quarkiverse.agent.runtime.llm.ChatMessage;
import io.quarkiverse.agent.runtime.llm.ChatRequest;
import io.quarkiverse.agent.runtime.llm.ChatResponse;
import io.quarkiverse.agent.runtime.llm.LlmClient;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for engine parsing logic and core behaviors.
 * Uses a deterministic LlmClient that returns pre-scripted responses.
 */
class EngineParsingTest {

    // ── ToolUseEngine ──

    @Test
    void toolUseEngineParsesCaseInsensitiveToolCall() {
        var responses = List.of(
                "Let me calculate. tool_call: calculator | 2+2",
                "FINAL_ANSWER: The result is 4"
        );
        var llm = scriptedLlm(responses);

        var config = AgentConfig.builder()
                .model("test")
                .temperature(0.0)
                .maxIterations(5)
                .addTool(new Tool("calculator", "math ops", input -> "4"))
                .build();

        var engine = new ToolUseEngine();
        AgentResult result = engine.execute(config, "What is 2+2?", llm);

        assertEquals("The result is 4", result.content());
        assertTrue(result.steps().stream().anyMatch(s -> s.content().contains("calculator")));
    }

    @Test
    void toolUseEngineHandlesUnknownTool() {
        var responses = List.of(
                "TOOL_CALL: unknown_tool | some input",
                "FINAL_ANSWER: I couldn't find that tool"
        );
        var llm = scriptedLlm(responses);

        var config = AgentConfig.builder()
                .model("test")
                .temperature(0.0)
                .maxIterations(5)
                .addTool(new Tool("calculator", "math ops", input -> "4"))
                .build();

        var engine = new ToolUseEngine();
        AgentResult result = engine.execute(config, "Use unknown", llm);

        assertEquals("I couldn't find that tool", result.content());
        assertTrue(result.steps().stream().anyMatch(s -> s.content().contains("Tool not found")));
    }

    @Test
    void toolUseEngineRespectsMaxIterations() {
        // Always responds with a thought (never FINAL_ANSWER, never TOOL_CALL)
        var llm = constantLlm("I'm still thinking...");

        var config = AgentConfig.builder()
                .model("test")
                .temperature(0.0)
                .maxIterations(3)
                .build();

        var engine = new ToolUseEngine();
        AgentResult result = engine.execute(config, "Think forever", llm);

        // Should stop after 3 iterations
        assertNotNull(result);
        assertTrue(result.steps().size() <= 3);
    }

    // ── ReflectionEngine ──

    @Test
    void reflectionEngineStopsOnApproved() {
        var responses = List.of(
                "Here is my initial response about AI.",    // generation
                "APPROVED — the response is comprehensive." // critique
        );
        var llm = scriptedLlm(responses);

        var config = AgentConfig.builder()
                .model("test")
                .temperature(0.0)
                .maxIterations(5)
                .build();

        var engine = new ReflectionEngine();
        AgentResult result = engine.execute(config, "Tell me about AI", llm);

        assertEquals("Here is my initial response about AI.", result.content());
        assertTrue(result.steps().stream().anyMatch(s -> s.type() == io.quarkiverse.agent.runtime.core.Step.StepType.CRITIQUE));
    }

    @Test
    void reflectionEngineRefinesBeforeApproval() {
        var responses = List.of(
                "Initial draft.",                        // generation
                "Needs more detail on X.",                // critique
                "Improved draft with detail on X.",       // refinement
                "APPROVED"                                // second critique
        );
        var llm = scriptedLlm(responses);

        var config = AgentConfig.builder()
                .model("test")
                .temperature(0.0)
                .maxIterations(5)
                .build();

        var engine = new ReflectionEngine();
        AgentResult result = engine.execute(config, "Write about X", llm);

        // After first critique fails, it refines, then second critique approves
        // M3 note: returns the REFINED version because `generation` is updated in the refine step
        assertEquals("Improved draft with detail on X.", result.content());
    }

    // ── VerificationEngine ──

    @Test
    void verificationEngineStopsOnVerified() {
        var responses = List.of(
                "Paris is the capital of France.",  // generation
                "VERIFIED"                           // verification
        );
        var llm = scriptedLlm(responses);

        var config = AgentConfig.builder()
                .model("test")
                .temperature(0.0)
                .maxIterations(3)
                .build();

        var engine = new VerificationEngine();
        AgentResult result = engine.execute(config, "Capital of France?", llm);

        assertEquals("Paris is the capital of France.", result.content());
    }

    @Test
    void verificationEngineRetriesOnRejection() {
        var responses = List.of(
                "London is the capital.",             // gen 1 — wrong
                "Incorrect, London is UK capital.",   // verify 1 — rejected
                "Paris is the capital of France.",    // gen 2 — corrected
                "VERIFIED"                            // verify 2 — approved
        );
        var llm = scriptedLlm(responses);

        var config = AgentConfig.builder()
                .model("test")
                .temperature(0.0)
                .maxIterations(3)
                .build();

        var engine = new VerificationEngine();
        AgentResult result = engine.execute(config, "Capital of France?", llm);

        assertEquals("Paris is the capital of France.", result.content());
    }

    // ── Helpers ──

    /** Returns an LlmClient that cycles through pre-scripted responses. */
    private LlmClient scriptedLlm(List<String> responses) {
        AtomicInteger idx = new AtomicInteger(0);
        return request -> {
            int i = idx.getAndIncrement();
            String text = i < responses.size() ? responses.get(i) : "";
            return Uni.createFrom().item(createResponse(text));
        };
    }

    /** Returns an LlmClient that always returns the same response. */
    private LlmClient constantLlm(String response) {
        return request -> Uni.createFrom().item(createResponse(response));
    }

    private ChatResponse createResponse(String text) {
        var message = new ChatMessage("assistant", text);
        var choice = new ChatResponse.Choice(0, message, "stop");
        return new ChatResponse("test-id", "test-model", List.of(choice), null);
    }
}
