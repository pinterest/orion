/*******************************************************************************
 * Copyright 2020 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.pinterest.orion.core.actions.alert;

import org.simplejavamail.api.email.Email;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;

import com.pinterest.orion.core.PluginConfigurationException;

import java.util.Map;
import java.util.logging.Logger;

public class LocalhostEmailAlertAction extends AlertAction {
  private static final Logger logger = Logger.getLogger(LocalhostEmailAlertAction.class.getName());
  public static final String CONF_FROM_KEY = "from";
  private String fromAddress;

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    super.initialize(config);
    if (!config.containsKey(CONF_FROM_KEY)) {
      throw new PluginConfigurationException("Missing config " + CONF_FROM_KEY);
    }
    fromAddress = config.get(CONF_FROM_KEY).toString();
  }

  @Override
  public void alert(AlertMessage message) {
    Email email = EmailBuilder.startingBlank()
        .from(fromAddress)
        .to(message.getOwner())
        .withSubject(message.getTitle())
        .withPlainText(message.getBody())
        .buildEmail();
    try {
      MailerBuilder
          .withSMTPServer("localhost", 25)
          .buildMailer()
          .sendMail(email);

    } catch (Exception e) {
      logger.warning("Failed to send out email: " + e);
    }
  }

  @Override
  public String getName() {
    return "Email Alert";
  }

}
