package expo.modules.notifications.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import expo.modules.notifications.notifications.interfaces.DateTimeTrigger;

public class ExpoNotificationsStore {
  private static final String SHARED_PREFERENCES_NAME = "expo.modules.notifications.ExpoNotificationsStore";

  private SharedPreferences mSharedPreferences;

  public ExpoNotificationsStore(Context context) {
    mSharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
  }

  public Pair<JSONObject, DateTimeTrigger> getNotification(String identifier) throws JSONException, IOException, ClassNotFoundException {
    JSONObject request = new JSONObject(mSharedPreferences.getString(preferencesRequestKey(identifier), null));
    DateTimeTrigger trigger = deserializeTrigger(mSharedPreferences.getString(preferencesTriggerKey(identifier), null));
    return new Pair<>(request, trigger);
  }

  public void saveNotification(String identifier, JSONObject notification, DateTimeTrigger trigger) throws IOException {
    mSharedPreferences.edit()
        .putString(preferencesRequestKey(identifier), notification.toString())
        .putString(preferencesTriggerKey(identifier), serializeTrigger(trigger))
        .apply();
  }

  public void removeNotification(String identifier) {
    mSharedPreferences.edit()
        .remove(preferencesRequestKey(identifier))
        .remove(preferencesTriggerKey(identifier))
        .apply();
  }

  private String serializeTrigger(DateTimeTrigger trigger) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
    objectOutputStream.writeObject(trigger);
    objectOutputStream.close();
    return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP);
  }

  private DateTimeTrigger deserializeTrigger(String trigger) throws IOException, ClassNotFoundException {
    byte[] data = Base64.decode(trigger, Base64.NO_WRAP);
    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
    Object o = ois.readObject();
    ois.close();
    if (o instanceof DateTimeTrigger) {
      return (DateTimeTrigger) o;
    }
    throw new InvalidClassException("Expected serialized trigger to be an instance of DateTimeTrigger. Found: " + o.toString());
  }

  private String preferencesRequestKey(String identifier) {
    return "request-" + identifier;
  }

  private String preferencesTriggerKey(String identifier) {
    return "trigger-" + identifier;
  }
}
