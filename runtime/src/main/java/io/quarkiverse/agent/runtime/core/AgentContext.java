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

import io.quarkiverse.agent.runtime.llm.ChatMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mutable shared state for an agent execution.
 * Tracks conversation history, variables, and iteration count.
 */
public class AgentContext {

    private final List<ChatMessage> messages = new ArrayList<>();
    private final List<Step> steps = new ArrayList<>();
    private final Map<String, Object> variables = new HashMap<>();
    private int iteration;
    private boolean done;

    public AgentContext addMessage(ChatMessage message) {
        messages.add(message);
        return this;
    }

    public AgentContext addStep(Step step) {
        steps.add(step);
        return this;
    }

    public AgentContext setVariable(String key, Object value) {
        variables.put(key, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key) {
        return (T) variables.get(key);
    }

    public AgentContext incrementIteration() {
        iteration++;
        return this;
    }

    public AgentContext markDone() {
        done = true;
        return this;
    }

    public List<ChatMessage> messages() { return Collections.unmodifiableList(messages); }
    public List<Step> steps() { return Collections.unmodifiableList(steps); }
    public Map<String, Object> variables() { return Collections.unmodifiableMap(variables); }
    public int iteration() { return iteration; }
    public boolean isDone() { return done; }
}
