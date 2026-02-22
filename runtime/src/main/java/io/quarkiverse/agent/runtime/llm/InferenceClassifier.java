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

/**
 * Strategy interface for classifying prompts into inference quality tiers.
 * Implement this interface and register as a CDI bean to override the default heuristic.
 *
 * <pre>
 * &#64;ApplicationScoped
 * &#64;Alternative
 * &#64;Priority(1)
 * public class MyClassifier implements InferenceClassifier {
 *     &#64;Override
 *     public InferenceQuality classify(String prompt) {
 *         // custom logic, e.g. LLM-based classification
 *     }
 * }
 * </pre>
 */
public interface InferenceClassifier {

    /**
     * Classify a prompt into an inference quality tier.
     *
     * @param prompt the user prompt to classify
     * @return the quality tier (HIGH, MEDIUM, LOW)
     */
    InferenceQuality classify(String prompt);
}
