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

/**
 * Route configuration: maps an {@link InferenceQuality} tier to a specific provider + model.
 *
 * <pre>
 * var route = InferenceRoute.of(InferenceQuality.HIGH, LlmProvider.ANTHROPIC, "claude-sonnet-4");
 * </pre>
 */
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record InferenceRoute(
        InferenceQuality quality,
        LlmProvider provider,
        String model,
        String baseUrl,
        String apiKey
) {
    public static InferenceRoute of(InferenceQuality quality, LlmProvider provider, String model) {
        return new InferenceRoute(quality, provider, model, provider.defaultBaseUrl(), null);
    }

    public static InferenceRoute of(InferenceQuality quality, LlmProvider provider, String model, String apiKey) {
        return new InferenceRoute(quality, provider, model, provider.defaultBaseUrl(), apiKey);
    }

    public static InferenceRoute of(InferenceQuality quality, LlmProvider provider, String model, String baseUrl, String apiKey) {
        return new InferenceRoute(quality, provider, model, baseUrl, apiKey);
    }
}
