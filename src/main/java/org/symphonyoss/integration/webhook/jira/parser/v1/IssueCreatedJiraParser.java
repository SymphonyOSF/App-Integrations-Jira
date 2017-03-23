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

import static org.symphonyoss.integration.parser.ParserUtils.presentationFormat;
import static org.symphonyoss.integration.webhook.jira.JiraEventConstants.JIRA_ISSUE_CREATED;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.symphonyoss.integration.entity.EntityBuilder;
import org.symphonyoss.integration.exception.EntityXMLGeneratorException;
import org.symphonyoss.integration.parser.SafeString;
import org.symphonyoss.integration.webhook.jira.parser.JiraParser;
import org.symphonyoss.integration.webhook.jira.parser.JiraParserException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible to validate the event 'jira:issue_created' sent by JIRA Webhook.
 *
 * Created by rsanchez on 18/05/16.
 */
@Component
public class IssueCreatedJiraParser extends IssueJiraParser {

  /**
   * Formatted message expected by user
   */
  public static final String ISSUE_CREATED_FORMATTED_TEXT = "%s<br/>%s";

  /**
   * Issue action
   */
  public static final String ISSUE_CREATED_ACTION = "created";

  @Override
  public List<String> getEvents() {
    return Arrays.asList(JIRA_ISSUE_CREATED);
  }

  @Override
  public String parse(Map<String, String> parameters, JsonNode node) throws JiraParserException {
    String entityML = getEntityML(node);

    return entityML;
  }

  /**
   * Returns the EntityML for created issue
   * @param node
   * @return entityML
   * @throws JiraParserException
   */
  private String getEntityML(JsonNode node) throws JiraParserException {
    EntityBuilder builder = createBasicEntityBuilder(node, ISSUE_CREATED_ACTION);
    EntityBuilder issueBuilder = createBasicIssueEntityBuilder(node);

    SafeString presentationML = getPresentationML(node);

    try {
      return builder.presentationML(presentationML)
          .nestedEntity(issueBuilder.build())
          .generateXML();
    } catch (EntityXMLGeneratorException e) {
      throw new JiraParserException("Something went wrong while building the message for JIRA Issue Created event.", e);
    }
  }

  /**
   * Returns the presentationML for created issue.
   * @param node
   * @return presentationML
   * @throws JiraParserException
   */
  private SafeString getPresentationML(JsonNode node) throws JiraParserException {
    SafeString issueInfo = getIssueInfo(node, ISSUE_CREATED_ACTION);

    SafeString description = getDescriptionFormatted(node);
    SafeString assignee = getAssigneeWithMention(node);
    SafeString label = getLabelFormatted(node);
    SafeString epic = getEpicFormatted(node);
    SafeString priority = getPriorityFormatted(node);
    SafeString status = getStatusFormatted(node);

    SafeString presentationBody = getPresentationMLBody(assignee, label, epic, priority, status, description);

    return presentationFormat(ISSUE_CREATED_FORMATTED_TEXT, issueInfo, presentationBody);
  }



}
