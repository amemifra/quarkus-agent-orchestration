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
package io.quarkiverse.agent.it;

import io.quarkiverse.agent.runtime.annotation.Agent;
import io.quarkiverse.agent.runtime.annotation.Architecture;
import io.quarkiverse.agent.runtime.annotation.EvaluateOutput;
import io.quarkiverse.agent.runtime.annotation.Generate;
import io.quarkiverse.agent.runtime.annotation.UserPrompt;
import io.quarkiverse.agent.runtime.llm.InferenceQuality;

/**
 * Showcase: a declarative agent with quality tier annotation.
 */
@Agent(provider = "ollama", model = "llama3", architecture = Architecture.REFLECTION,
        maxIterations = 3, quality = InferenceQuality.HIGH)
public class AnnotatedWriterAgent {

    @Generate
    public String write(@UserPrompt String topic) {
        return "Write a creative poem about: " + topic;
    }

    @EvaluateOutput
    public boolean isGoodEnough(String output) {
        return output != null && output.length() > 10;
    }
}
