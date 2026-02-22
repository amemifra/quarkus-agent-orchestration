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

import io.quarkiverse.agent.runtime.llm.anthropic.AnthropicLlmClient;
import io.quarkiverse.agent.runtime.llm.cohere.CohereLlmClient;
import io.quarkiverse.agent.runtime.llm.gemini.GeminiLlmClient;
import io.quarkiverse.agent.runtime.llm.openai.OpenAiLlmClient;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Factory that creates the correct {@link LlmClient} for a given provider.
 * OpenAI-compatible providers share the same client; others get dedicated ones.
 */
@ApplicationScoped
public class LlmClientFactory {

    public LlmClient create(LlmProvider provider, String baseUrl, String apiKey) {
        String url = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : provider.defaultBaseUrl();

        if (provider.isOpenAiCompatible()) {
            return OpenAiLlmClient.create(url, apiKey);
        }

        return switch (provider) {
            case ANTHROPIC -> new AnthropicLlmClient(url, apiKey);
            case GOOGLE -> new GeminiLlmClient(url, apiKey);
            case COHERE -> new CohereLlmClient(url, apiKey);
            default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
        };
    }

    public LlmClient create(LlmProvider provider, String apiKey) {
        return create(provider, provider.defaultBaseUrl(), apiKey);
    }
}
