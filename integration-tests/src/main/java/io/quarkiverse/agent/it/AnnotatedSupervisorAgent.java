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

import io.quarkiverse.agent.runtime.annotation.*;

/** Annotated Supervisor agent with workers. */
@Agent(model = "llama3", architecture = Architecture.SUPERVISOR)
@Worker(name = "researcher", systemPrompt = "You are a research expert")
@Worker(name = "writer", systemPrompt = "You are a technical writer")
public class AnnotatedSupervisorAgent { }
