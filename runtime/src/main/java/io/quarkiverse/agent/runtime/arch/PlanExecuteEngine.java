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

import java.util.ArrayList;
import java.util.List;

/**
 * A2: Plan-and-Execute — Generate a plan, execute step-by-step, replan on failure.
 */
@ApplicationScoped
public class PlanExecuteEngine {

    public AgentResult execute(AgentConfig config, String userPrompt, LlmClient llm) {
        long start = System.currentTimeMillis();
        var context = new AgentContext();

        // Step 1: Generate plan
        var planContext = new AgentContext();
        planContext.addMessage(ChatMessage.system(
                (config.systemPrompt() != null ? config.systemPrompt() + "\n\n" : "")
                + "Create a step-by-step plan to accomplish the user's task. "
                + "Format each step as a numbered list. Be specific and actionable."));
        planContext.addMessage(ChatMessage.user(userPrompt));
        String plan = LlmHelper.chat(config, planContext, llm);
        context.addStep(Step.plan(plan));

        // Step 2: Execute each step
        String accumulatedResults = "";
        String[] plannedSteps = plan.split("\\n");
        for (int i = 0; i < plannedSteps.length && i < config.maxIterations(); i++) {
            String stepText = plannedSteps[i].trim();
            if (stepText.isEmpty()) continue;
            context.incrementIteration();

            var execContext = new AgentContext();
            execContext.addMessage(ChatMessage.system(
                    "Execute the following step from a plan. Provide the result.\n"
                    + "Previous results so far:\n" + accumulatedResults));
            execContext.addMessage(ChatMessage.user("Step to execute: " + stepText));

            String result = LlmHelper.chat(config, execContext, llm);
            context.addStep(Step.observation("Step result: " + result));
            accumulatedResults += "\n" + stepText + " → " + result;
        }

        // Step 3: Synthesize final answer
        var synthContext = new AgentContext();
        synthContext.addMessage(ChatMessage.system("Synthesize a final answer from the results of the executed plan."));
        synthContext.addMessage(ChatMessage.user(
                "Original task: " + userPrompt + "\n\nResults:\n" + accumulatedResults));
        String finalAnswer = LlmHelper.chat(config, synthContext, llm);
        context.addStep(Step.synthesis(finalAnswer));

        return AgentResult.of(finalAnswer, context, System.currentTimeMillis() - start);
    }

}
