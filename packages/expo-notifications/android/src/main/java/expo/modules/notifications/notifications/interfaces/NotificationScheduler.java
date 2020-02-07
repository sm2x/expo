package expo.modules.notifications.notifications.interfaces;

import org.json.JSONObject;

import java.io.IOException;

public interface NotificationScheduler {
  boolean scheduleNotification(String identifier, JSONObject notificationRequest, DateTimeTrigger trigger) throws IOException;
}
