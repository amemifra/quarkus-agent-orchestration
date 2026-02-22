package io.quarkiverse.agent.runtime.declarative;

import java.util.List;
import java.util.Map;

/**
 * Representation of an Agent loaded from a declarative format (YAML/JSON/MD).
 */
public class DeclarativeAgentConfig {

    public String name;
    public String type; // e.g. "REACT", "TOOL_USE", "SUPERVISOR", etc.
    public String description;
    public String systemPrompt;

    // For Multi-Agent architectures
    public List<WorkerDef> workers;
    public List<TeamDef> teams;
    public List<CrewMemberDef> crew;

    public static class WorkerDef {
        public String id;
        public String name;
        public String description;
        public DeclarativeAgentConfig agent; // nested agent definition
    }

    public static class TeamDef {
        public String name;
        public String objective;
        public List<WorkerDef> workers;
    }

    public static class CrewMemberDef {
        public String role;
        public String goal;
        public String backstory;
        public DeclarativeAgentConfig agent;
    }
}
