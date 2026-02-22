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
package io.quarkiverse.agent.runtime.llm.anthropic;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkiverse.agent.runtime.llm.ChatMessage;
import io.quarkiverse.agent.runtime.llm.ChatRequest;

import java.util.List;

/**
 * Anthropic Claude API request format.
 * Maps from unified ChatRequest to Claude's /v1/messages format.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnthropicRequest(
        String model,
        @JsonProperty("max_tokens") int maxTokens,
        String system,
        List<Message> messages,
        Double temperature
) {
    public record Message(String role, String content) { }

    public static AnthropicRequest from(ChatRequest request) {
        String systemPrompt = null;
        var messages = new java.util.ArrayList<Message>();

        for (var msg : request.messages()) {
            if ("system".equals(msg.role())) {
                systemPrompt = msg.content();
            } else {
                messages.add(new Message(msg.role(), msg.content()));
            }
        }

        return new AnthropicRequest(
                request.model(),
                request.maxTokens() > 0 ? request.maxTokens() : 4096,
                systemPrompt,
                messages,
                request.temperature()
        );
    }
}
