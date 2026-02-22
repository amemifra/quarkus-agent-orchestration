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
package io.quarkiverse.agent.runtime.arch;

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.quarkiverse.agent.runtime.llm.LlmHelper;
import io.quarkiverse.agent.runtime.core.AgentConfig;
import io.quarkiverse.agent.runtime.core.AgentContext;
import io.quarkiverse.agent.runtime.core.AgentResult;
import io.quarkiverse.agent.runtime.core.Step;
import io.quarkiverse.agent.runtime.llm.ChatMessage;
import io.quarkiverse.agent.runtime.llm.LlmClient;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A4: State Graph — Cyclic graph of nodes (functions) connected by edges (conditional/fixed).
 * Maintains persistent state. Inspired by LangGraph.
 */
@ApplicationScoped
public class StateGraphEngine {

    public AgentResult execute(AgentConfig config, GraphDef graph, String input, LlmClient llm) {
        long start = System.currentTimeMillis();
        var context = new AgentContext();
        context.setVariable("input", input);
        context.setVariable("output", "");

        String currentNode = graph.entryNode();

        while (currentNode != null && !currentNode.equals("END") && context.iteration() < config.maxIterations()) {
            context.incrementIteration();
            NodeDef node = graph.nodes().get(currentNode);
            if (node == null) break;

            context.addStep(Step.action("Node: " + currentNode));

            // Execute node
            String nodeInput = context.getVariable("output") != null ?
                    context.<String>getVariable("output") : input;

            var nodeCtx = new AgentContext();
            nodeCtx.addMessage(ChatMessage.system(node.systemPrompt()));
            nodeCtx.addMessage(ChatMessage.user(nodeInput));
            String nodeOutput = LlmHelper.chat(config, nodeCtx, llm);
            context.setVariable("output", nodeOutput);
            context.addStep(Step.observation(nodeOutput));

            // Determine next node
            if (node.router() != null) {
                currentNode = node.router().apply(nodeOutput);
            } else {
                currentNode = graph.edges().getOrDefault(currentNode, "END");
            }
        }

        String output = context.getVariable("output");
        return AgentResult.of(output != null ? output : "", context, System.currentTimeMillis() - start);
    }


    @RegisterForReflection
    public record NodeDef(String systemPrompt, Function<String, String> router) {
        public static NodeDef of(String systemPrompt) {
            return new NodeDef(systemPrompt, null);
        }

        public static NodeDef withRouter(String systemPrompt, Function<String, String> router) {
            return new NodeDef(systemPrompt, router);
        }
    }

    @RegisterForReflection
    public record GraphDef(String entryNode, Map<String, NodeDef> nodes, Map<String, String> edges) {
        public static GraphDef of(String entryNode, Map<String, NodeDef> nodes, Map<String, String> edges) {
            return new GraphDef(entryNode, nodes, edges);
        }
    }
}
