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
package io.quarkiverse.agent.runtime.core;

/**
 * A single execution step in an agent's reasoning trace.
 */
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record Step(
        StepType type,
        String content
) {
    public enum StepType {
        THOUGHT, ACTION, OBSERVATION, PLAN, CRITIQUE, REFINEMENT, DELEGATION, SYNTHESIS
    }

    public static Step thought(String content) { return new Step(StepType.THOUGHT, content); }
    public static Step action(String content) { return new Step(StepType.ACTION, content); }
    public static Step observation(String content) { return new Step(StepType.OBSERVATION, content); }
    public static Step plan(String content) { return new Step(StepType.PLAN, content); }
    public static Step critique(String content) { return new Step(StepType.CRITIQUE, content); }
    public static Step refinement(String content) { return new Step(StepType.REFINEMENT, content); }
    public static Step delegation(String content) { return new Step(StepType.DELEGATION, content); }
    public static Step synthesis(String content) { return new Step(StepType.SYNTHESIS, content); }
}
