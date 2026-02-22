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

import java.util.List;

/**
 * A11: Map-Reduce — Fan-out to N agents sequentially, then aggregate results.
 * Each input is processed independently with the map prompt, then all results
 * are aggregated with the reduce prompt.
 */
@ApplicationScoped
public class MapReduceEngine {

    public AgentResult execute(AgentConfig config, List<String> inputs, String mapPrompt, String reducePrompt, LlmClient llm) {
        long start = System.currentTimeMillis();
        var context = new AgentContext();

        // Map: process each input independently
        StringBuilder mappedResults = new StringBuilder();
        for (int i = 0; i < inputs.size(); i++) {
            context.incrementIteration();
            var mapContext = new AgentContext();
            mapContext.addMessage(ChatMessage.system(mapPrompt));
            mapContext.addMessage(ChatMessage.user(inputs.get(i)));
            String result = LlmHelper.chat(config, mapContext, llm);
            context.addStep(Step.observation("Map[" + i + "]: " + result));
            mappedResults.append("Input ").append(i + 1).append(": ").append(result).append("\n\n");
        }

        // Reduce: aggregate
        var reduceContext = new AgentContext();
        reduceContext.addMessage(ChatMessage.system(reducePrompt));
        reduceContext.addMessage(ChatMessage.user(mappedResults.toString()));
        String reduced = LlmHelper.chat(config, reduceContext, llm);
        context.addStep(Step.synthesis(reduced));

        return AgentResult.of(reduced, context, System.currentTimeMillis() - start);
    }

}
