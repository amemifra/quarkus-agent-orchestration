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
 * Inference quality tier — determines which LLM provider to route to.
 * <ul>
 *   <li>HIGH: Most capable model (Claude Opus, GPT-4o, Llama 70B)</li>
 *   <li>MEDIUM: Balanced cost/quality (Mistral Medium, Claude Sonnet, Llama 8B)</li>
 *   <li>LOW: Fast and cheap (tinyllama, Mistral Small, Groq)</li>
 * </ul>
 */
public enum InferenceQuality {
    HIGH,
    MEDIUM,
    LOW
}
