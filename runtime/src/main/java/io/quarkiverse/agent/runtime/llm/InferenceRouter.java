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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.EnumMap;
import java.util.Map;

/**
 * Routes agent requests to the appropriate LLM provider based on inference quality tier.
 * Uses a pluggable {@link InferenceClassifier} for prompt classification.
 *
 * <pre>
 * router.configure(InferenceRoute.of(HIGH, ANTHROPIC, "claude-sonnet-4", "sk-xxx"));
 * router.configure(InferenceRoute.of(MEDIUM, OLLAMA, "llama3"));
 * router.configure(InferenceRoute.of(LOW, GROQ, "mixtral-8x7b", "gsk-xxx"));
 *
 * LlmClient client = router.route(InferenceQuality.HIGH);
 * InferenceQuality tier = router.classify("Write a complex legal contract"); // → HIGH
 * </pre>
 */
@ApplicationScoped
public class InferenceRouter {

    private final Map<InferenceQuality, InferenceRoute> routes = new EnumMap<>(InferenceQuality.class);
    private final Map<InferenceQuality, LlmClient> clientCache = new EnumMap<>(InferenceQuality.class);
    private final LlmClientFactory factory;
    private final LlmClient defaultClient;
    private final InferenceClassifier classifier;

    @Inject
    public InferenceRouter(LlmClientFactory factory, LlmClient defaultClient, InferenceClassifier classifier) {
        this.factory = factory;
        this.defaultClient = defaultClient;
        this.classifier = classifier;
    }

    /**
     * Register a route for a quality tier.
     */
    public InferenceRouter configure(InferenceRoute route) {
        routes.put(route.quality(), route);
        clientCache.remove(route.quality());
        return this;
    }

    /**
     * Get the LlmClient for a specific quality tier.
     * Falls back to the default CDI-injected client if no route is configured.
     */
    public LlmClient route(InferenceQuality quality) {
        if (!routes.containsKey(quality)) {
            return defaultClient;
        }
        return clientCache.computeIfAbsent(quality, q -> {
            var route = routes.get(q);
            return factory.create(route.provider(), route.baseUrl(), route.apiKey());
        });
    }

    /**
     * Classify a task prompt into an inference quality tier.
     * Delegates to the injected {@link InferenceClassifier}.
     */
    public InferenceQuality classify(String prompt) {
        return classifier.classify(prompt);
    }

    /**
     * Route automatically: classify the prompt, then return the appropriate client.
     */
    public LlmClient autoRoute(String prompt) {
        return route(classify(prompt));
    }

    /**
     * Get the configured route for a quality tier, or null.
     */
    public InferenceRoute getRoute(InferenceQuality quality) {
        return routes.get(quality);
    }

    /**
     * Check if a route is configured for a quality tier.
     */
    public boolean hasRoute(InferenceQuality quality) {
        return routes.containsKey(quality);
    }
}
