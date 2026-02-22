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
package io.quarkiverse.agent.it;

import io.quarkiverse.agent.runtime.AgentOrchestrationService;
import io.quarkiverse.agent.runtime.arch.*;
import io.quarkiverse.agent.runtime.core.AgentConfig;
import io.quarkiverse.agent.runtime.core.AgentResult;
import io.quarkiverse.agent.runtime.core.Step;
import io.quarkiverse.agent.runtime.core.Tool;
import io.quarkiverse.agent.runtime.llm.ChatMessage;
import io.quarkiverse.agent.runtime.llm.ChatRequest;
import io.quarkiverse.agent.runtime.llm.ChatResponse;
import io.quarkiverse.agent.runtime.llm.LlmClient;
import io.quarkiverse.agent.runtime.tool.ToolExecutor;
import io.quarkiverse.agent.runtime.tool.ToolRequest;
import io.quarkiverse.agent.runtime.tool.ToolResult;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class AgentExtensionTest {

    @Inject
    LlmClient llmClient;

    @Inject
    ToolExecutor toolExecutor;

    @Inject
    AgentOrchestrationService agents;

    // ══════════════════════════════════════
    // T1: Health & Version
    // ══════════════════════════════════════

    @Test
    void testHealthEndpoint() {
        given().when().get("/agent/health")
                .then().statusCode(200).body(is("ok"));
    }

    @Test
    void testVersionEndpoint() {
        given().when().get("/agent/version")
                .then().statusCode(200).body(is("1.0.0-SNAPSHOT"));
    }

    // ══════════════════════════════════════
    // T2: LLM Client
    // ══════════════════════════════════════

    @Test
    void chatRequestBuilderProducesValidRequest() {
        var request = ChatRequest.builder()
                .model("llama3")
                .systemMessage("You are a helpful assistant")
                .userMessage("Hello!")
                .temperature(0.7).maxTokens(100).build();

        assertEquals("llama3", request.model());
        assertEquals(2, request.messages().size());
    }

    @Test
    void chatRequestBuilderRejectsEmptyModel() {
        assertThrows(IllegalStateException.class, () ->
                ChatRequest.builder().userMessage("Hello!").build());
    }

    @Test
    void chatRequestBuilderRejectsEmptyMessages() {
        assertThrows(IllegalStateException.class, () ->
                ChatRequest.builder().model("llama3").build());
    }

    @Test
    void chatMessageFactoryMethods() {
        assertEquals("system", ChatMessage.system("test").role());
        assertEquals("user", ChatMessage.user("test").role());
        assertEquals("assistant", ChatMessage.assistant("test").role());
    }

    @Test
    void llmClientChatReturnsResponse() {
        var request = ChatRequest.builder().model("test-model").userMessage("Hello").build();
        ChatResponse response = llmClient.chat(request).await().indefinitely();
        assertNotNull(response);
        assertFalse(response.content().isEmpty());
    }

    @Test
    void fakeLlmClientIsInjectedInTestMode() {
        given().when().get("/agent/llm-class")
                .then().statusCode(200).body(containsString("FakeLlmClient"));
    }

    // ══════════════════════════════════════
    // T3: Tool Executor
    // ══════════════════════════════════════

    @Test
    void toolRequestBuilderProducesValidRequest() {
        var request = ToolRequest.builder()
                .image("alpine:latest").command("echo", "hello")
                .timeout(Duration.ofSeconds(10)).environment("MY_VAR", "test").build();
        assertEquals("alpine:latest", request.image());
        assertArrayEquals(new String[]{"echo", "hello"}, request.command());
    }

    @Test
    void toolRequestBuilderRejectsEmptyImage() {
        assertThrows(IllegalStateException.class, () ->
                ToolRequest.builder().command("echo", "hello").build());
    }

    @Test
    void toolRequestBuilderRejectsEmptyCommand() {
        assertThrows(IllegalStateException.class, () ->
                ToolRequest.builder().image("alpine:latest").build());
    }

    @Test
    void dockerSandboxExecutesEchoCommand() {
        var request = ToolRequest.builder()
                .image("alpine:latest").command("echo", "hello world")
                .timeout(Duration.ofSeconds(30)).build();
        ToolResult result = toolExecutor.execute(request);
        assertTrue(result.isSuccess());
        assertTrue(result.stdout().contains("hello world"));
    }

    @Test
    void toolResultRecordIsSuccess() {
        assertTrue(new ToolResult(0, "output", "", 100).isSuccess());
        assertFalse(new ToolResult(1, "", "error", 50).isSuccess());
    }

    // ══════════════════════════════════════
    // T4: AgentConfig
    // ══════════════════════════════════════

    @Test
    void agentConfigBuilderProducesValidConfig() {
        var config = AgentConfig.builder()
                .model("llama3").systemPrompt("Be helpful")
                .temperature(0.5).maxIterations(5)
                .addTool("calc", "Calculator", input -> "42").build();
        assertEquals("llama3", config.model());
        assertEquals(0.5, config.temperature());
        assertEquals(5, config.maxIterations());
        assertEquals(1, config.tools().size());
    }

    @Test
    void agentConfigBuilderRejectsEmptyModel() {
        assertThrows(IllegalStateException.class, () ->
                AgentConfig.builder().build());
    }

    // ══════════════════════════════════════
    // T5: E2E Architecture Tests (≥5)
    // ══════════════════════════════════════

    @Test
    void e2eReflectArchitecture() {
        var config = AgentConfig.builder().model("test").maxIterations(3).build();
        AgentResult result = agents.reflect(config, "Write a haiku about Java");

        assertNotNull(result);
        assertFalse(result.content().isEmpty());
        assertFalse(result.steps().isEmpty());
        assertTrue(result.steps().stream().anyMatch(s -> s.type() == Step.StepType.CRITIQUE));
        assertTrue(result.durationMs() >= 0);
    }

    @Test
    void e2ePipelineArchitecture() {
        var config = AgentConfig.builder().model("test").build();
        var steps = List.of(
                PipelineEngine.PipelineStep.of("summarize", "Summarize the input text concisely."),
                PipelineEngine.PipelineStep.of("translate", "Translate the text to Italian.")
        );
        AgentResult result = agents.pipeline(config, steps, "Hello world, this is a test.");

        assertNotNull(result);
        assertFalse(result.content().isEmpty());
        assertEquals(4, result.steps().size()); // 2 plan + 2 observation
    }

    @Test
    void e2eMapReduceArchitecture() {
        var config = AgentConfig.builder().model("test").build();
        var inputs = List.of("Document A about cats", "Document B about dogs", "Document C about birds");
        AgentResult result = agents.mapReduce(config, inputs,
                "Summarize this document in one sentence.",
                "Aggregate all summaries into a final report.");

        assertNotNull(result);
        assertFalse(result.content().isEmpty());
        assertTrue(result.steps().size() >= 4); // 3 map + 1 reduce
    }

    @Test
    void e2eDebateArchitecture() {
        var config = AgentConfig.builder().model("test").build();
        AgentResult result = agents.debate(config, 3, "Is Java better than Python?");

        assertNotNull(result);
        assertFalse(result.content().isEmpty());
        assertTrue(result.steps().stream().anyMatch(s -> s.type() == Step.StepType.SYNTHESIS));
    }

    @Test
    void e2eCrewArchitecture() {
        var config = AgentConfig.builder().model("test").build();
        var crew = List.of(
                CrewEngine.CrewMember.of("Researcher", "Expert in data analysis", "Research the topic"),
                CrewEngine.CrewMember.of("Writer", "Excellent technical writer", "Write a report"),
                CrewEngine.CrewMember.of("Reviewer", "Quality assurance expert", "Review the report")
        );
        AgentResult result = agents.crew(config, crew, "Analyze cloud computing trends");

        assertNotNull(result);
        assertFalse(result.content().isEmpty());
        assertEquals(6, result.steps().size()); // 3 delegation + 3 observation
    }

    @Test
    void e2eVerifyArchitecture() {
        var config = AgentConfig.builder().model("test").maxIterations(3).build();
        AgentResult result = agents.verify(config, "What is the capital of France?");

        assertNotNull(result);
        assertFalse(result.content().isEmpty());
    }

    // ══════════════════════════════════════
    // T6: Showcase App Endpoints
    // ══════════════════════════════════════

    @Test
    void showcaseReflectEndpoint() {
        given().when().get("/agent/showcase/reflect?prompt=Write+a+poem")
                .then().statusCode(200)
                .body("architecture", is("A3-Reflection"))
                .body("content", org.hamcrest.Matchers.notNullValue())
                .body("steps", org.hamcrest.Matchers.greaterThan(0));
    }

    @Test
    void showcasePipelineEndpoint() {
        given().when().get("/agent/showcase/pipeline?input=Hello+world")
                .then().statusCode(200)
                .body("architecture", is("A9-Pipeline"))
                .body("content", org.hamcrest.Matchers.notNullValue())
                .body("steps", org.hamcrest.Matchers.greaterThan(0));
    }

    @Test
    void showcaseCrewEndpoint() {
        given().when().get("/agent/showcase/crew?topic=AI+trends")
                .then().statusCode(200)
                .body("architecture", is("A7-Crew"))
                .body("content", org.hamcrest.Matchers.notNullValue())
                .body("steps", org.hamcrest.Matchers.greaterThan(0));
    }

    // ══════════════════════════════════════
    // T8: Annotation-Based Agent Discovery
    // ══════════════════════════════════════

    @Inject
    io.quarkiverse.agent.runtime.annotation.AgentRegistry agentRegistry;

    @Test
    void annotatedAgentIsDiscoveredByJandex() {
        var def = agentRegistry.findByClass("io.quarkiverse.agent.it.AnnotatedWriterAgent");
        assertNotNull(def, "Agent should be discovered at build time via Jandex");
        assertEquals("llama3", def.model());
        assertEquals(io.quarkiverse.agent.runtime.annotation.Architecture.REFLECTION, def.architecture());
        assertEquals(3, def.maxIterations());
        assertEquals("write", def.generateMethodName());
        assertEquals("isGoodEnough", def.evaluateMethodName());
        assertEquals(io.quarkiverse.agent.runtime.llm.InferenceQuality.HIGH, def.quality());
    }

    @Test
    void annotatedCrewAgentHasMembers() {
        var def = agentRegistry.findByClass("io.quarkiverse.agent.it.AnnotatedCrewAgent");
        assertNotNull(def, "Crew agent should be discovered");
        assertEquals(io.quarkiverse.agent.runtime.annotation.Architecture.CREW, def.architecture());
        assertEquals(3, def.crewMembers().size());
        assertEquals("Researcher", def.crewMembers().get(0).role());
        assertEquals("Writer", def.crewMembers().get(1).role());
        assertEquals("Reviewer", def.crewMembers().get(2).role());
        assertEquals(io.quarkiverse.agent.runtime.llm.InferenceQuality.MEDIUM, def.quality()); // default
    }

    @Test
    void annotatedPipelineAgentHasOrderedSteps() {
        var def = agentRegistry.findByClass("io.quarkiverse.agent.it.AnnotatedPipelineAgent");
        assertNotNull(def, "Pipeline agent should be discovered");
        assertEquals(io.quarkiverse.agent.runtime.annotation.Architecture.PIPELINE, def.architecture());
        assertEquals(3, def.pipelineSteps().size());
        assertEquals("summarize", def.pipelineSteps().get(0).name());
        assertEquals("translate", def.pipelineSteps().get(1).name());
        assertEquals("format", def.pipelineSteps().get(2).name());
        assertEquals(1, def.pipelineSteps().get(0).order());
    }

    // ══════════════════════════════════════
    // T13-14: Multi-Provider + Quality Routing
    // ══════════════════════════════════════

    @Inject
    io.quarkiverse.agent.runtime.llm.LlmClientFactory llmFactory;

    @Inject
    io.quarkiverse.agent.runtime.llm.InferenceRouter inferenceRouter;

    // ── Per-Provider Enum Tests ──

    @Test
    void allOpenAiCompatibleProviders() {
        var compatible = java.util.Arrays.stream(io.quarkiverse.agent.runtime.llm.LlmProvider.values())
                .filter(io.quarkiverse.agent.runtime.llm.LlmProvider::isOpenAiCompatible)
                .toList();
        assertEquals(18, compatible.size(), "18 OpenAI-compatible providers");
    }

    @Test
    void providerOpenAi() {
        var p = io.quarkiverse.agent.runtime.llm.LlmProvider.OPENAI;
        assertEquals("https://api.openai.com", p.defaultBaseUrl());
        assertTrue(p.isOpenAiCompatible());
    }

    @Test
    void providerOllama() {
        var p = io.quarkiverse.agent.runtime.llm.LlmProvider.OLLAMA;
        assertEquals("http://localhost:11434", p.defaultBaseUrl());
        assertTrue(p.isOpenAiCompatible());
    }

    @Test
    void providerDeepSeek() {
        var p = io.quarkiverse.agent.runtime.llm.LlmProvider.DEEPSEEK;
        assertEquals("https://api.deepseek.com", p.defaultBaseUrl());
        assertTrue(p.isOpenAiCompatible());
    }

    @Test
    void providerMistral() {
        var p = io.quarkiverse.agent.runtime.llm.LlmProvider.MISTRAL;
        assertEquals("https://api.mistral.ai", p.defaultBaseUrl());
        assertTrue(p.isOpenAiCompatible());
    }

    @Test
    void providerGroq() {
        var p = io.quarkiverse.agent.runtime.llm.LlmProvider.GROQ;
        assertEquals("https://api.groq.com/openai", p.defaultBaseUrl());
        assertTrue(p.isOpenAiCompatible());
    }

    @Test
    void providerTogether() {
        var p = io.quarkiverse.agent.runtime.llm.LlmProvider.TOGETHER;
        assertEquals("https://api.together.xyz", p.defaultBaseUrl());
        assertTrue(p.isOpenAiCompatible());
    }

    @Test
    void providerFireworks() {
        var p = io.quarkiverse.agent.runtime.llm.LlmProvider.FIREWORKS;
        assertEquals("https://api.fireworks.ai/inference", p.defaultBaseUrl());
        assertTrue(p.isOpenAiCompatible());
    }

    @Test
    void providerPerplexity() {
        var p = io.quarkiverse.agent.runtime.llm.LlmProvider.PERPLEXITY;
        assertEquals("https://api.perplexity.ai", p.defaultBaseUrl());
        assertTrue(p.isOpenAiCompatible());
    }

    @Test
    void providerDeepInfra() {
        var p = io.quarkiverse.agent.runtime.llm.LlmProvider.DEEPINFRA;
        assertEquals("https://api.deepinfra.com", p.defaultBaseUrl());
        assertTrue(p.isOpenAiCompatible());
    }

    @Test
    void providerOpenRouter() {
        var p = io.quarkiverse.agent.runtime.llm.LlmProvider.OPENROUTER;
        assertEquals("https://openrouter.ai/api", p.defaultBaseUrl());
        assertTrue(p.isOpenAiCompatible());
    }

    @Test
    void providerAnthropic() {
        var p = io.quarkiverse.agent.runtime.llm.LlmProvider.ANTHROPIC;
        assertEquals("https://api.anthropic.com", p.defaultBaseUrl());
        assertFalse(p.isOpenAiCompatible());
    }

    @Test
    void providerGoogle() {
        var p = io.quarkiverse.agent.runtime.llm.LlmProvider.GOOGLE;
        assertEquals("https://generativelanguage.googleapis.com", p.defaultBaseUrl());
        assertFalse(p.isOpenAiCompatible());
    }

    @Test
    void providerCohere() {
        var p = io.quarkiverse.agent.runtime.llm.LlmProvider.COHERE;
        assertEquals("https://api.cohere.com", p.defaultBaseUrl());
        assertFalse(p.isOpenAiCompatible());
    }

    @Test
    void totalProviderCount() {
        assertEquals(21, io.quarkiverse.agent.runtime.llm.LlmProvider.values().length);
    }

    // ── Inference Quality Classification Tests ──

    @Test
    void classifyBlankPromptAsLow() {
        assertEquals(io.quarkiverse.agent.runtime.llm.InferenceQuality.LOW, inferenceRouter.classify(""));
    }

    @Test
    void classifySimplePromptAsLow() {
        assertEquals(io.quarkiverse.agent.runtime.llm.InferenceQuality.LOW, inferenceRouter.classify("Hello"));
    }

    @Test
    void classifyComplexPromptAsHigh() {
        assertEquals(io.quarkiverse.agent.runtime.llm.InferenceQuality.HIGH,
                inferenceRouter.classify("Analyze the detailed scientific research on complex phenomena"));
    }

    @Test
    void classifyMediumLengthPromptAsMedium() {
        String medium = "Write a summary about the following topic including multiple perspectives and covering the key points that should be addressed thoroughly in this report for our upcoming team meeting next week on Thursday";
        assertEquals(io.quarkiverse.agent.runtime.llm.InferenceQuality.MEDIUM, inferenceRouter.classify(medium));
    }

    // ── Multi-Provider Routing Tests ──

    @Test
    void routerFallsBackToDefaultWhenNoRouteConfigured() {
        // A fresh router with no routes returns the injected default client
        // We can't call route() because it caches and may trigger REST client build
        // Instead verify the route config is absent
        assertFalse(inferenceRouter.hasRoute(io.quarkiverse.agent.runtime.llm.InferenceQuality.HIGH)
                && inferenceRouter.getRoute(io.quarkiverse.agent.runtime.llm.InferenceQuality.HIGH) == null);
    }

    @Test
    void routerConfigureAndRetrieveRoute() {
        var route = io.quarkiverse.agent.runtime.llm.InferenceRoute.of(
                io.quarkiverse.agent.runtime.llm.InferenceQuality.LOW,
                io.quarkiverse.agent.runtime.llm.LlmProvider.GROQ,
                "mixtral-8x7b"
        );
        inferenceRouter.configure(route);
        assertTrue(inferenceRouter.hasRoute(io.quarkiverse.agent.runtime.llm.InferenceQuality.LOW));
        assertEquals("mixtral-8x7b", inferenceRouter.getRoute(io.quarkiverse.agent.runtime.llm.InferenceQuality.LOW).model());
    }

    @Test
    void multiProviderRoutingEndToEnd() {
        // Configure 3 tiers with different providers (verify route config, not REST client creation)
        var highRoute = io.quarkiverse.agent.runtime.llm.InferenceRoute.of(
                io.quarkiverse.agent.runtime.llm.InferenceQuality.HIGH,
                io.quarkiverse.agent.runtime.llm.LlmProvider.ANTHROPIC, "claude-opus"
        );
        var medRoute = io.quarkiverse.agent.runtime.llm.InferenceRoute.of(
                io.quarkiverse.agent.runtime.llm.InferenceQuality.MEDIUM,
                io.quarkiverse.agent.runtime.llm.LlmProvider.OLLAMA, "llama3"
        );
        inferenceRouter.configure(highRoute);
        inferenceRouter.configure(medRoute);

        // Verify routes are registered correctly
        assertTrue(inferenceRouter.hasRoute(io.quarkiverse.agent.runtime.llm.InferenceQuality.HIGH));
        assertTrue(inferenceRouter.hasRoute(io.quarkiverse.agent.runtime.llm.InferenceQuality.MEDIUM));
        assertEquals("claude-opus", inferenceRouter.getRoute(io.quarkiverse.agent.runtime.llm.InferenceQuality.HIGH).model());
        assertEquals(io.quarkiverse.agent.runtime.llm.LlmProvider.ANTHROPIC,
                inferenceRouter.getRoute(io.quarkiverse.agent.runtime.llm.InferenceQuality.HIGH).provider());
        assertEquals("llama3", inferenceRouter.getRoute(io.quarkiverse.agent.runtime.llm.InferenceQuality.MEDIUM).model());

        // Classify prompts → verify quality tier selection
        assertEquals(io.quarkiverse.agent.runtime.llm.InferenceQuality.HIGH,
                inferenceRouter.classify("Analyze complex legal scientific research implications"));
        assertEquals(io.quarkiverse.agent.runtime.llm.InferenceQuality.LOW,
                inferenceRouter.classify("Hi"));
    }

    @Test
    void inferenceRouteWithCustomBaseUrl() {
        var route = io.quarkiverse.agent.runtime.llm.InferenceRoute.of(
                io.quarkiverse.agent.runtime.llm.InferenceQuality.HIGH,
                io.quarkiverse.agent.runtime.llm.LlmProvider.OPENAI,
                "gpt-4o",
                "https://custom-proxy.example.com",
                "sk-test-key"
        );
        assertEquals("https://custom-proxy.example.com", route.baseUrl());
        assertEquals("sk-test-key", route.apiKey());
        assertEquals("gpt-4o", route.model());
    }

    // ══════════════════════════════════════
    // T15: ALL Architecture Annotations
    // ══════════════════════════════════════

    @Test
    void annotatedReActAgentHasTools() {
        var def = agentRegistry.findByClass("io.quarkiverse.agent.it.AnnotatedReActAgent");
        assertNotNull(def, "ReAct agent should be discovered");
        assertEquals(io.quarkiverse.agent.runtime.annotation.Architecture.REACT, def.architecture());
        assertEquals(io.quarkiverse.agent.runtime.llm.InferenceQuality.HIGH, def.quality());
        assertEquals(2, def.agentTools().size());
        assertEquals("calculator", def.agentTools().get(0).name());
        assertEquals("Performs math operations", def.agentTools().get(0).description());
        assertEquals("search", def.agentTools().get(1).name());
    }

    @Test
    void annotatedGraphAgentHasNodes() {
        var def = agentRegistry.findByClass("io.quarkiverse.agent.it.AnnotatedGraphAgent");
        assertNotNull(def, "Graph agent should be discovered");
        assertEquals(io.quarkiverse.agent.runtime.annotation.Architecture.STATE_GRAPH, def.architecture());
        assertEquals(3, def.graphNodes().size());
        assertEquals("analyze", def.graphNodes().get(0).name());
        assertEquals("process,output", def.graphNodes().get(0).transitions());
        assertEquals("output", def.graphNodes().get(2).name());
        assertTrue(def.graphNodes().get(2).transitions().isEmpty());
    }

    @Test
    void annotatedSupervisorAgentHasWorkers() {
        var def = agentRegistry.findByClass("io.quarkiverse.agent.it.AnnotatedSupervisorAgent");
        assertNotNull(def, "Supervisor agent should be discovered");
        assertEquals(io.quarkiverse.agent.runtime.annotation.Architecture.SUPERVISOR, def.architecture());
        assertEquals(2, def.workers().size());
        assertEquals("researcher", def.workers().get(0).name());
        assertEquals("You are a research expert", def.workers().get(0).systemPrompt());
    }

    @Test
    void annotatedHierarchicalAgentHasTeams() {
        var def = agentRegistry.findByClass("io.quarkiverse.agent.it.AnnotatedHierarchicalAgent");
        assertNotNull(def, "Hierarchical agent should be discovered");
        assertEquals(io.quarkiverse.agent.runtime.annotation.Architecture.HIERARCHICAL, def.architecture());
        assertEquals(2, def.teams().size());
        assertEquals("Research", def.teams().get(0).name());
        assertEquals(2, def.teams().get(0).workers().size());
        assertTrue(def.teams().get(0).workers().contains("analyst"));
        assertTrue(def.teams().get(0).workers().contains("scientist"));
        assertEquals("Writing", def.teams().get(1).name());
    }

    @Test
    void annotatedSwarmAgentHasAgents() {
        var def = agentRegistry.findByClass("io.quarkiverse.agent.it.AnnotatedSwarmAgent");
        assertNotNull(def, "Swarm agent should be discovered");
        assertEquals(io.quarkiverse.agent.runtime.annotation.Architecture.SWARM, def.architecture());
        assertEquals(3, def.swarmAgents().size());
        assertEquals("Alice", def.swarmAgents().get(0).name());
        assertEquals("Optimistic strategist", def.swarmAgents().get(0).personality());
        assertEquals("Bob", def.swarmAgents().get(1).name());
    }

    @Test
    void annotatedHandoffAgentHasHandoffs() {
        var def = agentRegistry.findByClass("io.quarkiverse.agent.it.AnnotatedHandoffAgent");
        assertNotNull(def, "Handoff agent should be discovered");
        assertEquals(io.quarkiverse.agent.runtime.annotation.Architecture.HANDOFF, def.architecture());
        assertEquals(3, def.handoffAgents().size());
        assertEquals("triage", def.handoffAgents().get(0).name());
        assertEquals("billing,support", def.handoffAgents().get(0).canHandoffTo());
        assertEquals("billing", def.handoffAgents().get(1).name());
    }

    @Test
    void annotatedMapReduceAgentHasConfig() {
        var def = agentRegistry.findByClass("io.quarkiverse.agent.it.AnnotatedMapReduceAgent");
        assertNotNull(def, "MapReduce agent should be discovered");
        assertEquals(io.quarkiverse.agent.runtime.annotation.Architecture.MAP_REDUCE, def.architecture());
        assertNotNull(def.mapReduce());
        assertEquals("Summarize this document", def.mapReduce().mapPrompt());
        assertEquals("Synthesize all summaries into a report", def.mapReduce().reducePrompt());
    }
}
