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

import io.quarkiverse.agent.runtime.llm.ChatMessage;
import io.quarkiverse.agent.runtime.llm.ChatRequest;
import io.quarkiverse.agent.runtime.llm.ChatResponse;
import io.quarkiverse.agent.runtime.llm.LlmClient;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Context-aware fake LLM for E2E testing.
 * Responds based on system prompt keywords to exercise full architecture flows.
 */
@Alternative
@Priority(1)
@ApplicationScoped
public class FakeLlmClient implements LlmClient {

    private final AtomicInteger callCount = new AtomicInteger(0);

    @Override
    public Uni<ChatResponse> chat(ChatRequest request) {
        int call = callCount.incrementAndGet();
        String systemPrompt = request.messages().stream()
                .filter(m -> "system".equals(m.role()))
                .map(ChatMessage::content)
                .reduce("", (a, b) -> a + " " + b);
        String userPrompt = request.messages().stream()
                .filter(m -> "user".equals(m.role()))
                .map(ChatMessage::content)
                .reduce("", (a, b) -> a + " " + b);

        String response = generateResponse(systemPrompt, userPrompt, call);

        var msg = ChatMessage.assistant(response);
        var choice = new ChatResponse.Choice(0, msg, "stop");
        var usage = new ChatResponse.Usage(10, 5, 15);
        return Uni.createFrom().item(
                new ChatResponse("fake-" + call, request.model(), java.util.List.of(choice), usage)
        );
    }

    private String generateResponse(String system, String user, int call) {
        // Reflection: approve on second critique
        if (system.contains("Critically review")) {
            return "APPROVED — the response is high quality.";
        }
        // Verification: verify on first check
        if (system.contains("strict verifier")) {
            return "VERIFIED — response is correct.";
        }
        // Pipeline: transform input
        if (system.contains("Pipeline") || system.contains("step")) {
            return "Processed: " + user.substring(0, Math.min(50, user.length()));
        }
        // Supervisor: assign all workers
        if (system.contains("supervisor") && system.contains("workers")) {
            return "WORKER: researcher | Research the topic\nWORKER: writer | Write the report";
        }
        // Debate: provide position
        if (system.contains("Debater")) {
            return "My position: The answer is 42. This is based on thorough analysis.";
        }
        // Arbiter: synthesize
        if (system.contains("arbiter")) {
            return "Consensus: All debaters agree the answer is 42.";
        }
        // Crew: process input
        if (system.contains("Your task:")) {
            return "Crew output: completed the assigned task successfully.";
        }
        // MapReduce: map/reduce
        if (system.contains("Synthesize") || system.contains("aggregate")) {
            return "Aggregated result from all inputs.";
        }
        // Plan-Execute: generate plan
        if (system.contains("step-by-step plan")) {
            return "1. Analyze the problem\n2. Implement solution\n3. Verify results";
        }
        // Plan-Execute: synthesize
        if (system.contains("Synthesize a final")) {
            return "Final synthesized answer from executed plan.";
        }
        // Default
        return "Hello from fake LLM! Model: " + (user.length() > 20 ? user.substring(0, 20) : user);
    }
}
