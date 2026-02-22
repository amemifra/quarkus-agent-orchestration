package io.quarkiverse.agent.runtime.declarative;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeclarativeAgentParser {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    // Regex to match YAML frontmatter between "---" blocks and capture the rest as
    // markdown body
    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile("^---\r?\n(.*?)\r?\n---\r?\n(.*)$",
            Pattern.DOTALL);

    public static DeclarativeAgentConfig parseFile(Path path) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        return parse(content, path.getFileName().toString());
    }

    public static DeclarativeAgentConfig parseClasspath(String resourcePath) throws IOException {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Resource not found in classpath: " + resourcePath);
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return parse(content, resourcePath);
        }
    }

    private static DeclarativeAgentConfig parse(String content, String fileName) throws IOException {
        String lowerName = fileName.toLowerCase();

        if (lowerName.endsWith(".md") || lowerName.endsWith(".markdown")) {
            return parseMarkdown(content);
        } else if (lowerName.endsWith(".yaml") || lowerName.endsWith(".yml")) {
            return YAML_MAPPER.readValue(content, DeclarativeAgentConfig.class);
        } else if (lowerName.endsWith(".json")) {
            return JSON_MAPPER.readValue(content, DeclarativeAgentConfig.class);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported file extension. Must be .yaml, .yml, .json, or .md: " + fileName);
        }
    }

    private static DeclarativeAgentConfig parseMarkdown(String content) throws IOException {
        Matcher matcher = FRONTMATTER_PATTERN.matcher(content);
        if (matcher.find()) {
            String yamlPart = matcher.group(1);
            String bodyPart = matcher.group(2).trim();

            DeclarativeAgentConfig config = YAML_MAPPER.readValue(yamlPart, DeclarativeAgentConfig.class);
            // If the user didn't explicitly set a systemPrompt in the YAML, inject the
            // Markdown body
            if ((config.systemPrompt == null || config.systemPrompt.isBlank()) && !bodyPart.isEmpty()) {
                config.systemPrompt = bodyPart;
            }
            return config;
        } else {
            throw new IllegalArgumentException(
                    "Markdown file does not contain valid YAML frontmatter between '---' markers.");
        }
    }
}
