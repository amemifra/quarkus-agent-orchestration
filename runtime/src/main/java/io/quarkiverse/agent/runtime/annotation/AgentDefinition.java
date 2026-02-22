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

import io.quarkiverse.agent.runtime.core.AgentConfig;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkiverse.agent.runtime.llm.InferenceQuality;

import java.util.List;

/**
 * Runtime representation of a discovered @Agent-annotated class.
 * Created at build time by the processor, used at runtime to execute agents.
 */
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record AgentDefinition(
        String className,
        String provider,
        String model,
        Architecture architecture,
        int maxIterations,
        double temperature,
        String systemPrompt,
        InferenceQuality quality,
        String generateMethodName,
        String evaluateMethodName,
        // Multi-agent annotation data
        List<CrewMemberDef> crewMembers,
        List<PipelineStepDef> pipelineSteps,
        List<DebateAgentDef> debateAgents,
        List<AgentToolDef> agentTools,
        List<GraphNodeDef> graphNodes,
        List<WorkerDef> workers,
        List<TeamDef> teams,
        List<SwarmAgentDef> swarmAgents,
        List<HandoffAgentDef> handoffAgents,
        MapReduceDef mapReduce
) {
    /** Compact constructor for backward compat. */
    public AgentDefinition(String className, String provider, String model,
                           Architecture architecture, int maxIterations, double temperature,
                           String systemPrompt, String generateMethodName, String evaluateMethodName) {
        this(className, provider, model, architecture, maxIterations, temperature,
                systemPrompt, InferenceQuality.MEDIUM, generateMethodName, evaluateMethodName,
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), null);
    }

    public AgentConfig toConfig() {
        var builder = AgentConfig.builder()
                .model(model)
                .temperature(temperature)
                .maxIterations(maxIterations);
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            builder.systemPrompt(systemPrompt);
        }
        return builder.build();
    }

    // Existing defs
    public record CrewMemberDef(String role, String backstory, String task) { }
    public record PipelineStepDef(String name, String systemPrompt, int order) { }
    public record DebateAgentDef(String name, String personality) { }

    // New defs for Sprint #15
    public record AgentToolDef(String name, String description) { }
    public record GraphNodeDef(String name, String systemPrompt, String transitions) { }
    public record WorkerDef(String name, String systemPrompt) { }
    public record TeamDef(String name, List<String> workers) { }
    public record SwarmAgentDef(String name, String personality) { }
    public record HandoffAgentDef(String name, String systemPrompt, String canHandoffTo) { }
    public record MapReduceDef(String mapPrompt, String reducePrompt) { }
}
