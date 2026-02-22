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
import io.quarkiverse.agent.runtime.annotation.CrewMember;

/**
 * Annotated crew agent: 3 members defined declaratively.
 */
@Agent(model = "llama3", architecture = Architecture.CREW)
@CrewMember(role = "Researcher", backstory = "Expert analyst", task = "Research the topic")
@CrewMember(role = "Writer", backstory = "Technical writer", task = "Write a report")
@CrewMember(role = "Reviewer", backstory = "QA specialist", task = "Review the output")
public class AnnotatedCrewAgent {
}
