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

package org.symphonyoss.integration.webhook.jira;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.symphonyoss.integration.messageml.MessageMLFormatConstants.MESSAGEML_END;
import static org.symphonyoss.integration.messageml.MessageMLFormatConstants.MESSAGEML_START;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.symphonyoss.integration.entity.model.User;
import org.symphonyoss.integration.json.JsonUtils;
import org.symphonyoss.integration.model.config.IntegrationSettings;
import org.symphonyoss.integration.service.UserService;
import org.symphonyoss.integration.webhook.WebHookPayload;
import org.symphonyoss.integration.webhook.exception.WebHookParseException;
import org.symphonyoss.integration.webhook.jira.parser.JiraParserException;
import org.symphonyoss.integration.webhook.jira.parser.JiraParserResolver;
import org.symphonyoss.integration.webhook.jira.parser.ParserFactory;
import org.symphonyoss.integration.webhook.jira.parser.v1.CommentJiraParser;
import org.symphonyoss.integration.webhook.jira.parser.v1.IssueCreatedJiraParser;
import org.symphonyoss.integration.webhook.jira.parser.v1.IssueUpdatedJiraParser;
import org.symphonyoss.integration.webhook.jira.parser.v1.NullJiraParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@link JiraWebHookIntegration}.
 *
 * Created by mquilzini on 11/05/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class JiraWebHookIntegrationTest {

  private static final String ISSUE_CREATED_FILENAME =
      "parser/issueCreatedJiraParser/jiraCallbackSampleIssueCreated.json";

  private static final String ISSUE_UPDATED_FILENAME =
      "parser/issueUpdatedJiraParser/jiraCallbackSampleIssueUpdated.json";

  private static final String ISSUE_UPDATED_WITHOUT_HASHTAG =
      "parser/issueUpdatedJiraParser/jiraCallbackSampleIssueUpdatedWithoutHashTagLabel.json";

  private static final String COMMENT_ADDED_FILENAME = "parser/commentJiraParser/jiraCallbackSampleCommentAdded.json";

  private static final String COMMENT_ADDED_WITH_MENTION_FILENAME =
      "parser/commentJiraParser/jiraCallbackSampleCommentAddedWithMention.json";

  private static final String COMMENT_UPDATED_FILENAME =
      "parser/commentJiraParser/jiraCallbackSampleCommentUpdated.json";

  private static final String COMMENT_UPDATED_WITH_MENTION_FILENAME =
      "parser/commentJiraParser/jiraCallbackSampleCommentUpdatedWithMention.json";

  private static final String COMMENT_DELETED_FILENAME =
      "parser/commentJiraParser/jiraCallbackSampleCommentDeleted.json";

  @Spy
  private List<ParserFactory> factories = new ArrayList<>();

  @Mock
  private ParserFactory factory;

  @Mock
  private JiraParserResolver parserResolver;

  @InjectMocks
  private JiraWebHookIntegration jiraWhi = new JiraWebHookIntegration();

  @Mock
  private NullJiraParser defaultJiraParser = new NullJiraParser();

  @Mock
  private UserService userService;

  @InjectMocks
  private IssueCreatedJiraParser issueCreatedJiraParser = new IssueCreatedJiraParser();

  @InjectMocks
  private CommentJiraParser commentJiraParser = new CommentJiraParser();

  @InjectMocks
  private IssueUpdatedJiraParser issueUpdatedJiraParser = new IssueUpdatedJiraParser();

  @Before
  public void setup() {
    mockUsers("test@symphony.com", "test2@symphony.com", "mquilzini@symphony.com",
        "ppires@symphony.com");

    factories.add(factory);

    doReturn(factory).when(parserResolver).getFactory();
  }

  @Test
  public void testOnConfigChange() {
    IntegrationSettings settings = new IntegrationSettings();

    jiraWhi.onConfigChange(settings);

    verify(factory, times(1)).onConfigChange(settings);
  }

  @Test
  public void testIssueCreated() throws IOException, WebHookParseException {
    doReturn(issueCreatedJiraParser).when(factory).getParser(any(JsonNode.class));

    String body = getBody(ISSUE_CREATED_FILENAME);

    WebHookPayload payload = new WebHookPayload(Collections.<String, String>emptyMap(), Collections.<String, String>emptyMap(), body);
    String result = jiraWhi.parse(payload);

    assertNotNull(result);

    String expected = readFileAppendingMessageMLTag("parser/issueCreatedJiraParser/jiraMessageMLIssueCreated.xml");

    assertEquals(expected, result);
  }

  @Test
  public void testIssueUpdated() throws IOException, WebHookParseException {
    testIssueUpdated(ISSUE_UPDATED_FILENAME);
  }

  @Test
  public void testIssueUpdatedWithoutHashTag() throws IOException, WebHookParseException {
    testIssueUpdated(ISSUE_UPDATED_WITHOUT_HASHTAG);
  }

  private void testIssueUpdated(String filename) throws IOException {
    doReturn(issueUpdatedJiraParser).when(factory).getParser(any(JsonNode.class));

    String expected = readFileAppendingMessageMLTag("parser/issueUpdatedJiraParser/jiraMessageMLIssueUpdated.xml");
    String body = getBody(filename);
    WebHookPayload payload = new WebHookPayload(Collections.<String, String>emptyMap(), Collections.<String, String>emptyMap(), body);

    String result = jiraWhi.parse(payload);

    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void testCommentAdded() throws IOException, WebHookParseException {
    doReturn(commentJiraParser).when(factory).getParser(any(JsonNode.class));

    String body = getBody(COMMENT_ADDED_FILENAME);

    WebHookPayload payload = new WebHookPayload(Collections.<String, String>emptyMap(), Collections.<String, String>emptyMap(), body);
    String result = jiraWhi.parse(payload);

    assertNotNull(result);

    String expected = readFileAppendingMessageMLTag("parser/commentJiraParser/jiraMessageMLIssueCommented.xml");

    assertEquals(expected, result);
  }

  @Test
  public void testCommentAddedWithMention() throws IOException, WebHookParseException {
    doReturn(commentJiraParser).when(factory).getParser(any(JsonNode.class));

    String body = getBody(COMMENT_ADDED_WITH_MENTION_FILENAME);

    User user = new User();
    user.setEmailAddress("integrationuser@symphony.com");
    user.setId(123L);
    user.setUserName("integrationuser");
    user.setDisplayName("Integration User");
    when(userService.getUserByUserName(anyString(), eq("integrationuser"))).thenReturn(user);

    WebHookPayload payload = new WebHookPayload(Collections.<String, String>emptyMap(), Collections.<String, String>emptyMap(), body);
    String result = jiraWhi.parse(payload);

    assertNotNull(result);

    String expected = readFileAppendingMessageMLTag(
        "parser/commentJiraParser/jiraMessageMLIssueCommentedWithMention.xml");

    assertEquals(expected, result);
  }

  @Test
  public void testCommentUpdated() throws IOException, WebHookParseException {
    doReturn(commentJiraParser).when(factory).getParser(any(JsonNode.class));

    String body = getBody(COMMENT_UPDATED_FILENAME);

    WebHookPayload payload = new WebHookPayload(Collections.<String, String>emptyMap(), Collections.<String, String>emptyMap(), body);
    String result = jiraWhi.parse(payload);

    assertNotNull(result);

    String expected = readFileAppendingMessageMLTag("parser/commentJiraParser/jiraMessageMLIssueCommentUpdated.xml");

    assertEquals(expected, result);
  }

  @Test
  public void testCommentUpdatedWithMention() throws IOException, WebHookParseException {
    doReturn(commentJiraParser).when(factory).getParser(any(JsonNode.class));

    String body = getBody(COMMENT_UPDATED_WITH_MENTION_FILENAME);

    User user = new User();
    user.setEmailAddress("integrationuser@symphony.com");
    user.setId(123L);
    user.setUserName("integrationuser");
    user.setDisplayName("Integration User");
    when(userService.getUserByUserName(anyString(), eq("integrationuser"))).thenReturn(user);

    WebHookPayload payload = new WebHookPayload(Collections.<String, String>emptyMap(), Collections.<String, String>emptyMap(), body);
    String result = jiraWhi.parse(payload);

    assertNotNull(result);

    String expected = readFileAppendingMessageMLTag(
        "parser/commentJiraParser/jiraMessageMLIssueCommentUpdatedWithMention.xml");

    assertEquals(expected, result);
  }

  @Test
  public void testCommentDeleted() throws IOException, WebHookParseException {
    doReturn(commentJiraParser).when(factory).getParser(any(JsonNode.class));

    String body = getBody(COMMENT_DELETED_FILENAME);

    WebHookPayload payload = new WebHookPayload(Collections.<String, String>emptyMap(), Collections.<String, String>emptyMap(), body);
    String result = jiraWhi.parse(payload);

    assertNotNull(result);

    String expected = readFileAppendingMessageMLTag("parser/commentJiraParser/jiraMessageMLIssueCommentDeleted.xml");

    assertEquals(expected, result);
  }

  @Test
  public void testNoEventPayload() throws WebHookParseException {
    doReturn(defaultJiraParser).when(factory).getParser(any(JsonNode.class));

    String body = "{ \"random\": \"json\" }";
    WebHookPayload payload = new WebHookPayload(Collections.<String, String>emptyMap(), Collections.<String, String>emptyMap(), body);
    assertNull(jiraWhi.parse(payload));
  }

  @Test(expected = JiraParserException.class)
  public void testFailReadingJSON() throws IOException, WebHookParseException {
    String body = "";

    WebHookPayload payload = new WebHookPayload(Collections.<String, String>emptyMap(), Collections.<String, String>emptyMap(), body);
    jiraWhi.parse(payload);
  }

  private String getBody(String filename) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    JsonNode node = JsonUtils.readTree(classLoader.getResourceAsStream(filename));
    return JsonUtils.writeValueAsString(node);
  }

  private String readFile(String fileName) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    String expected =
        FileUtils.readFileToString(new File(classLoader.getResource(fileName).getPath()));
    return expected = expected.replaceAll("\n", "");
  }

  private String readFileAppendingMessageMLTag(String fileName) throws IOException {
    return MESSAGEML_START + readFile(fileName) + MESSAGEML_END;
  }

  private void mockUsers(String... emails) {
    for (String email : emails) {
      User user = new User();
      user.setEmailAddress(email);
      when(userService.getUserByEmail(anyString(), eq(email))).thenReturn(user);
    }
  }
}
