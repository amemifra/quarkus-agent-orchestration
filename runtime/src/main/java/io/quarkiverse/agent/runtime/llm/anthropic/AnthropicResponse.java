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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkiverse.agent.runtime.llm.ChatMessage;
import io.quarkiverse.agent.runtime.llm.ChatResponse;

import java.util.List;

/**
 * Anthropic Claude response format.
 * Maps Claude response to unified ChatResponse.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AnthropicResponse(
        String id,
        String model,
        String type,
        List<ContentBlock> content,
        @JsonProperty("stop_reason") String stopReason,
        Usage usage
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentBlock(String type, String text) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
            @JsonProperty("input_tokens") int inputTokens,
            @JsonProperty("output_tokens") int outputTokens
    ) { }

    public ChatResponse toChatResponse() {
        String text = content != null && !content.isEmpty()
                ? content.stream().filter(c -> "text".equals(c.type())).map(ContentBlock::text).reduce("", (a, b) -> a + b)
                : "";

        var choice = new ChatResponse.Choice(0, ChatMessage.assistant(text), stopReason);
        var chatUsage = new ChatResponse.Usage(
                usage != null ? usage.inputTokens() : 0,
                usage != null ? usage.outputTokens() : 0,
                usage != null ? usage.inputTokens() + usage.outputTokens() : 0
        );
        return new ChatResponse(id, model, List.of(choice), chatUsage);
    }
}
