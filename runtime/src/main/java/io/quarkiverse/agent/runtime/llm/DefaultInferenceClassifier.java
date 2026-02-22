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

/**
 * Default keyword-based prompt classifier.
 * Users can override this by providing an {@code @Alternative} CDI bean
 * implementing {@link InferenceClassifier}.
 */
@ApplicationScoped
public class DefaultInferenceClassifier implements InferenceClassifier {

    @Override
    public InferenceQuality classify(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return InferenceQuality.LOW;
        }
        int words = prompt.split("\\s+").length;
        boolean hasComplexKeywords = prompt.matches(
                "(?i).*(analy[sz]e|research|compare|synthesize|complex|detailed|comprehensive|legal|medical|scientific|architect).*");

        if (hasComplexKeywords || words > 100) {
            return InferenceQuality.HIGH;
        } else if (words > 30) {
            return InferenceQuality.MEDIUM;
        }
        return InferenceQuality.LOW;
    }
}
