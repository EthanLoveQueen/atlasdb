/**
 * Copyright 2016 Palantir Technologies
 *
 * Licensed under the BSD-3 License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.atlasdb.ete;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.jayway.awaitility.Awaitility;
import com.palantir.atlasdb.http.AtlasDbHttpClients;
import com.palantir.atlasdb.todo.ImmutableTodo;
import com.palantir.atlasdb.todo.Todo;
import com.palantir.atlasdb.todo.TodoResource;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.waiting.HealthCheck;
import com.palantir.docker.compose.connection.waiting.SuccessOrFailure;
import com.palantir.docker.compose.execution.DockerComposeExecArgument;
import com.palantir.docker.compose.execution.DockerComposeExecOption;
import com.palantir.giraffe.command.Command;
import com.palantir.giraffe.command.CommandResult;
import com.palantir.giraffe.command.Commands;
import com.palantir.giraffe.host.HostAccessors;
import com.palantir.giraffe.host.HostControlSystem;

public class CassandraMultinodeEteTest {

    private static final Gradle GRADLE_PREPARE_TASK =
            Gradle.ensureTaskHasRun(":atlasdb-ete-test-utils:buildCassandraImage");

    private static final int TIMELOCK_SERVER_PORT = 3828;

    private static final String CONTAINER_LOGS_DIRECTORY = "container-logs/cassandra-multinode";

    private static final List<String> CASSANDRA_NODES = ImmutableList.of("cassandra1", "cassandra2", "cassandra3");

    private static final long MAX_CASSANDRA_NODE_DOWN_MILLIS = 30000;
    private static final long MAX_CASSANDRA_NODES_RUNNING_MILLIS = 3000;

    private static final DockerComposeRule MULTINODE_CASSANDRA_SETUP = DockerComposeRule.builder()
            .file("docker-compose.multiple-cassandra.yml")
            .waitingForService("cassandra1", Container::areAllPortsOpen)
            .waitingForService("cassandra2", Container::areAllPortsOpen)
            .waitingForService("cassandra3", Container::areAllPortsOpen)
            .waitingForService("ete1", toBeReady())
            .saveLogsTo(CONTAINER_LOGS_DIRECTORY)
            .build();


    @ClassRule
    public static final RuleChain PREPARED_DOCKER_SETUP = RuleChain
            .outerRule(GRADLE_PREPARE_TASK)
            .around(MULTINODE_CASSANDRA_SETUP);

    @Test
    public void shouldRunTransactionsWithAllCassandraNodesRunningWithoutUnacceptableDelay()
            throws InterruptedException, IOException {
        TodoResource clientToSingleNode = createClientFor(TodoResource.class, asPort("ete1"));

        long transactionStartTime = System.currentTimeMillis();
        assertAddTodoTransactionWasSuccessful(clientToSingleNode);
        long transactionEndTime = System.currentTimeMillis();

        long transactionTimeWithAllNodesRunning = transactionEndTime - transactionStartTime;

        assertThat("transactionTimeWithAllNodesRunning",
                transactionTimeWithAllNodesRunning,
                is(lessThan(MAX_CASSANDRA_NODES_RUNNING_MILLIS)));

        String container = CASSANDRA_NODES.get(0);
        checkNodetoolStatus(MULTINODE_CASSANDRA_SETUP.containers().container(container), "UN", 3);
    }

    @Test
    public void shouldRunTransactionsAfterCassandraNodeIsShutDownWithoutUnacceptableDelay()
            throws InterruptedException {
        TodoResource clientToSingleNode = createClientFor(TodoResource.class, asPort("ete1"));

        assertAddTodoTransactionWasSuccessful(clientToSingleNode);

        String cassandraNodeToKill = getRandomCassandraNodeToShutdown();
        killCassandraContainer(cassandraNodeToKill);

        long transactionStartTime = System.currentTimeMillis();
        assertAddTodoTransactionWasSuccessful(clientToSingleNode);
        long transactionEndTime = System.currentTimeMillis();

        long transactionTimeAfterNodeIsKilled = transactionEndTime - transactionStartTime;

        startCassandraContainer(cassandraNodeToKill);

        assertThat("transactionTimeAfterNodeIsKilled",
                transactionTimeAfterNodeIsKilled,
                is(lessThan(MAX_CASSANDRA_NODE_DOWN_MILLIS)));
    }

    private static DockerPort asPort(String node) {
        return MULTINODE_CASSANDRA_SETUP.containers().container(node).port(TIMELOCK_SERVER_PORT);
    }

    private static <T> T createClientFor(Class<T> clazz, DockerPort port) {
        String uri = port.inFormat("http://$HOST:$EXTERNAL_PORT");
        return AtlasDbHttpClients.createProxy(Optional.absent(), uri, clazz);
    }

    private static HealthCheck<Container> toBeReady() {
        return (container) -> {
            TodoResource todos = createClientFor(TodoResource.class, container.port(TIMELOCK_SERVER_PORT));

            return SuccessOrFailure.onResultOf(() -> {
                todos.isHealthy();
                return true;
            });
        };
    }

    private String getRandomCassandraNodeToShutdown() {
        int index = ThreadLocalRandom.current().nextInt(CASSANDRA_NODES.size());
        return CASSANDRA_NODES.get(index);
    }

    private static void killCassandraContainer(String containerName) {
        Container container = MULTINODE_CASSANDRA_SETUP.containers().container(containerName);
        try {
            container.kill();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void startCassandraContainer(String containerName) throws InterruptedException {
        Container container = MULTINODE_CASSANDRA_SETUP.containers().container(containerName);
        try {
            container.start();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        waitForAllPorts(container);
        waitForNodetoolToConfirmStatus(container, "UN", 3);
    }

    private static void waitForAllPorts(Container container) {
        Awaitility.await()
                .atMost(50, TimeUnit.SECONDS)
                .until(() -> container.areAllPortsOpen().succeeded());
    }

    private void assertAddTodoTransactionWasSuccessful(TodoResource todoClient) {
        Todo todo = ImmutableTodo.of("some unique TODO item with UUID=" + UUID.randomUUID());

        todoClient.addTodo(todo);
        List<Todo> todoList = todoClient.getTodoList();

        assertThat(todoList, hasItem(todo));
    }

    private void waitForNodetoolToConfirmStatus(Container container, String status, int expectedNodeCount) {
        Awaitility.await()
                .atMost(360, TimeUnit.SECONDS)
                .pollInterval(5, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        return checkNodetoolStatus(container, status, expectedNodeCount);
                    } catch (Exception e) {
                        return false;
                    }
                });
    }

    private Boolean checkNodetoolStatus(Container container, String status, int expectedNodeCount)
            throws IOException, InterruptedException {
        return checkNodetoolStatusWithGiraffe(container.getContainerName(), status, expectedNodeCount)
            || checkNodetoolStatusWithDockerExec(container, status, expectedNodeCount);
    }

    // Works on circle
    private Boolean checkNodetoolStatusWithGiraffe(String containerName, String status, int expectedNodeCount) {
        HostControlSystem hcs = HostAccessors.getDefault().open();
        Command command = hcs.getCommand(String.format(
                "sudo lxc-attach -n \"$(docker inspect --format \"{{.Id}}\" %s)\" -- bash -c nodetool status",
                containerName));
        try {
            CommandResult cr = Commands.execute(command);
            return StringUtils.countMatches(cr.getStdOut(), status) == expectedNodeCount;
        } catch (IOException e) {
            System.out.println("Failed to execute Giraffe command. Exception: " + e);
            return false;
        }
    }

    // Works locally
    private Boolean checkNodetoolStatusWithDockerExec(Container container, String status, int expectedNodeCount)
            throws IOException, InterruptedException {
        String nodetoolStatus = MULTINODE_CASSANDRA_SETUP.exec(
                DockerComposeExecOption.options("-T"),
                container.getContainerName(),
                DockerComposeExecArgument.arguments("bash", "-c", "nodetool status | grep " + status));
        return StringUtils.countMatches(nodetoolStatus, status) == expectedNodeCount;
    }
}