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
package io.quarkiverse.agent.runtime;

import io.quarkiverse.agent.runtime.arch.*;
import io.quarkiverse.agent.runtime.core.AgentConfig;
import io.quarkiverse.agent.runtime.core.AgentResult;
import io.quarkiverse.agent.runtime.llm.LlmClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

/**
 * The single CDI injectable service that exposes ALL 14 AI agent orchestration
 * architectures.
 * 
 * <pre>
 * &#64;Inject
 * AgentOrchestrationService agents;
 * agents.react(config, "What is 2+2?");
 * agents.crew(config, crewMembers, "Write a report");
 * </pre>
 */
@ApplicationScoped
public class AgentOrchestrationService {

    @Inject
    LlmClient llmClient;

    // ── All 14 Architecture Engines (CDI-injected) ──
    @Inject
    ToolUseEngine toolUseEngine;
    @Inject
    ReActEngine reActEngine;
    @Inject
    ReflectionEngine reflectionEngine;
    @Inject
    VerificationEngine verificationEngine;
    @Inject
    PipelineEngine pipelineEngine;
    @Inject
    PlanExecuteEngine planExecuteEngine;
    @Inject
    SupervisorEngine supervisorEngine;
    @Inject
    MapReduceEngine mapReduceEngine;
    @Inject
    DebateEngine debateEngine;
    @Inject
    StateGraphEngine stateGraphEngine;
    @Inject
    HierarchicalEngine hierarchicalEngine;
    @Inject
    CrewEngine crewEngine;
    @Inject
    SwarmEngine swarmEngine;
    @Inject
    HandoffEngine handoffEngine;

    public String version() {
        return "1.0.0-SNAPSHOT";
    }

    // ── Declarative Agents (Roadmap T7) ──
    public io.quarkiverse.agent.runtime.declarative.DeclarativeAgentConfig loadDeclarative(String classpathResource) {
        try {
            return io.quarkiverse.agent.runtime.declarative.DeclarativeAgentParser.parseClasspath(classpathResource);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to load declarative agent from: " + classpathResource, e);
        }
    }

    public AgentResult executeDeclarative(AgentConfig config, String classpathResource, String prompt) {
        io.quarkiverse.agent.runtime.declarative.DeclarativeAgentConfig dec = loadDeclarative(classpathResource);
        // Map declarative config to execution. Using "TOOL_USE" as default fallback for
        // single agents.
        if (dec.type == null || dec.type.equalsIgnoreCase("TOOL_USE") || dec.type.equalsIgnoreCase("TOOL")) {
            // For now, override system prompt if defined in declarative file
            if (dec.systemPrompt != null && !dec.systemPrompt.isBlank()) {
                config = AgentConfig.builder(config).systemPrompt(dec.systemPrompt).build();
            }
            return toolUseEngine.execute(config, prompt, llmClient);
        } else if (dec.type.equalsIgnoreCase("REACT")) {
            if (dec.systemPrompt != null && !dec.systemPrompt.isBlank()) {
                config = AgentConfig.builder(config).systemPrompt(dec.systemPrompt).build();
            }
            return reActEngine.execute(config, prompt, llmClient);
        }
        throw new UnsupportedOperationException("Execution not yet fully mapped for declarative type: " + dec.type);
    }

    // ── A13: Tool-Use Agent ──
    public AgentResult toolUse(AgentConfig config, String prompt) {
        return toolUseEngine.execute(config, prompt, llmClient);
    }

    // ── A1: ReAct ──
    public AgentResult react(AgentConfig config, String prompt) {
        return reActEngine.execute(config, prompt, llmClient);
    }

    // ── A3: Reflection ──
    public AgentResult reflect(AgentConfig config, String prompt) {
        return reflectionEngine.execute(config, prompt, llmClient);
    }

    // ── A14: Verification Loop ──
    public AgentResult verify(AgentConfig config, String prompt) {
        return verificationEngine.execute(config, prompt, llmClient);
    }

    // ── A9: DAG Pipeline ──
    public AgentResult pipeline(AgentConfig config, List<PipelineEngine.PipelineStep> steps, String input) {
        return pipelineEngine.execute(config, steps, input, llmClient);
    }

    // ── A2: Plan-and-Execute ──
    public AgentResult planAndExecute(AgentConfig config, String prompt) {
        return planExecuteEngine.execute(config, prompt, llmClient);
    }

    // ── A5: Supervisor ──
    public AgentResult supervisor(AgentConfig config, List<SupervisorEngine.WorkerDef> workers, String prompt) {
        return supervisorEngine.execute(config, workers, prompt, llmClient);
    }

    // ── A11: Map-Reduce ──
    public AgentResult mapReduce(AgentConfig config, List<String> inputs, String mapPrompt, String reducePrompt) {
        return mapReduceEngine.execute(config, inputs, mapPrompt, reducePrompt, llmClient);
    }

    // ── A12: Debate-Consensus ──
    public AgentResult debate(AgentConfig config, int numDebaters, String prompt) {
        return debateEngine.execute(config, numDebaters, prompt, llmClient);
    }

    // ── A4: State Graph ──
    public AgentResult stateGraph(AgentConfig config, StateGraphEngine.GraphDef graph, String input) {
        return stateGraphEngine.execute(config, graph, input, llmClient);
    }

    // ── A6: Hierarchical ──
    public AgentResult hierarchical(AgentConfig config, List<HierarchicalEngine.TeamDef> teams, String prompt) {
        return hierarchicalEngine.execute(config, teams, prompt, llmClient);
    }

    // ── A7: Role-Based Crew ──
    public AgentResult crew(AgentConfig config, List<CrewEngine.CrewMember> crew, String prompt) {
        return crewEngine.execute(config, crew, prompt, llmClient);
    }

    // ── A8: Conversational Swarm ──
    public AgentResult swarm(AgentConfig config, List<SwarmEngine.SwarmAgent> agents, String topic) {
        return swarmEngine.execute(config, agents, topic, llmClient);
    }

    // ── A10: Handoff ──
    public AgentResult handoff(AgentConfig config, Map<String, HandoffEngine.HandoffAgent> agents,
            String startAgent, String prompt) {
        return handoffEngine.execute(config, agents, startAgent, prompt, llmClient);
    }
}
