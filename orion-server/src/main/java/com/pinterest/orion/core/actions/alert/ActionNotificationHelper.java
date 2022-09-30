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
    private Action action;
    private String alertTitle;
    private String alertMessage;
    private boolean isSendingSlackNotification = false;
    private List<String> slackWebhookUrlList;
    private Logger logger;

    public ActionNotificationHelper(Action action, Map<String, Object> config) {
        // Use action and its config to construct ActionNotificationHelper.
        setAction(action);
        setAlertTitle(action.getName());
        setAlertMessage(action.getName());
        setLogger(action.logger());
        // Config determines which types of notification will be sent. Can add more types of notifications here.
        if (config.containsKey(ATTR_SLACK_WEBHOOK_URL_LIST_KEY)) {
            Object slackWebhookUrlListObject = config.get(ATTR_SLACK_WEBHOOK_URL_LIST_KEY);
            if (slackWebhookUrlListObject != null) {
                if (slackWebhookUrlListObject instanceof List) {
                    this.isSendingSlackNotification = true;
                    this.slackWebhookUrlList = (List<String>) config.get(ATTR_SLACK_WEBHOOK_URL_LIST_KEY);
                } else {
                    // Log warn if slackWebhookUrlList cannot be parsed from config.
                    getLogger().log(Level.WARNING, ATTR_SLACK_WEBHOOK_URL_LIST_KEY + " value is unacceptable type.");
                }
            } else {
                getLogger().log(Level.WARNING, ATTR_SLACK_WEBHOOK_URL_LIST_KEY + " value is null.");
            }
        }
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public void setAlertTitle(String alertTitle) {
        // Call this in Action class if you need personalized alert title.
        this.alertTitle = alertTitle;
    }

    public void setAlertMessage(String alertMessage) {
        // Call this in Action class if you need personalized alert message.
        this.alertMessage = alertMessage;
    }

    public void setLogger(Logger logger) {
        // Use logger from action. If action does not have logger, create one.
        if (logger != null) {
            this.logger = logger;
        } else {
            this.logger = Logger.getLogger(this.getClass().getName());
        }
    }

    public Action getAction() {
        return this.action;
    }

    public String getAlertTitle(){
        return this.alertTitle;
    }

    public String getAlertMessage() {
        return this.alertMessage;
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
            getLogger().log(Level.WARNING, "ActionNotificationHelper fails to get web targets: " + e);
            return;
        }
        AlertMessage slackAlertMessage = new AlertMessage(
                getAlertTitle(),
                getAlertMessage(),
                getAction().getOwner()
        );
        try {
            getAction().getEngine().alert(slackAlert, slackAlertMessage);
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "ActionNotificationHelper fails to send Slack message: " + e);
        }
    }

    public void sendNotifications() {
        if (isSendingSlackNotification) {
            sendSlackNotifications();
        }
    }
}
