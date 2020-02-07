package expo.modules.notifications.notifications;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.WeakHashMap;

import androidx.core.app.AlarmManagerCompat;
import expo.modules.notifications.notifications.interfaces.DateTimeTrigger;
import expo.modules.notifications.notifications.scheduling.ExpoNotificationScheduler;

public class ExpoNotificationsService extends IntentService {
  private static final String TAG = "ExpoNotifications";

  // Known intent actions
  public static final String SCHEDULE_ACTION = "expo.modules.notifications.intents.SCHEDULE";
  public static final String TRIGGER_ACTION = "expo.modules.notifications.intents.TRIGGER";
  public static final String REMOVE_ACTION = "expo.modules.notifications.intents.REMOVE";

  // Intent extras keys
  private static final String NOTIFICATION_IDENTIFIER_KEY = "id";
  private static final String NOTIFICATION_REQUEST_KEY = "request";
  private static final String NOTIFICATION_TRIGGER_KEY = "trigger";

  //                                      EXPO NOTIFICATIONS
  private static final int REQUEST_CODE = 397606684;

  // Unfortunately we cannot save state between instances of a service other way
  // than by static properties. Fortunately, using weak references we can
  // be somehow sure instances of PushTokenListeners won't be leaked by this component.

  /**
   * A weak map of listeners -> reference. Used to check quickly whether given listener
   * is already registered and to iterate over when notifying of new token.
   */
  private static WeakHashMap<ExpoNotificationScheduler, WeakReference<ExpoNotificationScheduler>> sListenersReferences = new WeakHashMap<>();

  //region Public API

  public static void addListener(ExpoNotificationScheduler listener) {
    // Checks whether this listener has already been registered
    if (!sListenersReferences.containsKey(listener)) {
      WeakReference<ExpoNotificationScheduler> listenerReference = new WeakReference<>(listener);
      sListenersReferences.put(listener, listenerReference);
    }
  }

  public static boolean enqueueSchedule(Context context, String identifier, JSONObject notificationRequest, DateTimeTrigger trigger) {
    return context.startService(getScheduleIntent(context, identifier, notificationRequest, trigger)) != null;
  }

  public static boolean enqueueRemove(Context context, String identifier) {
    return context.startService(getRemoveIntent(context, identifier)) != null;
  }

  //endregion

  private AlarmManager mAlarmManager;
  private ExpoNotificationsStore mStore;

  //region IntentService callbacks

  public ExpoNotificationsService() {
    super(TAG);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    mStore = new ExpoNotificationsStore(this);
    mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    if (intent == null) {
      return;
    }

    String identifier = intent.getStringExtra(NOTIFICATION_IDENTIFIER_KEY);
    if (SCHEDULE_ACTION.equals(intent.getAction())) {
      try {
        JSONObject notificationRequest = new JSONObject(intent.getStringExtra(NOTIFICATION_REQUEST_KEY));
        DateTimeTrigger trigger = (DateTimeTrigger) intent.getSerializableExtra(NOTIFICATION_TRIGGER_KEY);
        scheduleNotification(this, identifier, notificationRequest, trigger);
      } catch (JSONException e) {
        Log.e(TAG, String.format("Could not have parsed notification \"%s\".", identifier));
      }
    } else if (TRIGGER_ACTION.equals(intent.getAction())) {
      onNotificationTriggered(this, identifier);
    } else if (REMOVE_ACTION.equals(intent.getAction())) {
      removeNotification(this, identifier);
    } else {
      Log.d(TAG, String.format("Received intent of unrecognized action: %s. Ignoring.", intent.getAction()));
    }
  }

  //region Creating intents

  private static Intent getScheduleIntent(Context context, String identifier, JSONObject notificationRequest, DateTimeTrigger trigger) {
    Intent intent = new Intent(SCHEDULE_ACTION, getUriForIdentifier(identifier), context, ExpoNotificationsService.class);
    intent.putExtra(NOTIFICATION_IDENTIFIER_KEY, identifier);
    intent.putExtra(NOTIFICATION_REQUEST_KEY, notificationRequest.toString());
    intent.putExtra(NOTIFICATION_TRIGGER_KEY, trigger);
    return intent;
  }

  private static Intent getTriggerIntent(Context context, String identifier) {
    Intent intent = new Intent(context, ExpoNotificationsService.class);
    intent.setAction(TRIGGER_ACTION);
    intent.putExtra(NOTIFICATION_IDENTIFIER_KEY, identifier);
    intent.setData(getUriForIdentifier(identifier));
    return intent;
  }

  private static PendingIntent getTriggerPendingIntent(Context context, String identifier) {
    return PendingIntent.getService(context, REQUEST_CODE, getTriggerIntent(context, identifier), PendingIntent.FLAG_UPDATE_CURRENT);
  }

  private static Intent getRemoveIntent(Context context, String identifier) {
    Intent intent = new Intent(REMOVE_ACTION, getUriForIdentifier(identifier), context, ExpoNotificationsService.class);
    intent.putExtra(NOTIFICATION_IDENTIFIER_KEY, identifier);
    return intent;
  }

  //endregion

  //endregion

  private void scheduleNotification(Context context, String identifier, JSONObject request, DateTimeTrigger trigger) {
    Date nextTriggerDate = trigger.nextTriggerDate();

    if (nextTriggerDate == null) {
      Log.d(TAG, String.format("Notification \"%s\" will not trigger in the future, removing.", identifier));
      enqueueRemove(context, identifier);
      return;
    }

    try {
      mStore.saveNotification(identifier, request, trigger);
      PendingIntent intent = getTriggerPendingIntent(context, identifier);
      AlarmManagerCompat.setExactAndAllowWhileIdle(mAlarmManager, AlarmManager.RTC_WAKEUP, nextTriggerDate.getTime(), intent);
    } catch (IOException e) {
      Log.e(TAG, String.format("Could not have saved notification \"%s\": %s.", identifier, e.getMessage()));
      e.printStackTrace();
    }
  }

  private void removeNotification(Context context, String identifier) {
    mAlarmManager.cancel(getTriggerPendingIntent(context, identifier));
    mStore.removeNotification(identifier);
  }

  private void onNotificationTriggered(Context context, String identifier) {
    try {
      Pair<JSONObject, DateTimeTrigger> notification = mStore.getNotification(identifier);
      JSONObject notificationRequest = notification.first;
      DateTimeTrigger trigger = notification.second;

      for (WeakReference<ExpoNotificationScheduler> listenerReference : sListenersReferences.values()) {
        ExpoNotificationScheduler listener = listenerReference.get();
        if (listener != null) {
          listener.onNotificationFired(identifier, notificationRequest, trigger);
        }
      }

      enqueueSchedule(context, identifier, notificationRequest, trigger);
    } catch (IOException | JSONException | ClassNotFoundException e) {
      Log.e(TAG, String.format("Could not have triggered notification \"%s\": %s", identifier, e.getMessage()));
      e.printStackTrace();
    }
  }

  //region Helpers

  private static Uri getUriForIdentifier(String identifier) {
    return Uri.parse("expo-notifications://notifications/" + Uri.encode(identifier));
  }

  //endregion
}
