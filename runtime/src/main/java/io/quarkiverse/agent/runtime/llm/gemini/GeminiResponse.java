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
package io.quarkiverse.agent.runtime.llm.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkiverse.agent.runtime.llm.ChatMessage;
import io.quarkiverse.agent.runtime.llm.ChatResponse;

import java.util.List;

/**
 * Google Gemini response format.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiResponse(
        List<Candidate> candidates,
        UsageMetadata usageMetadata
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(Content content, String finishReason) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(List<Part> parts, String role) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Part(String text) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UsageMetadata(int promptTokenCount, int candidatesTokenCount, int totalTokenCount) { }

    public ChatResponse toChatResponse() {
        String text = "";
        if (candidates != null && !candidates.isEmpty()) {
            var c = candidates.get(0);
            if (c.content() != null && c.content().parts() != null && !c.content().parts().isEmpty()) {
                text = c.content().parts().stream().map(Part::text).reduce("", (a, b) -> a + b);
            }
        }

        var choice = new ChatResponse.Choice(0, ChatMessage.assistant(text),
                candidates != null && !candidates.isEmpty() ? candidates.get(0).finishReason() : "STOP");
        var usage = usageMetadata != null
                ? new ChatResponse.Usage(usageMetadata.promptTokenCount(), usageMetadata.candidatesTokenCount(), usageMetadata.totalTokenCount())
                : new ChatResponse.Usage(0, 0, 0);
        return new ChatResponse("gemini", "gemini", List.of(choice), usage);
    }
}
