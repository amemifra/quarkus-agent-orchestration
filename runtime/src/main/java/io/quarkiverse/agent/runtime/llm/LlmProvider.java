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
 * All supported LLM providers, classified by API compatibility.
 * OpenAI-compatible providers reuse the same REST client with different base URLs.
 */
public enum LlmProvider {

    // ── OpenAI-Compatible (/v1/chat/completions) ──
    OPENAI("https://api.openai.com", true),
    OLLAMA("http://localhost:11434", true),
    DEEPSEEK("https://api.deepseek.com", true),
    MISTRAL("https://api.mistral.ai", true),
    GROQ("https://api.groq.com/openai", true),
    TOGETHER("https://api.together.xyz", true),
    FIREWORKS("https://api.fireworks.ai/inference", true),
    PERPLEXITY("https://api.perplexity.ai", true),
    DEEPINFRA("https://api.deepinfra.com", true),
    OPENROUTER("https://openrouter.ai/api", true),
    VLLM("http://localhost:8000", true),
    LM_STUDIO("http://localhost:1234", true),
    LOCAL_AI("http://localhost:8080", true),
    HUGGING_FACE("https://api-inference.huggingface.co", true),
    REPLICATE("https://openai-proxy.replicate.com", true),
    SAMBANOVA("https://api.sambanova.ai", true),
    CEREBRAS("https://api.cerebras.ai", true),
    ANYSCALE("https://api.endpoints.anyscale.com", true),

    // ── Dedicated Clients ──
    ANTHROPIC("https://api.anthropic.com", false),
    GOOGLE("https://generativelanguage.googleapis.com", false),
    COHERE("https://api.cohere.com", false);

    private final String defaultBaseUrl;
    private final boolean openAiCompatible;

    LlmProvider(String defaultBaseUrl, boolean openAiCompatible) {
        this.defaultBaseUrl = defaultBaseUrl;
        this.openAiCompatible = openAiCompatible;
    }

    public String defaultBaseUrl() { return defaultBaseUrl; }
    public boolean isOpenAiCompatible() { return openAiCompatible; }
}
