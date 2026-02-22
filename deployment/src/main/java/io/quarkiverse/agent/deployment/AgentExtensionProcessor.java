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
package io.quarkiverse.agent.deployment;

import io.quarkiverse.agent.runtime.AgentExtensionRecorder;
import io.quarkiverse.agent.runtime.AgentOrchestrationService;
import io.quarkiverse.agent.runtime.annotation.AgentDefinition;
import io.quarkiverse.agent.runtime.annotation.AgentRegistry;
import io.quarkiverse.agent.runtime.llm.openai.OpenAiLlmClient;
import io.quarkiverse.agent.runtime.tool.docker.DockerSandboxExecutor;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.jboss.jandex.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Build-step processor for the Agent Extension.
 * Scans for @Agent + ALL repeatable multi-agent annotations via Jandex.
 */
class AgentExtensionProcessor {

    private static final String FEATURE = "quarkus-agent-extension";
    private static final String PKG = "io.quarkiverse.agent.runtime.annotation.";

    private static final DotName AGENT = DotName.createSimple(PKG + "Agent");
    private static final DotName GENERATE = DotName.createSimple(PKG + "Generate");
    private static final DotName EVALUATE = DotName.createSimple(PKG + "EvaluateOutput");

    // Existing repeatable annotations
    private static final DotName CREW_MEMBER = DotName.createSimple(PKG + "CrewMember");
    private static final DotName CREW_MEMBERS = DotName.createSimple(PKG + "CrewMembers");
    private static final DotName PIPELINE_STEP = DotName.createSimple(PKG + "PipelineStep");
    private static final DotName PIPELINE_STEPS = DotName.createSimple(PKG + "PipelineSteps");
    private static final DotName DEBATE_AGENT = DotName.createSimple(PKG + "DebateAgent");
    private static final DotName DEBATE_AGENTS = DotName.createSimple(PKG + "DebateAgents");

    // Sprint #15: New repeatable annotations
    private static final DotName AGENT_TOOL = DotName.createSimple(PKG + "AgentTool");
    private static final DotName AGENT_TOOLS = DotName.createSimple(PKG + "AgentTools");
    private static final DotName GRAPH_NODE = DotName.createSimple(PKG + "GraphNode");
    private static final DotName GRAPH_NODES = DotName.createSimple(PKG + "GraphNodes");
    private static final DotName WORKER = DotName.createSimple(PKG + "Worker");
    private static final DotName WORKERS = DotName.createSimple(PKG + "Workers");
    private static final DotName TEAM = DotName.createSimple(PKG + "Team");
    private static final DotName TEAMS = DotName.createSimple(PKG + "Teams");
    private static final DotName SWARM_AGENT = DotName.createSimple(PKG + "SwarmAgent");
    private static final DotName SWARM_AGENTS = DotName.createSimple(PKG + "SwarmAgents");
    private static final DotName HANDOFF_AGENT = DotName.createSimple(PKG + "HandoffAgent");
    private static final DotName HANDOFF_AGENTS = DotName.createSimple(PKG + "HandoffAgents");
    private static final DotName MAP_REDUCE = DotName.createSimple(PKG + "MapReduce");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem registerBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(
                        AgentOrchestrationService.class,
                        OpenAiLlmClient.class,
                        DockerSandboxExecutor.class,
                        AgentRegistry.class,
                        io.quarkiverse.agent.runtime.llm.LlmClientFactory.class,
                        io.quarkiverse.agent.runtime.llm.InferenceRouter.class,
                        io.quarkiverse.agent.runtime.llm.DefaultInferenceClassifier.class,
                        // All 14 architecture engines
                        io.quarkiverse.agent.runtime.arch.ReActEngine.class,
                        io.quarkiverse.agent.runtime.arch.PlanExecuteEngine.class,
                        io.quarkiverse.agent.runtime.arch.ReflectionEngine.class,
                        io.quarkiverse.agent.runtime.arch.StateGraphEngine.class,
                        io.quarkiverse.agent.runtime.arch.SupervisorEngine.class,
                        io.quarkiverse.agent.runtime.arch.HierarchicalEngine.class,
                        io.quarkiverse.agent.runtime.arch.CrewEngine.class,
                        io.quarkiverse.agent.runtime.arch.SwarmEngine.class,
                        io.quarkiverse.agent.runtime.arch.PipelineEngine.class,
                        io.quarkiverse.agent.runtime.arch.HandoffEngine.class,
                        io.quarkiverse.agent.runtime.arch.MapReduceEngine.class,
                        io.quarkiverse.agent.runtime.arch.DebateEngine.class,
                        io.quarkiverse.agent.runtime.arch.ToolUseEngine.class,
                        io.quarkiverse.agent.runtime.arch.VerificationEngine.class
                )
                .setUnremovable()
                .build();
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void scanAgentAnnotations(CombinedIndexBuildItem index,
                              AgentExtensionRecorder recorder) {
        for (AnnotationInstance annotation : index.getIndex().getAnnotations(AGENT)) {
            var classInfo = annotation.target().asClass();

            // Methods: @Generate, @EvaluateOutput
            String generateMethod = null;
            String evaluateMethod = null;
            for (MethodInfo method : classInfo.methods()) {
                if (method.hasAnnotation(GENERATE)) generateMethod = method.name();
                if (method.hasAnnotation(EVALUATE)) evaluateMethod = method.name();
            }

            // ── Existing Annotations ──

            var crewMembers = new ArrayList<AgentDefinition.CrewMemberDef>();
            collectRepeatableAnnotations(classInfo, CREW_MEMBER, CREW_MEMBERS).forEach(a ->
                    crewMembers.add(new AgentDefinition.CrewMemberDef(
                            a.value("role").asString(),
                            strVal(a, "backstory", ""),
                            a.value("task").asString()
                    )));

            var pipelineSteps = new ArrayList<AgentDefinition.PipelineStepDef>();
            collectRepeatableAnnotations(classInfo, PIPELINE_STEP, PIPELINE_STEPS).forEach(a ->
                    pipelineSteps.add(new AgentDefinition.PipelineStepDef(
                            a.value("name").asString(),
                            a.value("systemPrompt").asString(),
                            intVal(a, "order", 0)
                    )));
            pipelineSteps.sort(Comparator.comparingInt(AgentDefinition.PipelineStepDef::order));

            var debateAgents = new ArrayList<AgentDefinition.DebateAgentDef>();
            collectRepeatableAnnotations(classInfo, DEBATE_AGENT, DEBATE_AGENTS).forEach(a ->
                    debateAgents.add(new AgentDefinition.DebateAgentDef(
                            a.value("name").asString(),
                            strVal(a, "personality", "")
                    )));

            // ── Sprint #15: New Annotations ──

            var agentTools = new ArrayList<AgentDefinition.AgentToolDef>();
            collectRepeatableAnnotations(classInfo, AGENT_TOOL, AGENT_TOOLS).forEach(a ->
                    agentTools.add(new AgentDefinition.AgentToolDef(
                            a.value("name").asString(),
                            strVal(a, "description", "")
                    )));

            var graphNodes = new ArrayList<AgentDefinition.GraphNodeDef>();
            collectRepeatableAnnotations(classInfo, GRAPH_NODE, GRAPH_NODES).forEach(a ->
                    graphNodes.add(new AgentDefinition.GraphNodeDef(
                            a.value("name").asString(),
                            strVal(a, "systemPrompt", ""),
                            strVal(a, "transitions", "")
                    )));

            var workers = new ArrayList<AgentDefinition.WorkerDef>();
            collectRepeatableAnnotations(classInfo, WORKER, WORKERS).forEach(a ->
                    workers.add(new AgentDefinition.WorkerDef(
                            a.value("name").asString(),
                            strVal(a, "systemPrompt", "")
                    )));

            var teams = new ArrayList<AgentDefinition.TeamDef>();
            collectRepeatableAnnotations(classInfo, TEAM, TEAMS).forEach(a ->
                    teams.add(new AgentDefinition.TeamDef(
                            a.value("name").asString(),
                            Arrays.asList(a.value("workers").asStringArray())
                    )));

            var swarmAgents = new ArrayList<AgentDefinition.SwarmAgentDef>();
            collectRepeatableAnnotations(classInfo, SWARM_AGENT, SWARM_AGENTS).forEach(a ->
                    swarmAgents.add(new AgentDefinition.SwarmAgentDef(
                            a.value("name").asString(),
                            strVal(a, "personality", "")
                    )));

            var handoffAgents = new ArrayList<AgentDefinition.HandoffAgentDef>();
            collectRepeatableAnnotations(classInfo, HANDOFF_AGENT, HANDOFF_AGENTS).forEach(a ->
                    handoffAgents.add(new AgentDefinition.HandoffAgentDef(
                            a.value("name").asString(),
                            strVal(a, "systemPrompt", ""),
                            strVal(a, "canHandoffTo", "")
                    )));

            // @MapReduce (single, not repeatable)
            AgentDefinition.MapReduceDef mapReduce = null;
            AnnotationInstance mrAnn = classInfo.annotation(MAP_REDUCE);
            if (mrAnn != null) {
                mapReduce = new AgentDefinition.MapReduceDef(
                        mrAnn.value("mapPrompt").asString(),
                        mrAnn.value("reducePrompt").asString()
                );
            }

            // ── M1: Build-time annotation validation ──
            String arch = annotation.value("architecture").asEnum();
            validateAnnotationConsistency(classInfo.name().toString(), arch,
                    crewMembers, pipelineSteps, debateAgents, agentTools,
                    graphNodes, workers, teams, swarmAgents, handoffAgents, mapReduce);

            recorder.registerAgent(
                    classInfo.name().toString(),
                    strVal(annotation, "provider", "ollama"),
                    annotation.value("model").asString(),
                    arch,
                    intVal(annotation, "maxIterations", 10),
                    doubleVal(annotation, "temperature", 0.7),
                    strVal(annotation, "systemPrompt", ""),
                    enumVal(annotation, "quality", "MEDIUM"),
                    generateMethod, evaluateMethod,
                    crewMembers, pipelineSteps, debateAgents,
                    agentTools, graphNodes, workers,
                    teams, swarmAgents, handoffAgents,
                    mapReduce
            );
        }
    }

    /** Validates that annotation composition matches the declared architecture. */
    private void validateAnnotationConsistency(String className, String arch,
                                                List<?> crews, List<?> pipelines, List<?> debates,
                                                List<?> tools, List<?> nodes, List<?> workers,
                                                List<?> teams, List<?> swarms, List<?> handoffs,
                                                AgentDefinition.MapReduceDef mapReduce) {
        switch (arch) {
            case "CREW" -> { if (crews.isEmpty()) warn(className, "CREW", "@CrewMember"); }
            case "PIPELINE" -> { if (pipelines.isEmpty()) warn(className, "PIPELINE", "@PipelineStep"); }
            case "DEBATE" -> { if (debates.isEmpty()) warn(className, "DEBATE", "@DebateAgent"); }
            case "REACT", "TOOL_USE" -> { if (tools.isEmpty()) warn(className, arch, "@AgentTool"); }
            case "STATE_GRAPH" -> { if (nodes.isEmpty()) warn(className, "STATE_GRAPH", "@GraphNode"); }
            case "SUPERVISOR" -> { if (workers.isEmpty()) warn(className, "SUPERVISOR", "@Worker"); }
            case "HIERARCHICAL" -> { if (teams.isEmpty()) warn(className, "HIERARCHICAL", "@Team"); }
            case "SWARM" -> { if (swarms.isEmpty()) warn(className, "SWARM", "@SwarmAgent"); }
            case "HANDOFF" -> { if (handoffs.isEmpty()) warn(className, "HANDOFF", "@HandoffAgent"); }
            case "MAP_REDUCE" -> { if (mapReduce == null) warn(className, "MAP_REDUCE", "@MapReduce"); }
            default -> { /* REFLECTION, PLAN_EXECUTE, VERIFICATION — no companion needed */ }
        }
    }

    private void warn(String className, String arch, String annotation) {
        System.out.printf("[WARNING] Agent '%s' has architecture %s but no %s annotations. " +
                "This will likely fail at runtime.%n", className, arch, annotation);
    }

    /** Collects both single and container (repeatable) annotations from a class. */
    private List<AnnotationInstance> collectRepeatableAnnotations(ClassInfo classInfo,
                                                                  DotName single, DotName container) {
        var result = new ArrayList<AnnotationInstance>();
        AnnotationInstance singleAnn = classInfo.annotation(single);
        if (singleAnn != null) result.add(singleAnn);
        AnnotationInstance containerAnn = classInfo.annotation(container);
        if (containerAnn != null) {
            for (AnnotationInstance nested : containerAnn.value().asNestedArray()) {
                result.add(nested);
            }
        }
        return result;
    }

    private static String strVal(AnnotationInstance a, String name, String def) {
        AnnotationValue v = a.value(name);
        return v != null ? v.asString() : def;
    }

    private static int intVal(AnnotationInstance a, String name, int def) {
        AnnotationValue v = a.value(name);
        return v != null ? v.asInt() : def;
    }

    private static double doubleVal(AnnotationInstance a, String name, double def) {
        AnnotationValue v = a.value(name);
        return v != null ? v.asDouble() : def;
    }

    private static String enumVal(AnnotationInstance a, String name, String def) {
        AnnotationValue v = a.value(name);
        return v != null ? v.asEnum() : def;
    }
}
