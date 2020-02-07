package expo.modules.notifications.notifications.interfaces;

import java.io.Serializable;
import java.util.Date;

import androidx.annotation.Nullable;

public interface DateTimeTrigger extends Serializable {
  @Nullable
  Date nextTriggerDate();
}
