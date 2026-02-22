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

import io.quarkiverse.agent.runtime.llm.ChatRequest;
import io.quarkiverse.agent.runtime.llm.ChatResponse;
import io.quarkiverse.agent.runtime.llm.LlmClient;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.smallrye.mutiny.Uni;

import java.net.URI;

/**
 * Google Gemini LLM client.
 * API key passed as query parameter per Gemini REST spec.
 */
public class GeminiLlmClient implements LlmClient {

    private final GeminiApi api;
    private final String apiKey;

    public GeminiLlmClient(String baseUrl, String apiKey) {
        this.apiKey = apiKey;
        this.api = QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create(baseUrl))
                .build(GeminiApi.class);
    }

    @Override
    public Uni<ChatResponse> chat(ChatRequest request) {
        return api.generateContent(request.model(), apiKey, GeminiRequest.from(request))
                .map(GeminiResponse::toChatResponse);
    }
}
