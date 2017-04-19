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

package org.symphonyoss.integration.webhook.jira.parser.v2;

import static org.symphonyoss.integration.webhook.jira.JiraEventConstants.ISSUE_EVENT_TYPE_NAME;
import static org.symphonyoss.integration.webhook.jira.JiraEventConstants.WEBHOOK_EVENT;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.symphonyoss.integration.model.message.MessageMLVersion;
import org.symphonyoss.integration.webhook.jira.parser.JiraParser;
import org.symphonyoss.integration.webhook.jira.parser.JiraParserFactory;
import org.symphonyoss.integration.webhook.jira.parser.v1.V1ParserFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser factory for the MessageML v2.
 *
 * Created by rsanchez on 21/03/17.
 */
@Component
public class V2ParserFactory extends JiraParserFactory {

  @Autowired
  private List<JiraMetadataParser> beans;

  @Autowired
  private V1ParserFactory fallbackFactory;

  @Override
  public boolean accept(MessageMLVersion version) {
    return MessageMLVersion.V2.equals(version);
  }

  @Override
  protected List<JiraParser> getBeans() {
    return new ArrayList<JiraParser>(beans);
  }

  @Override
  public JiraParser getParser(JsonNode node) {
    JiraParser result = super.getParser(node);

    if (result == null) {
      // Fallback use V1 Factory
      return fallbackFactory.getParser(node);
    }

    return result;
  }
}
