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
import io.quarkiverse.agent.runtime.core.AgentConfig;
import io.quarkiverse.agent.runtime.core.AgentContext;
import io.quarkiverse.agent.runtime.core.AgentResult;
import io.quarkiverse.agent.runtime.core.Step;
import io.quarkiverse.agent.runtime.core.Tool;
import io.quarkiverse.agent.runtime.llm.ChatMessage;
import io.quarkiverse.agent.runtime.llm.LlmClient;
import io.quarkiverse.agent.runtime.llm.LlmHelper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A13: Tool-Use Agent — single agent with access to tools.
 * The LLM decides which tool to call based on the prompt.
 * Loop: ask LLM → if it mentions a tool, invoke it → feed result back.
 */
@ApplicationScoped
public class ToolUseEngine {

    private static final Pattern TOOL_CALL_PATTERN =
            Pattern.compile("(?i)TOOL_CALL:\\s*([^|]+?)\\s*\\|\\s*(.*)", Pattern.DOTALL);
    private static final Pattern FINAL_ANSWER_PATTERN =
            Pattern.compile("(?i)FINAL_ANSWER:\\s*(.*)", Pattern.DOTALL);

    public AgentResult execute(AgentConfig config, String userPrompt, LlmClient llm) {
        long start = System.currentTimeMillis();
        var context = new AgentContext();

        String toolsDescription = config.tools().stream()
                .map(t -> "- " + t.name() + ": " + t.description())
                .reduce("", (a, b) -> a + "\n" + b);

        String systemPrompt = (config.systemPrompt() != null ? config.systemPrompt() + "\n\n" : "")
                + "You have access to the following tools:\n" + toolsDescription
                + "\n\nTo use a tool, respond with: TOOL_CALL: <tool_name> | <input>"
                + "\nWhen you have the final answer, respond with: FINAL_ANSWER: <answer>";

        context.addMessage(ChatMessage.system(systemPrompt));
        context.addMessage(ChatMessage.user(userPrompt));

        while (!context.isDone() && context.iteration() < config.maxIterations()) {
            context.incrementIteration();

            String response = LlmHelper.chat(config, context, llm);
            if (response.isEmpty()) {
                context.addStep(Step.observation("LLM call failed or timed out"));
                break;
            }
            context.addMessage(ChatMessage.assistant(response));

            Matcher toolMatcher = TOOL_CALL_PATTERN.matcher(response);
            Matcher finalMatcher = FINAL_ANSWER_PATTERN.matcher(response);

            if (toolMatcher.find()) {
                String toolName = toolMatcher.group(1).trim();
                String toolInput = toolMatcher.group(2).trim();

                context.addStep(Step.action("Tool call: " + toolName + " | " + toolInput));

                Tool tool = config.tools().stream()
                        .filter(t -> t.name().equalsIgnoreCase(toolName))
                        .findFirst()
                        .orElse(null);

                if (tool != null) {
                    String result = tool.invoke(toolInput);
                    context.addStep(Step.observation(result));
                    context.addMessage(ChatMessage.user("Tool result: " + result));
                } else {
                    context.addStep(Step.observation("Tool not found: " + toolName));
                    context.addMessage(ChatMessage.user("Error: Tool '" + toolName + "' not found."));
                }
            } else if (finalMatcher.find()) {
                String answer = finalMatcher.group(1).trim();
                context.addStep(Step.thought(answer));
                context.markDone();
                return AgentResult.of(answer, context, System.currentTimeMillis() - start);
            } else {
                context.addStep(Step.thought(response));
            }
        }

        String lastContent = context.steps().isEmpty() ? "" :
                context.steps().getLast().content();
        return AgentResult.of(lastContent, context, System.currentTimeMillis() - start);
    }
}
