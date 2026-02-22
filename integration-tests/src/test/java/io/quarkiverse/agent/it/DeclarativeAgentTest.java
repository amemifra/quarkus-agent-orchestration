package io.quarkiverse.agent.it;

import io.quarkiverse.agent.runtime.AgentOrchestrationService;
import io.quarkiverse.agent.runtime.declarative.DeclarativeAgentConfig;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class DeclarativeAgentTest {

    @Inject
    AgentOrchestrationService agentService;

    @Test
    void testLoadYamlAgent() {
        DeclarativeAgentConfig config = agentService.loadDeclarative("agent1.yaml");
        assertNotNull(config);
        assertEquals("Code Reviewer YAML", config.name);
        assertEquals("REACT", config.type);
        assertEquals("You are a senior code reviewer. Analyze the code strictly.", config.systemPrompt);
    }

    @Test
    void testLoadJsonAgent() {
        DeclarativeAgentConfig config = agentService.loadDeclarative("agent2.json");
        assertNotNull(config);
        assertEquals("Math Assistant JSON", config.name);
        assertEquals("TOOL_USE", config.type);
        assertEquals("You are a helpful math assistant.", config.systemPrompt);
    }

    @Test
    void testLoadMarkdownAgent() {
        DeclarativeAgentConfig config = agentService.loadDeclarative("agent3.md");
        assertNotNull(config);
        assertEquals("Markdown Writer MD", config.name);
        assertEquals("REACT", config.type);
        assertTrue(config.systemPrompt.contains("You are an expert technical writer."));
    }
}
