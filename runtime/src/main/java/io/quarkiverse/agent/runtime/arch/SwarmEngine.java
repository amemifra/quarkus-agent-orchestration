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
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.quarkiverse.agent.runtime.llm.LlmHelper;
import io.quarkiverse.agent.runtime.core.AgentConfig;
import io.quarkiverse.agent.runtime.core.AgentContext;
import io.quarkiverse.agent.runtime.core.AgentResult;
import io.quarkiverse.agent.runtime.core.Step;
import io.quarkiverse.agent.runtime.llm.ChatMessage;
import io.quarkiverse.agent.runtime.llm.LlmClient;

import java.util.List;

/**
 * A8: Conversational Swarm — Agents converse with each other in natural language.
 * Emergent problem solving through multi-turn discussion.
 */
@ApplicationScoped
public class SwarmEngine {

    public AgentResult execute(AgentConfig config, List<SwarmAgent> agents, String topic, LlmClient llm) {
        long start = System.currentTimeMillis();
        var context = new AgentContext();
        StringBuilder conversation = new StringBuilder("Topic: " + topic + "\n\n");

        for (int round = 0; round < config.maxIterations(); round++) {
            for (SwarmAgent agent : agents) {
                context.incrementIteration();

                var agentCtx = new AgentContext();
                agentCtx.addMessage(ChatMessage.system(
                        "You are " + agent.name() + ". " + agent.personality()
                        + "\nEngage in a discussion. If the group has reached a consensus or solved the problem, "
                        + "include CONSENSUS_REACHED in your response."));
                agentCtx.addMessage(ChatMessage.user(conversation.toString()));

                String response = LlmHelper.chat(config, agentCtx, llm);
                context.addStep(Step.thought(agent.name() + ": " + response));
                conversation.append(agent.name()).append(": ").append(response).append("\n\n");

                if (response.contains("CONSENSUS_REACHED")) {
                    context.markDone();
                    return AgentResult.of(response, context, System.currentTimeMillis() - start);
                }
            }
        }

        // Final synthesis
        var synthCtx = new AgentContext();
        synthCtx.addMessage(ChatMessage.system("Summarize the key conclusions from the following discussion."));
        synthCtx.addMessage(ChatMessage.user(conversation.toString()));
        String summary = LlmHelper.chat(config, synthCtx, llm);
        context.addStep(Step.synthesis(summary));

        return AgentResult.of(summary, context, System.currentTimeMillis() - start);
    }


    @RegisterForReflection
    public record SwarmAgent(String name, String personality) {
        public static SwarmAgent of(String name, String personality) {
            return new SwarmAgent(name, personality);
        }
    }
}
