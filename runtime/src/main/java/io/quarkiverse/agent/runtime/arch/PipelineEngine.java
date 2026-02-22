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
 * A9: DAG Pipeline — Linear/DAG chain of predefined steps.
 * Each step processes the output of the previous one. No cycles.
 */
@ApplicationScoped
public class PipelineEngine {

    public AgentResult execute(AgentConfig config, List<PipelineStep> steps, String input, LlmClient llm) {
        long start = System.currentTimeMillis();
        var context = new AgentContext();
        String current = input;

        for (int i = 0; i < steps.size(); i++) {
            context.incrementIteration();
            PipelineStep step = steps.get(i);

            context.addStep(Step.plan("Pipeline step " + (i + 1) + ": " + step.name()));

            var stepContext = new AgentContext();
            stepContext.addMessage(ChatMessage.system(step.systemPrompt()));
            stepContext.addMessage(ChatMessage.user(current));

            current = LlmHelper.chat(config, stepContext, llm);
            context.addStep(Step.observation("Output from step " + (i + 1)));
        }

        return AgentResult.of(current, context, System.currentTimeMillis() - start);
    }


    /**
     * A single step in a pipeline. Defines name and system prompt.
     */
    @RegisterForReflection
    public record PipelineStep(String name, String systemPrompt) {
        public static PipelineStep of(String name, String systemPrompt) {
            return new PipelineStep(name, systemPrompt);
        }
    }
}
