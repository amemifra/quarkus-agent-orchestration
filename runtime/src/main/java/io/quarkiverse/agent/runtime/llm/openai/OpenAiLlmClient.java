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
package io.quarkiverse.agent.runtime.llm.openai;

import io.quarkiverse.agent.runtime.llm.ChatRequest;
import io.quarkiverse.agent.runtime.llm.ChatResponse;
import io.quarkiverse.agent.runtime.llm.LlmClient;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.util.Optional;

/**
 * OpenAI-compatible implementation of {@link LlmClient}.
 * Supports all providers using the /v1/chat/completions endpoint.
 * API key is propagated via Authorization: Bearer header when provided.
 */
@ApplicationScoped
public class OpenAiLlmClient implements LlmClient {

    private final OpenAiCompatibleApi api;
    private final String apiKey;

    @Inject
    public OpenAiLlmClient(
            @ConfigProperty(name = "quarkus.agent.base-url", defaultValue = "http://localhost:11434") String baseUrl,
            @ConfigProperty(name = "quarkus.agent.api-key") Optional<String> apiKey) {
        this.apiKey = apiKey.orElse("");
        this.api = QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create(baseUrl))
                .build(OpenAiCompatibleApi.class);
    }

    /** Programmatic constructor for factory-created instances (no API key). */
    public OpenAiLlmClient(String baseUrl) {
        this.apiKey = "";
        this.api = QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create(baseUrl))
                .build(OpenAiCompatibleApi.class);
    }

    /** Factory method for factory-created instances with API key. */
    public static OpenAiLlmClient create(String baseUrl, String apiKey) {
        OpenAiLlmClient client = new OpenAiLlmClient(baseUrl);
        // Use reflection-free approach: store apiKey separately
        return new OpenAiLlmClient(baseUrl, apiKey);
    }

    /** Private constructor for factory method. */
    private OpenAiLlmClient(String baseUrl, String apiKey) {
        this.apiKey = apiKey != null ? apiKey : "";
        this.api = QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create(baseUrl))
                .build(OpenAiCompatibleApi.class);
    }

    @Override
    public Uni<ChatResponse> chat(ChatRequest request) {
        if (apiKey != null && !apiKey.isBlank()) {
            return api.chatCompletionWithAuth(request, "Bearer " + apiKey);
        }
        return api.chatCompletion(request);
    }
}
