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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkiverse.agent.runtime.llm.ChatMessage;
import io.quarkiverse.agent.runtime.llm.ChatResponse;

import java.util.List;

/**
 * Cohere v2 Chat response format.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CohereResponse(
        String id,
        @JsonProperty("finish_reason") String finishReason,
        Message message,
        Usage usage
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String role, List<ContentBlock> content) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentBlock(String type, String text) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
            @JsonProperty("billed_units") BilledUnits billedUnits,
            Tokens tokens
    ) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BilledUnits(@JsonProperty("input_tokens") int inputTokens,
                               @JsonProperty("output_tokens") int outputTokens) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Tokens(@JsonProperty("input_tokens") int inputTokens,
                          @JsonProperty("output_tokens") int outputTokens) { }

    public ChatResponse toChatResponse() {
        String text = "";
        if (message != null && message.content() != null) {
            text = message.content().stream()
                    .filter(c -> "text".equals(c.type()))
                    .map(ContentBlock::text)
                    .reduce("", (a, b) -> a + b);
        }

        var choice = new ChatResponse.Choice(0, ChatMessage.assistant(text), finishReason);
        int inTokens = usage != null && usage.tokens() != null ? usage.tokens().inputTokens() : 0;
        int outTokens = usage != null && usage.tokens() != null ? usage.tokens().outputTokens() : 0;
        var chatUsage = new ChatResponse.Usage(inTokens, outTokens, inTokens + outTokens);
        return new ChatResponse(id, "cohere", List.of(choice), chatUsage);
    }
}
