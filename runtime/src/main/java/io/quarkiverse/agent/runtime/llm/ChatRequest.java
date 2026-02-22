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
package io.quarkiverse.agent.runtime.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO for chat completions. Compatible with OpenAI API format.
 * Uses builder pattern for fluent construction.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRequest {

    @JsonProperty("model")
    private String model;

    @JsonProperty("messages")
    private List<ChatMessage> messages;

    @JsonProperty("temperature")
    private Double temperature;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    private ChatRequest() {
    }

    public String model() {
        return model;
    }

    public List<ChatMessage> messages() {
        return messages;
    }

    public Double temperature() {
        return temperature;
    }

    public Integer maxTokens() {
        return maxTokens;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ChatRequest request = new ChatRequest();

        private Builder() {
            request.messages = new ArrayList<>();
        }

        public Builder model(String model) {
            request.model = model;
            return this;
        }

        public Builder addMessage(ChatMessage message) {
            request.messages.add(message);
            return this;
        }

        public Builder systemMessage(String content) {
            return addMessage(ChatMessage.system(content));
        }

        public Builder userMessage(String content) {
            return addMessage(ChatMessage.user(content));
        }

        public Builder temperature(double temperature) {
            request.temperature = temperature;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            request.maxTokens = maxTokens;
            return this;
        }

        public ChatRequest build() {
            if (request.model == null || request.model.isBlank()) {
                throw new IllegalStateException("Model is required");
            }
            if (request.messages.isEmpty()) {
                throw new IllegalStateException("At least one message is required");
            }
            return request;
        }
    }
}
