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

import io.smallrye.mutiny.Uni;

/**
 * Provider-agnostic LLM client interface.
 * Implementations are selected at build-time based on configuration.
 */
public interface LlmClient {

    /**
     * Send a chat completion request to the configured LLM provider.
     *
     * @param request the chat request
     * @return a Uni with the chat response
     */
    Uni<ChatResponse> chat(ChatRequest request);
}
