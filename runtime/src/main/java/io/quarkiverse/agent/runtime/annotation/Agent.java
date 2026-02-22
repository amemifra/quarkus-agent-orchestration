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
package io.quarkiverse.agent.runtime.annotation;

import io.quarkiverse.agent.runtime.llm.InferenceQuality;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a class as an AI agent with a specific architecture.
 * The Quarkus extension processes this at build time via Jandex.
 *
 * <pre>
 * &#64;Agent(provider = "ollama", model = "llama3",
 *        architecture = Architecture.REFLECTION,
 *        quality = InferenceQuality.HIGH)
 * public class WriterAgent { ... }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Agent {
    String provider() default "ollama";
    String model();
    Architecture architecture();
    int maxIterations() default 10;
    double temperature() default 0.7;
    String systemPrompt() default "";
    InferenceQuality quality() default InferenceQuality.MEDIUM;
}
