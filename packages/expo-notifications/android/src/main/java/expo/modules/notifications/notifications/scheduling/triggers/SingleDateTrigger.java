package expo.modules.notifications.notifications.scheduling.triggers;

import java.util.Date;

import androidx.annotation.Nullable;
import expo.modules.notifications.notifications.interfaces.DateTimeTrigger;

public class SingleDateTrigger implements DateTimeTrigger {
  private Date mTriggerDate;

  public SingleDateTrigger(Date triggerDate) {
    mTriggerDate = triggerDate;
  }

  @Nullable
  @Override
  public Date nextTriggerDate() {
    if (mTriggerDate.before(new Date())) {
      return null;
    }
    return mTriggerDate;
  }
}
