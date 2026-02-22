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
 * A7: Role-Based Crew — Agents with defined roles collaborate on tasks.
 * Sequential execution: each agent builds on the previous one's output.
 */
@ApplicationScoped
public class CrewEngine {

    public AgentResult execute(AgentConfig config, List<CrewMember> crew, String userPrompt, LlmClient llm) {
        long start = System.currentTimeMillis();
        var context = new AgentContext();
        String current = userPrompt;

        for (CrewMember member : crew) {
            context.incrementIteration();
            context.addStep(Step.delegation("Agent: " + member.role() + " — " + member.task()));

            var memberCtx = new AgentContext();
            memberCtx.addMessage(ChatMessage.system(
                    "You are a " + member.role() + ". " + member.backstory()
                    + "\nYour task: " + member.task()));
            memberCtx.addMessage(ChatMessage.user(current));

            current = LlmHelper.chat(config, memberCtx, llm);
            context.addStep(Step.observation(member.role() + " output completed"));
        }

        return AgentResult.of(current, context, System.currentTimeMillis() - start);
    }


    @RegisterForReflection
    public record CrewMember(String role, String backstory, String task) {
        public static CrewMember of(String role, String backstory, String task) {
            return new CrewMember(role, backstory, task);
        }
    }
}
