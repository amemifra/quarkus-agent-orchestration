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
package io.quarkiverse.agent.runtime.tool;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Request to execute a command in a Docker sandbox.
 * Uses builder pattern for fluent construction.
 */
public class ToolRequest {

    private String image;
    private String[] command;
    private Duration timeout;
    private Map<String, String> environment;

    private ToolRequest() {
    }

    public String image() {
        return image;
    }

    public String[] command() {
        return command;
    }

    public Duration timeout() {
        return timeout;
    }

    public Map<String, String> environment() {
        return environment;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ToolRequest request = new ToolRequest();

        private Builder() {
            request.timeout = Duration.ofSeconds(30);
            request.environment = new HashMap<>();
        }

        public Builder image(String image) {
            request.image = image;
            return this;
        }

        public Builder command(String... command) {
            request.command = command;
            return this;
        }

        public Builder timeout(Duration timeout) {
            request.timeout = timeout;
            return this;
        }

        public Builder environment(String key, String value) {
            request.environment.put(key, value);
            return this;
        }

        public ToolRequest build() {
            if (request.image == null || request.image.isBlank()) {
                throw new IllegalStateException("Docker image is required");
            }
            if (request.command == null || request.command.length == 0) {
                throw new IllegalStateException("Command is required");
            }
            return request;
        }
    }
}
