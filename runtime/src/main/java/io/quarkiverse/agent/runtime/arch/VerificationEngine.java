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
 * A14: Verification Loop — Generator + Verifier.
 * One LLM call generates, another verifies for correctness/hallucination.
 */
@ApplicationScoped
public class VerificationEngine {

    public AgentResult execute(AgentConfig config, String userPrompt, LlmClient llm) {
        long start = System.currentTimeMillis();
        var context = new AgentContext();

        String generation = "";

        for (int i = 0; i < config.maxIterations(); i++) {
            context.incrementIteration();

            // Generate
            var genContext = new AgentContext();
            genContext.addMessage(ChatMessage.system(
                    config.systemPrompt() != null ? config.systemPrompt() : "You are a helpful assistant."));
            if (i == 0) {
                genContext.addMessage(ChatMessage.user(userPrompt));
            } else {
                genContext.addMessage(ChatMessage.user(userPrompt
                        + "\n\nPrevious attempt was rejected. Please provide a corrected response."));
            }
            generation = LlmHelper.chat(config, genContext, llm);
            context.addStep(Step.thought("Generation attempt " + (i + 1)));

            // Verify
            var verifyContext = new AgentContext();
            verifyContext.addMessage(ChatMessage.system(
                    "You are a strict verifier. Check the following response for errors, hallucinations, and quality issues. "
                    + "If the response is correct and high-quality, respond with: VERIFIED\n"
                    + "Otherwise, explain what's wrong."));
            verifyContext.addMessage(ChatMessage.user(
                    "Original prompt: " + userPrompt + "\n\nResponse to verify:\n" + generation));
            String verification = LlmHelper.chat(config, verifyContext, llm);

            if (verification.contains("VERIFIED")) {
                context.addStep(Step.observation("Verified ✓"));
                context.markDone();
                return AgentResult.of(generation, context, System.currentTimeMillis() - start);
            } else {
                context.addStep(Step.critique(verification));
            }
        }

        return AgentResult.of(generation, context, System.currentTimeMillis() - start);
    }

}
