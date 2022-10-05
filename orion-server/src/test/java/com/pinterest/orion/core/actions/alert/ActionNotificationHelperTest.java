package com.pinterest.orion.core.actions.alert;

import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.ActionEngine;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ActionNotificationHelperTest {

    @Test
    public void testNoNotification() throws Exception {
        Action action = Mockito.mock(Action.class);
        Logger logger = Mockito.mock(Logger.class);
        ActionEngine engine =  Mockito.mock(ActionEngine.class);
        // Mock action
        Mockito.when(action.logger()).thenReturn(logger);
        Mockito.when(action.getEngine()).thenReturn(engine);
        // Create a config
        Map<String, Object> emptyConfig = new HashMap<>();
        // Create notification helper from mock action and config
        ActionNotificationHelper notificationHelper = new ActionNotificationHelper(
                action, emptyConfig
        );
        // Validate sendNotifications method
        notificationHelper.sendNotifications();
        verify(action, times(0)).getEngine();
    }

    @Test
    public void testSendSlackNotification() throws Exception {
        Action action = Mockito.mock(Action.class);
        Logger logger = Mockito.mock(Logger.class);
        ActionEngine engine =  Mockito.mock(ActionEngine.class);
        // Mock action
        Mockito.when(action.logger()).thenReturn(logger);
        Mockito.when(action.getEngine()).thenReturn(engine);
        Mockito.when(action.getOwner()).thenReturn("test_owner");
        // Create a config
        List<String> testWebhookUrlList = new ArrayList<String>() {{
            add("test_webhook_url");
        }};
        Map<String, Object> config = new HashMap<String, Object>(){{
            put(ActionNotificationHelper.ATTR_SLACK_WEBHOOK_URL_LIST_KEY, testWebhookUrlList);
        }};
        // Create notification helper from mock action and config
        ActionNotificationHelper notificationHelper = new ActionNotificationHelper(
                action, config
        );
        // Test parameters
        notificationHelper.setAlertTitle("test_title");
        notificationHelper.setAlertMessage("test_message");
        assertEquals(testWebhookUrlList, notificationHelper.getSlackWebhookUrlList());
        // Create alerts and alert message
        AlertMessage testSlackAlertMessage = new AlertMessage(
                "test_title",
                "test_message",
                "test_owner"
        );
        SlackAlert testSlackAlert = new SlackAlert();
        testSlackAlert.setWebTargets(ActionNotificationHelper.getWebTargetsFromWebhookUrlList(testWebhookUrlList));
        // Validate sendNotifications method
        notificationHelper.sendNotifications();
        verify(action, times(1)).getEngine();
        verify(engine, times(1)).alert(testSlackAlert, testSlackAlertMessage);
    }

    @Test
    public void testHelperConstructor() throws Exception {
        Action action = Mockito.mock(Action.class);
        Logger logger = Mockito.mock(Logger.class);
        Mockito.when(action.logger()).thenReturn(logger);
        List<String> testWebhookUrlList = new ArrayList<String>();
        // WebhookUrlList is empty; helper is empty
        Map<String, Object> config = new HashMap<String, Object>(){{
            put(ActionNotificationHelper.ATTR_SLACK_WEBHOOK_URL_LIST_KEY, testWebhookUrlList);
        }};
        ActionNotificationHelper notificationHelper = new ActionNotificationHelper(
                action, config
        );
        assertEquals(new ArrayList<String>(), notificationHelper.getSlackWebhookUrlList());
        // WebhookUrlList only contains null value; helper is empty
        testWebhookUrlList.add(null);
        config = new HashMap<String, Object>(){{
            put(ActionNotificationHelper.ATTR_SLACK_WEBHOOK_URL_LIST_KEY, testWebhookUrlList);
        }};
        notificationHelper = new ActionNotificationHelper(
                action, config
        );
        assertEquals(new ArrayList<String>(), notificationHelper.getSlackWebhookUrlList());
        // WebhookUrlList contains both null and url; helper only keeps url
        testWebhookUrlList.add("test_url");
        config = new HashMap<String, Object>(){{
            put(ActionNotificationHelper.ATTR_SLACK_WEBHOOK_URL_LIST_KEY, testWebhookUrlList);
        }};
        notificationHelper = new ActionNotificationHelper(
                action, config
        );
        assertEquals(new ArrayList<String>() {{add("test_url");}}, notificationHelper.getSlackWebhookUrlList());
    }
}
