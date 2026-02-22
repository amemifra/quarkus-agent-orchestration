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

import io.quarkiverse.agent.runtime.annotation.AgentDefinition;
import io.quarkiverse.agent.runtime.annotation.AgentRegistry;
import io.quarkiverse.agent.runtime.annotation.Architecture;
import io.quarkiverse.agent.runtime.llm.InferenceQuality;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;

import java.util.List;

/**
 * Records agent definitions discovered at build time.
 */
@Recorder
public class AgentExtensionRecorder {

    public void registerAgent(String className, String provider, String model,
                              String architecture, int maxIterations, double temperature,
                              String systemPrompt, String quality,
                              String generateMethod, String evaluateMethod,
                              List<AgentDefinition.CrewMemberDef> crewMembers,
                              List<AgentDefinition.PipelineStepDef> pipelineSteps,
                              List<AgentDefinition.DebateAgentDef> debateAgents,
                              List<AgentDefinition.AgentToolDef> agentTools,
                              List<AgentDefinition.GraphNodeDef> graphNodes,
                              List<AgentDefinition.WorkerDef> workers,
                              List<AgentDefinition.TeamDef> teams,
                              List<AgentDefinition.SwarmAgentDef> swarmAgents,
                              List<AgentDefinition.HandoffAgentDef> handoffAgents,
                              AgentDefinition.MapReduceDef mapReduce) {
        var definition = new AgentDefinition(
                className, provider, model,
                Architecture.valueOf(architecture),
                maxIterations, temperature, systemPrompt,
                InferenceQuality.valueOf(quality),
                generateMethod, evaluateMethod,
                safe(crewMembers), safe(pipelineSteps), safe(debateAgents),
                safe(agentTools), safe(graphNodes), safe(workers),
                safe(teams), safe(swarmAgents), safe(handoffAgents),
                mapReduce
        );
        Arc.container().instance(AgentRegistry.class).get().register(definition);
    }

    private static <T> List<T> safe(List<T> list) {
        return list != null ? list : List.of();
    }
}
