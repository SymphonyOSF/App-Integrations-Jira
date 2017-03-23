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

package org.symphonyoss.integration.webhook.jira.parser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.symphonyoss.integration.event.HealthCheckServiceEvent;
import org.symphonyoss.integration.event.MessageMLVersionUpdatedEvent;
import org.symphonyoss.integration.model.message.MessageMLVersion;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Resolves the parser factory based on MessageML version.
 * Created by rsanchez on 22/03/17.
 */
@Component
public class JiraParserResolver {

  private static final String AGENT_SERVICE_NAME = "Agent";

  private static final Long INITIAL_DELAY = TimeUnit.SECONDS.toMillis(5);

  private static final Long DELAY = TimeUnit.MINUTES.toMillis(1);

  @Autowired
  private ApplicationEventPublisher publisher;

  @Autowired
  private List<ParserFactory> factories;

  private ParserFactory factory;

  private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  /**
   * Initialize the default parser factory.
   */
  @PostConstruct
  public void init() {
    setupParserFactory(MessageMLVersion.V1);
  }

  /**
   * Setup the parser factory based on messageML version.
   */
  private void setupParserFactory(MessageMLVersion version) {
    for (ParserFactory factory : factories) {
      if (factory.accept(version)) {
        this.factory = factory;
        break;
      }
    }
  }

  /**
   * Schedule to dispatch health-check service event to monitor the Agent version.
   */
  public void healthCheckAgentService() {
    scheduler.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        HealthCheckServiceEvent event = new HealthCheckServiceEvent(AGENT_SERVICE_NAME);
        publisher.publishEvent(event);
      }
    }, INITIAL_DELAY, DELAY, TimeUnit.MILLISECONDS);
  }

  /**
   * Handle events related to update MessageML version. If the new version of MessageML is V2 I can
   * stop the scheduler to check the version, otherwise I need to reschedule the monitoring process.
   * @param event MessageML version update event
   */
  @EventListener
  public void handleMessageMLVersionUpdatedEvent(MessageMLVersionUpdatedEvent event) {
    MessageMLVersion version = event.getVersion();

    if (MessageMLVersion.V2.equals(version)) {
      stopScheduler();
    } else if (scheduler.isTerminated()) {
      scheduler = Executors.newSingleThreadScheduledExecutor();
      healthCheckAgentService();
    }

    setupParserFactory(version);
  }

  /**
   * Stop the monitoring process.
   */
  @PreDestroy
  public void stopScheduler() {
    scheduler.shutdown();
  }

  public ParserFactory getFactory() {
    return factory;
  }

}
