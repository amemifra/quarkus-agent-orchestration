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
package io.quarkiverse.agent.runtime.tool;

/**
 * Executes tools/commands in an isolated environment.
 * Implementations may use Docker containers, local processes, etc.
 */
public interface ToolExecutor {

    /**
     * Execute the given tool request and return the result.
     *
     * @param request the tool execution request
     * @return the execution result with stdout, stderr, and exit code
     */
    ToolResult execute(ToolRequest request);
}
