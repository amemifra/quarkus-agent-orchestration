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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration for an agent execution. Builder pattern for fluent
 * construction.
 */
public class AgentConfig {

    private String model;
    private String systemPrompt;
    private double temperature = 0.7;
    private int maxIterations = 10;
    private final List<Tool> tools = new ArrayList<>();

    private AgentConfig() {
    }

    public String model() {
        return model;
    }

    public String systemPrompt() {
        return systemPrompt;
    }

    public double temperature() {
        return temperature;
    }

    public int maxIterations() {
        return maxIterations;
    }

    public List<Tool> tools() {
        return Collections.unmodifiableList(tools);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(AgentConfig existing) {
        Builder b = new Builder()
                .model(existing.model())
                .systemPrompt(existing.systemPrompt())
                .temperature(existing.temperature())
                .maxIterations(existing.maxIterations());
        for (Tool t : existing.tools()) {
            b.addTool(t);
        }
        return b;
    }

    public static class Builder {
        private final AgentConfig config = new AgentConfig();

        public Builder model(String model) {
            config.model = model;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            config.systemPrompt = systemPrompt;
            return this;
        }

        public Builder temperature(double temperature) {
            config.temperature = temperature;
            return this;
        }

        public Builder maxIterations(int maxIterations) {
            config.maxIterations = maxIterations;
            return this;
        }

        public Builder addTool(Tool tool) {
            config.tools.add(tool);
            return this;
        }

        public Builder addTool(String name, String description, java.util.function.Function<String, String> executor) {
            config.tools.add(Tool.of(name, description, executor));
            return this;
        }

        public AgentConfig build() {
            if (config.model == null || config.model.isBlank()) {
                throw new IllegalStateException("Model is required");
            }
            return config;
        }
    }
}
