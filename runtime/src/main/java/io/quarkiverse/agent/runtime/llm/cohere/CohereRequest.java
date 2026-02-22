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
package io.quarkiverse.agent.runtime.llm.cohere;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkiverse.agent.runtime.llm.ChatRequest;

import java.util.List;

/**
 * Cohere v2 Chat request format.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CohereRequest(
        String model,
        List<Message> messages,
        Double temperature,
        @JsonProperty("max_tokens") Integer maxTokens
) {
    public record Message(String role, String content) { }

    public static CohereRequest from(ChatRequest request) {
        var messages = request.messages().stream()
                .map(m -> new Message(
                        "assistant".equals(m.role()) ? "chatbot" : m.role(),
                        m.content()
                ))
                .toList();

        return new CohereRequest(
                request.model(),
                messages,
                request.temperature(),
                request.maxTokens() > 0 ? request.maxTokens() : null
        );
    }
}
