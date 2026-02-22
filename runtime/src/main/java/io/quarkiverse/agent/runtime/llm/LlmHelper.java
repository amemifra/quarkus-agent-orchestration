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

import io.quarkiverse.agent.runtime.core.AgentConfig;
import io.quarkiverse.agent.runtime.core.AgentContext;

import java.time.Duration;

/**
 * Shared helper for LLM chat calls across all engines.
 * Eliminates the duplicated {@code chat()} method that was in 11 engines.
 */
public final class LlmHelper {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(120);

    private LlmHelper() {}

    /**
     * Build a ChatRequest from config + context, call the LLM, and return the text content.
     * Includes timeout and basic error handling.
     *
     * @param config agent configuration (model, temperature)
     * @param ctx    agent context with accumulated messages
     * @param llm    the LLM client to use
     * @return the text content of the LLM response, or empty string on error
     */
    public static String chat(AgentConfig config, AgentContext ctx, LlmClient llm) {
        var builder = ChatRequest.builder()
                .model(config.model())
                .temperature(config.temperature());
        ctx.messages().forEach(builder::addMessage);
        try {
            return llm.chat(builder.build())
                    .ifNoItem().after(DEFAULT_TIMEOUT).fail()
                    .await().indefinitely()
                    .content();
        } catch (Exception e) {
            // Log and return empty rather than crashing the entire agent loop
            System.err.printf("[WARN] LLM call failed: %s%n", e.getMessage());
            return "";
        }
    }
}
