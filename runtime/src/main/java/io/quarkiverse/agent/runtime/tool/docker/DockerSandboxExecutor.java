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
package io.quarkiverse.agent.runtime.tool.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import io.quarkiverse.agent.runtime.tool.ToolExecutor;
import io.quarkiverse.agent.runtime.tool.ToolRequest;
import io.quarkiverse.agent.runtime.tool.ToolResult;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Executes tools in ephemeral Docker containers using the Docker Java API.
 * Production-grade: no test library dependencies.
 */
@ApplicationScoped
public class DockerSandboxExecutor implements ToolExecutor {

    private final DockerClient dockerClient;

    public DockerSandboxExecutor() {
        var config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        var httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(10)
                .connectionTimeout(Duration.ofSeconds(5))
                .responseTimeout(Duration.ofSeconds(30))
                .build();
        this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        long startTime = System.currentTimeMillis();
        String containerId = null;

        try {
            // Pull image if not present
            try {
                dockerClient.inspectImageCmd(request.image()).exec();
            } catch (Exception e) {
                dockerClient.pullImageCmd(request.image())
                        .start().awaitCompletion(request.timeout().toSeconds(), TimeUnit.SECONDS);
            }

            // Create container
            var createCmd = dockerClient.createContainerCmd(request.image())
                    .withCmd(request.command())
                    .withHostConfig(HostConfig.newHostConfig()
                            .withAutoRemove(false));

            // Set environment
            if (!request.environment().isEmpty()) {
                var env = request.environment().entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .toArray(String[]::new);
                createCmd.withEnv(env);
            }

            CreateContainerResponse container = createCmd.exec();
            containerId = container.getId();

            // Start and wait
            dockerClient.startContainerCmd(containerId).exec();

            int exitCode = dockerClient.waitContainerCmd(containerId)
                    .start()
                    .awaitStatusCode(
                            (int) request.timeout().toSeconds(), TimeUnit.SECONDS);

            // Capture logs
            var stdout = new StringBuilder();
            var stderr = new StringBuilder();

            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true).withStdErr(true).withFollowStream(false)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            switch (frame.getStreamType()) {
                                case STDOUT -> stdout.append(new String(frame.getPayload()));
                                case STDERR -> stderr.append(new String(frame.getPayload()));
                                default -> { }
                            }
                        }
                    }).awaitCompletion(5, TimeUnit.SECONDS);

            long duration = System.currentTimeMillis() - startTime;
            return new ToolResult(exitCode, stdout.toString().trim(), stderr.toString().trim(), duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return new ToolResult(1, "", e.getMessage(), duration);
        } finally {
            if (containerId != null) {
                try {
                    dockerClient.removeContainerCmd(containerId).withForce(true).exec();
                } catch (Exception ignored) { }
            }
        }
    }
}
