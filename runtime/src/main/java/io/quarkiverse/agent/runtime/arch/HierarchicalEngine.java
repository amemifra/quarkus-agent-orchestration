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
 * A6: Hierarchical — Multi-level supervisor tree.
 * Top-level supervisor delegates to mid-level supervisors, which delegate to workers.
 */
@ApplicationScoped
public class HierarchicalEngine {

    private final SupervisorEngine supervisorEngine = new SupervisorEngine();

    public AgentResult execute(AgentConfig config, List<TeamDef> teams, String userPrompt, LlmClient llm) {
        long start = System.currentTimeMillis();
        var context = new AgentContext();

        // Each team runs as a sub-supervisor
        StringBuilder teamResults = new StringBuilder();
        for (TeamDef team : teams) {
            context.incrementIteration();
            context.addStep(Step.delegation("Delegating to team: " + team.name()));
            AgentResult teamResult = supervisorEngine.execute(config, team.workers(), userPrompt, llm);
            teamResults.append(team.name()).append(": ").append(teamResult.content()).append("\n\n");
            context.addStep(Step.observation("Team " + team.name() + " completed"));
        }

        // Top-level synthesis
        var synthContext = new AgentContext();
        synthContext.addMessage(ChatMessage.system(
                "You are a top-level supervisor. Synthesize results from multiple teams into a final answer."));
        synthContext.addMessage(ChatMessage.user(
                "Task: " + userPrompt + "\n\nTeam results:\n" + teamResults));
        String finalAnswer = LlmHelper.chat(config, synthContext, llm);
        context.addStep(Step.synthesis(finalAnswer));

        return AgentResult.of(finalAnswer, context, System.currentTimeMillis() - start);
    }


    @RegisterForReflection
    public record TeamDef(String name, List<SupervisorEngine.WorkerDef> workers) {
        public static TeamDef of(String name, List<SupervisorEngine.WorkerDef> workers) {
            return new TeamDef(name, workers);
        }
    }
}
