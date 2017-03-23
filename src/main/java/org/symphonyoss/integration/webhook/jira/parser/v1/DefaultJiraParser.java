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
import static org.symphonyoss.integration.webhook.jira.JiraEventConstants.WEBHOOK_EVENT;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.integration.json.JsonUtils;
import org.symphonyoss.integration.webhook.jira.parser.JiraParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Default handler for any not-specifically treated event.
 *
 * Created by mquilzini on 22/07/16.
 */
public class DefaultJiraParser extends CommonJiraParser {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultJiraParser.class);

  private static final String DEFAULT_ACTION_FORMATTED_TEXT = "%s %s";

  private static final String USER_KEY_PARAMETER = "user_key";

  private static final String TRANSLATOR_FILENAME = "jira_actions_translator.json";

  private JsonNode actionTranslatorJsonNode;

  public DefaultJiraParser() {
    initialize();
  }

  private void initialize() {
    try (InputStream input = getClass().getClassLoader().getResourceAsStream(TRANSLATOR_FILENAME)) {
      this.actionTranslatorJsonNode = JsonUtils.readTree(input);
    } catch (IOException e) {
      LOG.error("Failed to load resource for event translation", e);
      this.actionTranslatorJsonNode = new ObjectNode(JsonNodeFactory.instance);
    }
  }

  @Override
  public String parse(Map<String, String> parameters, JsonNode node) throws JiraParserException {
    String webhookEvent = node.path(WEBHOOK_EVENT).asText();
    if (webhookEvent.isEmpty()) {
      throw new JiraParserException("Json received does not have an identifiable event. Info: " + node.toString());
    }

    String user = "";
    String action;

    if (parameters != null) {
      user = parameters.containsKey(USER_KEY_PARAMETER) ? parameters.get(USER_KEY_PARAMETER) : "";
    }
    action = this.actionTranslatorJsonNode.path(webhookEvent).asText();

    if (action.isEmpty()) {
      action = webhookEvent;
    }

    return presentationFormat(DEFAULT_ACTION_FORMATTED_TEXT, user, action).toString();
  }
}
