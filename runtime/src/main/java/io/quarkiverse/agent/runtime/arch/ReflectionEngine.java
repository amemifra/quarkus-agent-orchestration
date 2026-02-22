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

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkiverse.agent.runtime.llm.LlmHelper;
import io.quarkiverse.agent.runtime.core.AgentConfig;
import io.quarkiverse.agent.runtime.core.AgentContext;
import io.quarkiverse.agent.runtime.core.AgentResult;
import io.quarkiverse.agent.runtime.core.Step;
import io.quarkiverse.agent.runtime.llm.ChatMessage;
import io.quarkiverse.agent.runtime.llm.LlmClient;

/**
 * A3: Reflection — Generate → Critique → Refine loop.
 * The agent generates output, then critiques it, then refines until quality is acceptable.
 */
@ApplicationScoped
public class ReflectionEngine {

    public AgentResult execute(AgentConfig config, String userPrompt, LlmClient llm) {
        long start = System.currentTimeMillis();
        var context = new AgentContext();

        // Step 1: Generate initial response
        context.addMessage(ChatMessage.system(
                config.systemPrompt() != null ? config.systemPrompt() : "You are a helpful assistant."));
        context.addMessage(ChatMessage.user(userPrompt));

        String generation = LlmHelper.chat(config, context, llm);
        context.addStep(Step.thought("Initial generation"));
        context.addMessage(ChatMessage.assistant(generation));

        // Steps 2..N: Critique and Refine loop
        for (int i = 0; i < config.maxIterations() - 1; i++) {
            context.incrementIteration();

            // Critique
            context.addMessage(ChatMessage.user(
                    "Critically review the above response. List specific issues, errors, or improvements needed. "
                    + "If the response is already high quality, respond with: APPROVED"));
            String critique = LlmHelper.chat(config, context, llm);
            context.addStep(Step.critique(critique));
            context.addMessage(ChatMessage.assistant(critique));

            if (critique.contains("APPROVED")) {
                context.markDone();
                return AgentResult.of(generation, context, System.currentTimeMillis() - start);
            }

            // Refine
            context.addMessage(ChatMessage.user(
                    "Based on the critique above, provide an improved version of the original response."));
            generation = LlmHelper.chat(config, context, llm);
            context.addStep(Step.refinement(generation));
            context.addMessage(ChatMessage.assistant(generation));
        }

        return AgentResult.of(generation, context, System.currentTimeMillis() - start);
    }

}
