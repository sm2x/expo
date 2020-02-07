package expo.modules.notifications.notifications.scheduling;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;
import org.unimodules.core.interfaces.SingletonModule;

import expo.modules.notifications.notifications.ExpoNotificationsService;
import expo.modules.notifications.notifications.interfaces.DateTimeTrigger;
import expo.modules.notifications.notifications.interfaces.NotificationScheduler;

import static expo.modules.notifications.notifications.ExpoNotificationsService.enqueueRemove;
import static expo.modules.notifications.notifications.ExpoNotificationsService.enqueueSchedule;

public class ExpoNotificationScheduler implements SingletonModule, NotificationScheduler {
  private static final String SINGLETON_NAME = "NotificationScheduler";

  private Context mContext;

  public ExpoNotificationScheduler(Context context) {
    mContext = context;

    // Registers this singleton instance in static ExpoNotificationsService listeners collection.
    // Since it doesn't hold strong reference to the object this should be safe.
    ExpoNotificationsService.addListener(this);
  }

  @Override
  public String getName() {
    return SINGLETON_NAME;
  }

  public void onNotificationFired(String identifier, JSONObject request, DateTimeTrigger trigger) {
    Log.e("EXPO_NOTIFICATIONS", "Informing JS of a new notification! " + request.toString());
  }

  public boolean scheduleNotification(String identifier, JSONObject notificationRequest, DateTimeTrigger trigger) {
    return enqueueSchedule(mContext, identifier, notificationRequest, trigger);
  }

  public boolean cancelNotification(String identifier) {
    return enqueueRemove(mContext, identifier);
  }
}
