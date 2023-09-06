package com.pinterest.orion.core.actions.memq;

import java.util.logging.Logger;

import com.pinterest.orion.core.actions.Action;

public class MemqTopicCreationAction extends Action {

  public static final String ATTR_TOPIC_NAME_KEY = "topic";
  public static final String ATTR_TOPIC_CONFIG_KEY = "topicConfig";
  private static final Logger logger = Logger.getLogger(MemqTopicCreationAction.class.getCanonicalName());
  private static final String[] REQUIRED_ARG_KEYS = new String[] { ATTR_TOPIC_NAME_KEY,
      ATTR_TOPIC_CONFIG_KEY };

  @Override
  public String getName() {
    return "Create Topic " + getAttribute(this, ATTR_TOPIC_NAME_KEY).getValue().toString();
  }

  @Override
  public void runAction() throws Exception {

  }

  public static void main(String[] args) throws Exception {

  }
}
