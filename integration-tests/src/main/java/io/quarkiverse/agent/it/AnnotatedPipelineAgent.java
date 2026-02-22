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
import io.quarkiverse.agent.runtime.annotation.PipelineStep;

/**
 * Annotated pipeline agent: 3 ordered steps.
 */
@Agent(model = "llama3", architecture = Architecture.PIPELINE)
@PipelineStep(name = "summarize", systemPrompt = "Summarize the input.", order = 1)
@PipelineStep(name = "translate", systemPrompt = "Translate to Italian.", order = 2)
@PipelineStep(name = "format", systemPrompt = "Format as bullet points.", order = 3)
public class AnnotatedPipelineAgent {
}
