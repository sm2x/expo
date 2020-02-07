package expo.modules.notifications;

import android.content.Context;

import org.unimodules.core.BasePackage;
import org.unimodules.core.ExportedModule;
import org.unimodules.core.interfaces.SingletonModule;

import java.util.Arrays;
import java.util.List;

import expo.modules.notifications.installationid.InstallationIdProvider;
import expo.modules.notifications.notifications.NotificationManager;
import expo.modules.notifications.notifications.channels.ExpoNotificationChannelsManager;
import expo.modules.notifications.notifications.emitting.NotificationsEmitter;
import expo.modules.notifications.notifications.handling.NotificationsHandler;
import expo.modules.notifications.notifications.scheduling.ExpoNotificationScheduler;
import expo.modules.notifications.notifications.scheduling.NotificationSchedulingModule;
import expo.modules.notifications.permissions.NotificationPermissionsModule;
import expo.modules.notifications.tokens.PushTokenManager;
import expo.modules.notifications.tokens.PushTokenModule;

public class NotificationsPackage extends BasePackage {
  @Override
  public List<ExportedModule> createExportedModules(Context context) {
    return Arrays.asList(
        new PushTokenModule(context),
        new NotificationsEmitter(context),
        new NotificationsHandler(context),
        new InstallationIdProvider(context),
        new NotificationSchedulingModule(context),
        new NotificationPermissionsModule(context)
    );
  }

  @Override
  public List<SingletonModule> createSingletonModules(Context context) {
    return Arrays.asList(
        new PushTokenManager(),
        new NotificationManager(),
        new ExpoNotificationScheduler(context),
        new ExpoNotificationChannelsManager(context)
    );
  }
}
