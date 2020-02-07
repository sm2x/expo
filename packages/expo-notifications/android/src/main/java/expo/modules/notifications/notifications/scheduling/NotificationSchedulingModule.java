package expo.modules.notifications.notifications.scheduling;

import android.content.Context;

import org.json.JSONObject;
import org.unimodules.core.ExportedModule;
import org.unimodules.core.ModuleRegistry;
import org.unimodules.core.Promise;
import org.unimodules.core.arguments.ReadableArguments;
import org.unimodules.core.errors.InvalidArgumentException;
import org.unimodules.core.interfaces.ExpoMethod;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import expo.modules.notifications.notifications.interfaces.DateTimeTrigger;
import expo.modules.notifications.notifications.interfaces.NotificationScheduler;
import expo.modules.notifications.notifications.scheduling.triggers.SingleDateTrigger;
import expo.modules.notifications.notifications.scheduling.triggers.TimeIntervalTrigger;

public class NotificationSchedulingModule extends ExportedModule {
  private final static String EXPORTED_NAME = "NotificationScheduler";

  private NotificationScheduler mNotificationScheduler;

  public NotificationSchedulingModule(Context context) {
    super(context);
  }

  @Override
  public String getName() {
    return EXPORTED_NAME;
  }

  @Override
  public void onCreate(ModuleRegistry moduleRegistry) {
    mNotificationScheduler = moduleRegistry.getSingletonModule("NotificationScheduler", NotificationScheduler.class);
  }

  @ExpoMethod
  public void scheduleNotificationAsync(String identifier, Map notificationSpec, ReadableArguments triggerParams, Promise promise) {
    try {
      JSONObject notificationRequest = new JSONObject(notificationSpec);
      mNotificationScheduler.scheduleNotification(identifier, notificationRequest, triggerFromParams(triggerParams));
      promise.resolve(null);
    } catch (IOException e) {
      promise.reject("ERR_FAILED_TO_SCHEDULE", e);
    }
  }

  private DateTimeTrigger triggerFromParams(ReadableArguments params) {
    switch (params.getString("type")) {
      case "date":
        if (!(params.get("value") instanceof Number)) {
          throw new InvalidArgumentException("Invalid value provided as date of trigger.");
        }
        Number dateValue = (Number) params.get("value");
        return new SingleDateTrigger(new Date(dateValue.longValue()));
      case "interval":
        if (!(params.get("value") instanceof Number)) {
          throw new InvalidArgumentException("Invalid value provided as date of trigger.");
        }
        return new TimeIntervalTrigger(((Number) params.get("value")).longValue(), params.getBoolean("repeats"));
      default:
        throw new InvalidArgumentException("Trigger of type: " + params.getString("type") + " is not supported on Android.");
    }
  }
}
