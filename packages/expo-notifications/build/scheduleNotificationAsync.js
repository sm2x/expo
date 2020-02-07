import { NativeModulesProxy } from '@unimodules/core';
export default async function scheduleNotificationAsync(identifier, notification, trigger) {
    return await NativeModulesProxy.NotificationScheduler.scheduleNotificationAsync(identifier, notification, parseTrigger(trigger));
}
function parseTrigger(userFacingTrigger) {
    if (userFacingTrigger instanceof Date) {
        return { type: 'date', value: userFacingTrigger.getTime() };
    }
    else if (typeof userFacingTrigger === 'number') {
        return { type: 'date', value: userFacingTrigger };
    }
    else if ('seconds' in userFacingTrigger) {
        if (!userFacingTrigger.repeats) {
            // not-repeating interval is just a single date
            return { type: 'date', value: new Date().getTime() + userFacingTrigger.seconds };
        }
        else {
            return { type: 'interval', value: userFacingTrigger.seconds };
        }
    }
    else {
        return { type: 'calendar', value: userFacingTrigger };
    }
}
//# sourceMappingURL=scheduleNotificationAsync.js.map