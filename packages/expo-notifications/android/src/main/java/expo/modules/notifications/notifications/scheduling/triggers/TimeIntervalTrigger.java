package expo.modules.notifications.notifications.scheduling.triggers;

import java.util.Date;

import androidx.annotation.Nullable;
import expo.modules.notifications.notifications.interfaces.DateTimeTrigger;

public class TimeIntervalTrigger implements DateTimeTrigger {
  private Date mTriggerDate;
  private long mTimeInterval;
  private boolean mRepeats;

  public TimeIntervalTrigger(long timeInterval, boolean repeats) {
    mTriggerDate = new Date(new Date().getTime() + mTimeInterval);
    mTimeInterval = timeInterval;
    mRepeats = repeats;
  }

  @Nullable
  @Override
  public Date nextTriggerDate() {
    Date now = new Date();

    if (mRepeats) {
      while (mTriggerDate.before(now)) {
        mTriggerDate.setTime(mTriggerDate.getTime() + mTimeInterval);
      }
    }

    if (mTriggerDate.before(now)) {
      return null;
    }

    return mTriggerDate;
  }
}
