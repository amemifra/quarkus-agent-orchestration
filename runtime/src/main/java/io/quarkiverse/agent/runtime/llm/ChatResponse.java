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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

/**
 * Response DTO for chat completions. Compatible with OpenAI API format.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@RegisterForReflection
public record ChatResponse(
        @JsonProperty("id") String id,
        @JsonProperty("model") String model,
        @JsonProperty("choices") List<Choice> choices,
        @JsonProperty("usage") Usage usage
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(
            @JsonProperty("index") int index,
            @JsonProperty("message") ChatMessage message,
            @JsonProperty("finish_reason") String finishReason
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
            @JsonProperty("prompt_tokens") int promptTokens,
            @JsonProperty("completion_tokens") int completionTokens,
            @JsonProperty("total_tokens") int totalTokens
    ) {
    }

    /**
     * Convenience: get the text content of the first choice.
     */
    public String content() {
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        return choices.getFirst().message().content();
    }
}
