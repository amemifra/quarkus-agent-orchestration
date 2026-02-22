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
package io.quarkiverse.agent.runtime.annotation;

import java.lang.annotation.*;

/**
 * Defines a crew member for a CREW architecture agent.
 * Repeatable — multiple members form the crew.
 *
 * <pre>
 * &#64;Agent(model = "llama3", architecture = Architecture.CREW)
 * &#64;CrewMember(role = "Researcher", backstory = "Expert analyst", task = "Research the topic")
 * &#64;CrewMember(role = "Writer", backstory = "Technical writer", task = "Write a report")
 * public class ResearchCrew { }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(CrewMembers.class)
public @interface CrewMember {
    String role();
    String backstory() default "";
    String task();
}
