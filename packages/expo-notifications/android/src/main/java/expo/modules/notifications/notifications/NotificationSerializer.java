package expo.modules.notifications.notifications;

import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.unimodules.core.arguments.MapArguments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import expo.modules.notifications.notifications.interfaces.NotificationTrigger;
import expo.modules.notifications.notifications.triggers.FirebaseNotificationTrigger;

public class NotificationSerializer {
  public static Bundle toBundle(String identifier, JSONObject notification, NotificationTrigger trigger) {
    Bundle serializedNotification = new Bundle();
    serializedNotification.putString("identifier", identifier);
    serializedNotification.putBundle("notification", toBundle(notification));
    serializedNotification.putBundle("trigger", toBundle(trigger));
    return serializedNotification;
  }

  public static Bundle toBundle(JSONObject notification) {
    Map<String, Object> notificationMap = new HashMap<>(notification.length());
    Iterator<String> keyIterator = notification.keys();
    while (keyIterator.hasNext()) {
      String key = keyIterator.next();
      try {
        Object value = notification.get(key);
        if (value instanceof JSONObject) {
          notificationMap.put(key, toBundle((JSONObject) value));
        } else if (value instanceof JSONArray) {
          notificationMap.put(key, toList((JSONArray) value));
        } else if (value != null) {
          notificationMap.put(key, value);
        }
      } catch (JSONException e) {
        Log.e("expo-notifications", "Could not serialize whole notification - dropped value for key " + key + ": " + notification.opt(key));
      }
    }
    return new MapArguments(notificationMap).toBundle();
  }

  public static List toList(JSONArray array) {
    List<Object> result = new ArrayList<>(array.length());
    for (int i = 0; i < array.length(); i++) {
      if (array.isNull(i)) {
        result.add(null);
      } else if (array.optJSONObject(i) != null) {
        result.add(toBundle(array.optJSONObject(i)));
      } else if (array.optJSONArray(i) != null) {
        result.add(toList(array.optJSONArray(i)));
      } else {
        result.add(array.opt(i));
      }
    }
    return result;
  }

  public static Bundle toBundle(NotificationTrigger trigger) {
    Bundle bundle = new Bundle();
    if (trigger instanceof FirebaseNotificationTrigger) {
      bundle.putString("type", "push");
      bundle.putBundle("remoteMessage", RemoteMessageSerializer.toBundle(((FirebaseNotificationTrigger) trigger).getRemoteMessage()));
    } else {
      bundle.putString("type", "unknown");
    }
    return bundle;
  }

}
