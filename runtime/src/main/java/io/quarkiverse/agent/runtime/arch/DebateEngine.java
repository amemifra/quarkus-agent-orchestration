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
 * A12: Debate-Consensus — N agents generate independent responses, an arbiter synthesizes.
 */
@ApplicationScoped
public class DebateEngine {

    public AgentResult execute(AgentConfig config, int numDebaters, String userPrompt, LlmClient llm) {
        long start = System.currentTimeMillis();
        var context = new AgentContext();

        // Phase 1: Independent responses
        StringBuilder positions = new StringBuilder();
        for (int i = 0; i < numDebaters; i++) {
            context.incrementIteration();
            var debaterCtx = new AgentContext();
            debaterCtx.addMessage(ChatMessage.system(
                    "You are Debater " + (i + 1) + ". Provide your independent analysis and position. "
                    + "Be thorough and defend your reasoning."));
            debaterCtx.addMessage(ChatMessage.user(userPrompt));
            String position = LlmHelper.chat(config, debaterCtx, llm);
            context.addStep(Step.thought("Debater " + (i + 1) + ": " + position));
            positions.append("Debater ").append(i + 1).append(": ").append(position).append("\n\n");
        }

        // Phase 2: Arbiter synthesizes consensus
        var arbiterCtx = new AgentContext();
        arbiterCtx.addMessage(ChatMessage.system(
                "You are an impartial arbiter. Analyze the following positions from multiple debaters. "
                + "Identify areas of agreement and disagreement. Synthesize the best consensus answer."));
        arbiterCtx.addMessage(ChatMessage.user(
                "Question: " + userPrompt + "\n\nPositions:\n" + positions));
        String consensus = LlmHelper.chat(config, arbiterCtx, llm);
        context.addStep(Step.synthesis(consensus));

        return AgentResult.of(consensus, context, System.currentTimeMillis() - start);
    }

}
