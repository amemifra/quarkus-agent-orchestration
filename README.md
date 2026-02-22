# Quarkus Agent Extension

[![Maven Central](https://img.shields.io/maven-central/v/io.quarkiverse.agent/quarkus-agent-extension-parent)](https://search.maven.org/search?q=g:io.quarkiverse.agent)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]()
[![Quality Gate](https://img.shields.io/badge/quality%20gate-passed-brightgreen)]()
[![Java 21+](https://img.shields.io/badge/java-21%2B-blue)]()
[![Quarkus 3.x](https://img.shields.io/badge/quarkus-3.x-blue)]()

> A Quarkus extension that exposes **all 14 AI agent orchestration architectures** as methods of a single, injectable CDI service — with **21 LLM providers** and **quality-tier routing**.

## Features

- 🤖 **14 Agent Architectures** — ReAct, Plan-Execute, Reflection, StateGraph, Supervisor, Hierarchical, Crew, Swarm, Pipeline, Handoff, MapReduce, Debate, ToolUse, Verification
- 🌐 **21 LLM Providers** — OpenAI, Ollama, Claude, Gemini, DeepSeek, Mistral, Groq, Together, Fireworks, Perplexity, Cohere, DeepInfra, OpenRouter, HuggingFace, vLLM, LM Studio, LocalAI, Replicate, SambaNova, Cerebras, Anyscale
- 🧠 **Quality-Tier Routing** — Auto-classify tasks as HIGH/MEDIUM/LOW and route to the right provider
- 📝 **Declarative API** — `@Agent`, `@CrewMember`, `@PipelineStep`, `@DebateAgent` with `quality` tier
- ⚡ **Programmatic API** — Full control via `AgentOrchestrationService` CDI bean
- 🐳 **Docker Sandbox** — Isolated tool execution in ephemeral containers via Docker Java API
- 🏗️ **Build-Time Augmentation** — Jandex annotation scanning, zero runtime reflection

## Quick Start

### 1. Add dependency

```xml
<dependency>
    <groupId>io.quarkiverse.agent</groupId>
    <artifactId>quarkus-agent-extension</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Configure your LLM provider

```properties
# application.properties
quarkus.agent.base-url=http://localhost:11434
```

### 3. Use the Programmatic API

```java
@Inject
AgentOrchestrationService agents;

public void example() {
    var config = AgentConfig.builder()
        .model("llama3")
        .temperature(0.7)
        .maxIterations(5)
        .build();

    AgentResult result = agents.reflect(config, "Write a haiku about Java");
}
```

### 4. Use the Declarative API

```java
@Agent(provider = "ollama", model = "llama3",
       architecture = Architecture.REFLECTION,
       maxIterations = 3, quality = InferenceQuality.HIGH)
public class WriterAgent {

    @Generate
    public String write(@UserPrompt String topic) {
        return "Write a creative poem about: " + topic;
    }

    @EvaluateOutput
    public boolean isGoodEnough(String output) {
        return output != null && output.length() > 50;
    }
}
```

### 5. Quality-Tier Routing

```java
@Inject InferenceRouter router;

// Configure provider tiers
router.configure(InferenceRoute.of(HIGH, ANTHROPIC, "claude-sonnet-4", "sk-xxx"));
router.configure(InferenceRoute.of(MEDIUM, OLLAMA, "llama3"));
router.configure(InferenceRoute.of(LOW, GROQ, "mixtral-8x7b", "gsk-xxx"));

// Auto-route: classifies prompt → picks provider
LlmClient client = router.autoRoute("Analyze this complex legal contract...");
// → HIGH → Anthropic Claude

LlmClient fast = router.autoRoute("Say hello");
// → LOW → Groq
```

## Supported Providers

| Family | Providers | Strategy |
|--------|-----------|----------|
| **OpenAI-compatible** | OpenAI, Ollama, DeepSeek, Mistral, Groq, Together, Fireworks, Perplexity, DeepInfra, OpenRouter, vLLM, LM Studio, LocalAI, HuggingFace, Replicate, SambaNova, Cerebras, Anyscale | Same client, different base URL |
| **Anthropic Claude** | Claude API, AWS Bedrock, GCP Vertex AI | Dedicated client (`/v1/messages`) |
| **Google Gemini** | Google AI, Vertex AI | Dedicated client (`/v1beta/models`) |
| **Cohere** | Command | Dedicated client (`/v2/chat`) |

## Architecture Catalog

| # | Method | Architecture | Description |
|---|--------|-------------|-------------|
| A1 | `react()` | ReAct | Thought → Action → Observation loop |
| A2 | `planAndExecute()` | Plan-and-Execute | Plan → Execute → Synthesize |
| A3 | `reflect()` | Reflection | Generate → Critique → Refine |
| A4 | `stateGraph()` | State Graph | Cyclic graph with conditional edges |
| A5 | `supervisor()` | Supervisor | Delegate to workers → synthesize |
| A6 | `hierarchical()` | Hierarchical | Multi-level supervisor tree |
| A7 | `crew()` | Role-Based Crew | Agents with defined roles collaborate |
| A8 | `swarm()` | Conversational Swarm | Free-form agent conversation |
| A9 | `pipeline()` | DAG Pipeline | Sequential step chain |
| A10 | `handoff()` | Handoff | Dynamic agent-to-agent transfer |
| A11 | `mapReduce()` | Map-Reduce | Fan-out + aggregate |
| A12 | `debate()` | Debate-Consensus | N debaters + arbiter |
| A13 | `toolUse()` | Tool-Use Agent | LLM decides which tools to call |
| A14 | `verify()` | Verification Loop | Generator + verifier loop |

## Multi-Agent Annotations

```java
@Agent(model = "llama3", architecture = Architecture.CREW, quality = InferenceQuality.MEDIUM)
@CrewMember(role = "Researcher", backstory = "Expert analyst", task = "Research the topic")
@CrewMember(role = "Writer", backstory = "Technical writer", task = "Write a report")
@CrewMember(role = "Reviewer", backstory = "QA specialist", task = "Review the output")
public class ResearchCrew { }

@Agent(model = "llama3", architecture = Architecture.PIPELINE, quality = InferenceQuality.LOW)
@PipelineStep(name = "summarize", systemPrompt = "Summarize.", order = 1)
@PipelineStep(name = "translate", systemPrompt = "Translate to Italian.", order = 2)
public class TranslationPipeline { }

@Agent(model = "llama3", architecture = Architecture.DEBATE, quality = InferenceQuality.HIGH)
@DebateAgent(name = "Optimist", personality = "Always sees the bright side")
@DebateAgent(name = "Skeptic", personality = "Questions everything")
public class StrategyDebate { }
```

## Module Structure

```
quarkus-agent-extension/
├── runtime/          # CDI beans, DTOs, 14 engines, 21 providers, annotations
├── deployment/       # Build-time processor, Jandex scanning
├── integration-tests/# 50 @QuarkusTest + Ollama live tests
├── docker-compose.yml# SonarQube for quality gates
└── docs/             # Extension guide (AsciiDoc)
```

## Development

```bash
# Build & test (50 tests, excludes Ollama live)
mvn clean install -DexcludedGroups=ollama

# Run with real Ollama
mvn test -pl integration-tests -Dgroups=ollama

# SonarQube analysis
docker-compose up -d
mvn sonar:sonar -Dsonar.login=admin -Dsonar.password=admin
```

## Requirements

- Java 21+
- Maven 3.9+
- Docker (for tool execution sandbox and SonarQube)
- Ollama (optional, for live LLM tests)

## License

Apache License 2.0
