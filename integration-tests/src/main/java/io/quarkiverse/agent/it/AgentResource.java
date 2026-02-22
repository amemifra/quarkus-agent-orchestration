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
import io.quarkiverse.agent.runtime.arch.CrewEngine;
import io.quarkiverse.agent.runtime.arch.PipelineEngine;
import io.quarkiverse.agent.runtime.core.AgentConfig;
import io.quarkiverse.agent.runtime.core.AgentResult;
import io.quarkiverse.agent.runtime.llm.LlmClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;

/**
 * Showcase resource demonstrating 3+ agent architectures via REST endpoints.
 */
@Path("/agent")
public class AgentResource {

    @Inject
    AgentOrchestrationService agents;

    @Inject
    LlmClient llmClient;

    @GET
    @Path("/health")
    @Produces(MediaType.TEXT_PLAIN)
    public String health() {
        return "ok";
    }

    @GET
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    public String version() {
        return agents.version();
    }

    @GET
    @Path("/llm-class")
    @Produces(MediaType.TEXT_PLAIN)
    public String llmClass() {
        return llmClient.getClass().getSimpleName();
    }

    // ── T6 Showcase: 3 Architecture Demos ──

    /**
     * Demo: Reflection architecture — critique and refine a response.
     */
    @GET
    @Path("/showcase/reflect")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> showcaseReflect(@QueryParam("prompt") @DefaultValue("Write a haiku about Java") String prompt) {
        var config = AgentConfig.builder().model("showcase").maxIterations(3).build();
        AgentResult result = agents.reflect(config, prompt);
        return Map.of(
                "architecture", "A3-Reflection",
                "content", result.content(),
                "steps", result.steps().size(),
                "durationMs", result.durationMs()
        );
    }

    /**
     * Demo: Pipeline architecture — chain of processing steps.
     */
    @GET
    @Path("/showcase/pipeline")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> showcasePipeline(@QueryParam("input") @DefaultValue("Hello world") String input) {
        var config = AgentConfig.builder().model("showcase").build();
        var steps = List.of(
                PipelineEngine.PipelineStep.of("summarize", "Summarize the text concisely."),
                PipelineEngine.PipelineStep.of("translate", "Translate to Italian."),
                PipelineEngine.PipelineStep.of("format", "Format as a bullet-point list.")
        );
        AgentResult result = agents.pipeline(config, steps, input);
        return Map.of(
                "architecture", "A9-Pipeline",
                "content", result.content(),
                "steps", result.steps().size(),
                "durationMs", result.durationMs()
        );
    }

    /**
     * Demo: Crew architecture — role-based agents collaborate.
     */
    @GET
    @Path("/showcase/crew")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> showcaseCrew(@QueryParam("topic") @DefaultValue("cloud computing trends") String topic) {
        var config = AgentConfig.builder().model("showcase").build();
        var crew = List.of(
                CrewEngine.CrewMember.of("Researcher", "Expert data analyst", "Research the topic"),
                CrewEngine.CrewMember.of("Writer", "Technical writer", "Write a concise report"),
                CrewEngine.CrewMember.of("Editor", "Quality reviewer", "Review and polish the report")
        );
        AgentResult result = agents.crew(config, crew, topic);
        return Map.of(
                "architecture", "A7-Crew",
                "content", result.content(),
                "steps", result.steps().size(),
                "durationMs", result.durationMs()
        );
    }
}
