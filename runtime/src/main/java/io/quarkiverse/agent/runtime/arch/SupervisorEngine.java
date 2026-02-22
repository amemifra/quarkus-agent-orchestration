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
import io.quarkiverse.agent.runtime.core.AgentConfig;
import io.quarkiverse.agent.runtime.core.AgentContext;
import io.quarkiverse.agent.runtime.core.AgentResult;
import io.quarkiverse.agent.runtime.core.Step;
import io.quarkiverse.agent.runtime.llm.ChatMessage;
import io.quarkiverse.agent.runtime.llm.LlmClient;
import io.quarkiverse.agent.runtime.llm.LlmHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A5: Supervisor — one agent delegates tasks to N workers and synthesizes results.
 * <p>
 * The supervisor first decides which workers to involve and what specific sub-tasks
 * to assign each. Workers then execute their assigned tasks, and the supervisor
 * synthesizes the results.
 */
@ApplicationScoped
public class SupervisorEngine {

    private static final Pattern WORKER_ASSIGNMENT =
            Pattern.compile("(?im)^\\s*WORKER:\\s*([^|]+?)\\s*\\|\\s*(.+)$");

    public AgentResult execute(AgentConfig config, List<WorkerDef> workers, String userPrompt, LlmClient llm) {
        long start = System.currentTimeMillis();
        var context = new AgentContext();

        // Step 1: Supervisor routes/delegates
        String workerList = workers.stream()
                .map(w -> "- " + w.name() + ": " + w.description())
                .reduce("", (a, b) -> a + "\n" + b);

        var routeContext = new AgentContext();
        routeContext.addMessage(ChatMessage.system(
                "You are a supervisor. Assign the user's task to one or more workers.\n"
                + "Available workers:\n" + workerList
                + "\n\nRespond with the task assignment for EACH worker, one per line: WORKER: <name> | <task>"));
        routeContext.addMessage(ChatMessage.user(userPrompt));
        String assignments = LlmHelper.chat(config, routeContext, llm);
        context.addStep(Step.delegation(assignments));

        // Step 2: Parse assignments and route specific sub-tasks
        Map<String, String> parsedAssignments = parseAssignments(assignments);

        StringBuilder workerResults = new StringBuilder();
        for (WorkerDef worker : workers) {
            // Use specific assigned task if available, otherwise fall back to original prompt
            String workerTask = parsedAssignments.getOrDefault(worker.name(), userPrompt);
            context.incrementIteration();

            var workerContext = new AgentContext();
            workerContext.addMessage(ChatMessage.system(worker.systemPrompt()));
            workerContext.addMessage(ChatMessage.user(workerTask));
            String result = LlmHelper.chat(config, workerContext, llm);
            context.addStep(Step.observation(worker.name() + ": " + result));
            workerResults.append(worker.name()).append(": ").append(result).append("\n\n");
        }

        // Step 3: Supervisor synthesizes
        var synthContext = new AgentContext();
        synthContext.addMessage(ChatMessage.system("Synthesize the workers' results into a final answer."));
        synthContext.addMessage(ChatMessage.user(
                "Task: " + userPrompt + "\n\nWorker results:\n" + workerResults));
        String finalAnswer = LlmHelper.chat(config, synthContext, llm);
        context.addStep(Step.synthesis(finalAnswer));

        return AgentResult.of(finalAnswer, context, System.currentTimeMillis() - start);
    }

    /** Parse WORKER: <name> | <task> lines from the supervisor's routing response. */
    private Map<String, String> parseAssignments(String assignments) {
        Map<String, String> result = new HashMap<>();
        Matcher matcher = WORKER_ASSIGNMENT.matcher(assignments);
        while (matcher.find()) {
            result.put(matcher.group(1).trim(), matcher.group(2).trim());
        }
        return result;
    }

    @RegisterForReflection
    public record WorkerDef(String name, String description, String systemPrompt) {
        public static WorkerDef of(String name, String description, String systemPrompt) {
            return new WorkerDef(name, description, systemPrompt);
        }
    }
}
