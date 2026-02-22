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
import io.quarkiverse.agent.runtime.llm.LlmClient;

/**
 * A1: ReAct (Reason + Act) — Thought → Action → Observation loop.
 * The agent reasons about what to do, executes an action, observes the result, and iterates.
 */
@ApplicationScoped
public class ReActEngine {

    @jakarta.inject.Inject
    ToolUseEngine toolUseEngine;

    public AgentResult execute(AgentConfig config, String userPrompt, LlmClient llm) {
        long start = System.currentTimeMillis();
        var context = new AgentContext();

        String systemPrompt = (config.systemPrompt() != null ? config.systemPrompt() + "\n\n" : "")
                + "You are a ReAct agent. For each step, follow this format:\n"
                + "Thought: <your reasoning>\n"
                + "Action: TOOL_CALL: <tool_name> | <input>\n"
                + "If you have the final answer:\n"
                + "Thought: <final reasoning>\n"
                + "FINAL_ANSWER: <answer>";

        var builder = AgentConfig.builder()
                .model(config.model())
                .systemPrompt(systemPrompt)
                .temperature(config.temperature())
                .maxIterations(config.maxIterations());
        config.tools().forEach(builder::addTool);
        var reactConfig = builder.build();

        return toolUseEngine.execute(reactConfig, userPrompt, llm);
    }
}
