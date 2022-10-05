package com.pinterest.orion.core.actions.alert;

import com.pinterest.orion.core.actions.Action;
import org.glassfish.jersey.client.JerseyClientBuilder;

import javax.ws.rs.client.WebTarget;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActionNotificationHelper {

    public static final String ATTR_SLACK_WEBHOOK_URL_LIST_KEY = "slackWebhookUrlList";
    public static final String LOGGING_PREFIX = "[ActionNotificationHelper] ";
    private Action action;
    private String alertTitle;
    private String alertMessage;
    private String alertOwner;
    private boolean isSendingSlackNotification = false;
    private List<String> slackWebhookUrlList = new ArrayList<String>();
    private Logger logger;

    public ActionNotificationHelper(Action action, Map<String, Object> config) {
        // Use action and its config to construct ActionNotificationHelper.
        setAction(action);
        setLogger(action.logger());
        // Config determines which types of notification will be sent. Can add more types of notifications here.
        if (config.containsKey(ATTR_SLACK_WEBHOOK_URL_LIST_KEY)) {
            Object slackWebhookUrlListObject = config.get(ATTR_SLACK_WEBHOOK_URL_LIST_KEY);
            if (slackWebhookUrlListObject != null) {
                if (slackWebhookUrlListObject instanceof List) {
                    List<String> tmpSlackWebhookUrlList = (List<String>) config.get(ATTR_SLACK_WEBHOOK_URL_LIST_KEY);
                    for (int i = 0; i < tmpSlackWebhookUrlList.size(); i++) {
                        String slackWebhookUrl = tmpSlackWebhookUrlList.get(i);
                        if (slackWebhookUrl != null) {
                            this.slackWebhookUrlList.add(slackWebhookUrl);
                        }
                    }
                    if (this.slackWebhookUrlList.size() > 0) {
                        this.isSendingSlackNotification = true;
                        if (this.slackWebhookUrlList.size() < tmpSlackWebhookUrlList.size()) {
                            // Null value is detected in config list. Still sends notifications for other urls.
                            getLogger().log(Level.WARNING,
                                    LOGGING_PREFIX + ATTR_SLACK_WEBHOOK_URL_LIST_KEY + " contains null value.");
                        }
                    } else {
                        // Config list only contains null value.
                        getLogger().log(Level.WARNING,
                                LOGGING_PREFIX + ATTR_SLACK_WEBHOOK_URL_LIST_KEY + " only contains null value.");
                    }
                } else {
                    // Log warn if slackWebhookUrlList cannot be parsed from config.
                    getLogger().log(Level.WARNING,
                            LOGGING_PREFIX + ATTR_SLACK_WEBHOOK_URL_LIST_KEY + " value is unacceptable type.");
                }
            } else {
                getLogger().log(Level.WARNING,
                        LOGGING_PREFIX + ATTR_SLACK_WEBHOOK_URL_LIST_KEY + " value is null.");
            }
        }
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public void setAlertTitle(String alertTitle) {
        this.alertTitle = alertTitle;
    }

    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
    }

    public void setAlertOwner(String alertOwner) {
        this.alertOwner = alertOwner;
    }

    public void setLogger(Logger logger) {
        // Use logger from action. If action does not have logger, create one.
        if (logger != null) {
            this.logger = logger;
        } else {
            this.logger = Logger.getLogger(this.getClass().getName());
        }
    }

    public boolean isSendingNotifications() {
        return this.isSendingSlackNotification;
    }

    public Action getAction() {
        return this.action;
    }

    public String getAlertTitle(){
        if (this.alertTitle == null) {
            // Use action name by default
            return getAction().getName();
        }
        return this.alertTitle;
    }

    public String getAlertMessage() {
        if (this.alertMessage == null) {
            // Use action name by default
            return getAction().getName();
        }
        return this.alertMessage;
    }

    public String getAlertOwner() {
        if (this.alertOwner == null) {
            // Use action owner by default
            return getAction().getOwner();
        }
        return this.alertOwner;
    }

    public Logger getLogger() {
        return this.logger;
    }

    public List<String> getSlackWebhookUrlList() {
        return this.slackWebhookUrlList;
    }

    protected static List<WebTarget> getWebTargetsFromWebhookUrlList(List<String> webhookUrlList) {
        // Transfer webhook urls (String) to web targets (WebTarget)
        List<WebTarget> webTargets = new ArrayList<>();
        for (int i = 0; i < webhookUrlList.size(); i++) {
            WebTarget webTarget = JerseyClientBuilder.newClient().target(webhookUrlList.get(i));
            webTargets.add(webTarget);
        }
        return webTargets;
    }

    private void sendSlackNotifications() {
        SlackAlert slackAlert = new SlackAlert();
        try {
            List<WebTarget> webTargets = getWebTargetsFromWebhookUrlList(getSlackWebhookUrlList());
            slackAlert.setWebTargets(webTargets);
        } catch (Exception e) {
            getLogger().log(Level.WARNING,
                    LOGGING_PREFIX + "Fail to get web targets: " + e);
            return;
        }
        AlertMessage slackAlertMessage = new AlertMessage(
                getAlertTitle(),
                getAlertMessage(),
                getAlertOwner()
        );
        try {
            getAction().getEngine().alert(slackAlert, slackAlertMessage);
        } catch (Exception e) {
            getLogger().log(Level.WARNING,
                    LOGGING_PREFIX + "Fail to send Slack message: " + e);
        }
    }

    public void sendNotifications() {
        if (getAction() == null || getAction().getEngine() == null) {
            getLogger().log(Level.WARNING,
                    LOGGING_PREFIX + "Action engine has not been initialized. Cannot send message.");
        } else if (isSendingSlackNotification) {
            sendSlackNotifications();
        }
    }
}
