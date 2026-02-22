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

import java.util.Map;

/**
 * A10: Handoff — Agents pass control dynamically based on context.
 * No central orchestrator; each agent decides who handles next.
 */
@ApplicationScoped
public class HandoffEngine {

    public AgentResult execute(AgentConfig config, Map<String, HandoffAgent> agents, String startAgent,
                               String userPrompt, LlmClient llm) {
        long start = System.currentTimeMillis();
        var context = new AgentContext();
        String currentAgent = startAgent;
        String currentInput = userPrompt;

        while (currentAgent != null && !currentAgent.equals("END") && context.iteration() < config.maxIterations()) {
            context.incrementIteration();
            HandoffAgent agent = agents.get(currentAgent);
            if (agent == null) break;

            context.addStep(Step.delegation("Handoff to: " + currentAgent));

            String agentList = String.join(", ", agents.keySet());
            var agentCtx = new AgentContext();
            agentCtx.addMessage(ChatMessage.system(
                    "You are " + currentAgent + ". " + agent.systemPrompt()
                    + "\n\nAfter your response, decide the next step:"
                    + "\n- HANDOFF: <agent_name> if another agent should handle this"
                    + "\n- DONE if the task is complete"
                    + "\n\nAvailable agents: " + agentList));
            agentCtx.addMessage(ChatMessage.user(currentInput));

            String response = LlmHelper.chat(config, agentCtx, llm);
            context.addStep(Step.observation(currentAgent + ": " + response));

            if (response.contains("DONE")) {
                String answer = response.replace("DONE", "").trim();
                context.markDone();
                return AgentResult.of(answer, context, System.currentTimeMillis() - start);
            } else if (response.contains("HANDOFF:")) {
                String nextAgent = response.substring(response.indexOf("HANDOFF:") + 8).trim().split("\\s")[0];
                currentInput = response;
                currentAgent = agents.containsKey(nextAgent) ? nextAgent : null;
            } else {
                currentInput = response;
                currentAgent = null; // No handoff instruction, end
            }
        }

        String lastContent = context.steps().isEmpty() ? "" :
                context.steps().getLast().content();
        return AgentResult.of(lastContent, context, System.currentTimeMillis() - start);
    }


    @RegisterForReflection
    public record HandoffAgent(String systemPrompt) {
        public static HandoffAgent of(String systemPrompt) {
            return new HandoffAgent(systemPrompt);
        }
    }
}
