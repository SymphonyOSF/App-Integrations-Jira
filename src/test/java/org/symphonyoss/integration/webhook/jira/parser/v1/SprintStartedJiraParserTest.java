/**
 * Copyright 2016-2017 Symphony Integrations - Symphony LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.symphonyoss.integration.webhook.jira.parser.v1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.symphonyoss.integration.webhook.jira.JiraEventConstants.USER_KEY_PARAMETER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.symphonyoss.integration.json.JsonUtils;
import org.symphonyoss.integration.webhook.jira.parser.JiraParser;
import org.symphonyoss.integration.webhook.jira.parser.JiraParserException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Test class to validate {@link SprintStartedJiraParser}
 * Created by rsanchez on 17/05/16.
 */
public class SprintStartedJiraParserTest {

  private static final String FILENAME = "jiraCallbackSampleSprintStarted.json";

  private JiraParser sprintStarted = new SprintStartedJiraParser();

  @Test
  public void testSprintStarted() throws IOException, JiraParserException {
    ClassLoader classLoader = getClass().getClassLoader();

    Map<String, String> parameters = new HashMap<>();
    parameters.put(USER_KEY_PARAMETER, "test");

    JsonNode node = JsonUtils.readTree(classLoader.getResourceAsStream(FILENAME));
    String result = sprintStarted.parse(parameters, node);

    assertNotNull(result);
    assertEquals("test <b>started Sample Sprint 5</b>", result);
  }

  @Test
  public void testSprintStartedWithoutUserKey() throws IOException, JiraParserException {
    ClassLoader classLoader = getClass().getClassLoader();

    Map<String, String> parameters = new HashMap<>();

    JsonNode node = JsonUtils.readTree(classLoader.getResourceAsStream(FILENAME));
    String result = sprintStarted.parse(parameters, node);

    assertNotNull(result);
    assertEquals(" <b>started Sample Sprint 5</b>", result);
  }

  @Test
  public void testSprintStartedWithoutSprintNode() throws IOException, JiraParserException {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(USER_KEY_PARAMETER, "test");

    JsonNode rootNode = new ObjectNode(JsonNodeFactory.instance);
    String result = sprintStarted.parse(parameters, rootNode);

    assertNotNull(result);
    assertEquals("test <b>started a sprint</b>", result);
  }

  @Test
  public void testSprintStartedWithoutSprintName() throws IOException, JiraParserException {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(USER_KEY_PARAMETER, "test");

    ObjectNode rootNode = new ObjectNode(JsonNodeFactory.instance);
    rootNode.putObject(SprintStartedJiraParser.SPRINT_STARTED_SPRINT_PATH);

    String result = sprintStarted.parse(parameters, rootNode);

    assertNotNull(result);
    assertEquals("test <b>started a sprint</b>", result);
  }

  @Test
  public void testSprintStartedWithEmptySprintName() throws IOException, JiraParserException {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(USER_KEY_PARAMETER, "test");

    ObjectNode rootNode = new ObjectNode(JsonNodeFactory.instance);
    ObjectNode sprintNode = rootNode.putObject(SprintStartedJiraParser.SPRINT_STARTED_SPRINT_PATH);
    sprintNode.put(SprintStartedJiraParser.SPRINT_STARTED_NAME_PATH, "");

    String result = sprintStarted.parse(parameters, rootNode);

    assertNotNull(result);
    assertEquals("test <b>started a sprint</b>", result);
  }
}
