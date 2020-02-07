import { NativeModulesProxy } from '@unimodules/core';

type NotificationRequest = unknown; // TODO: Presentation PR

export type ScheduleId = string;
export type TimeZoneId = string;

// ISO8601 calendar pattern-matching
export interface CalendarTrigger {
  repeats?: boolean;

  timezone?: 'local' | TimeZoneId;

  era?: number;
  year?: number;
  month?: number;
  weekday?: number;
  weekOfMonth?: number;
  weekOfYear?: number;
  weekdayOrdinal?: number;
  day?: number;

  hour?: number;
  minute?: number;
  second?: number;
}

export interface TimeIntervalTrigger {
  repeats?: boolean;
  seconds: number; // from now
}

export type DateTrigger = Date | number; // Date object or seconds since 1970

export type NotificationTrigger = TimeIntervalTrigger | CalendarTrigger | DateTrigger;

export default async function scheduleNotificationAsync(
  identifier: string,
  notification: NotificationRequest,
  trigger: NotificationTrigger
): Promise<void> {
  return await NativeModulesProxy.NotificationScheduler.scheduleNotificationAsync(
    identifier,
    notification,
    parseTrigger(trigger)
  );
}

type NativeTrigger =
  | { type: 'date'; value: number }
  | { type: 'interval'; value: number }
  | { type: 'calendar'; value: CalendarTrigger };

function parseTrigger(userFacingTrigger: NotificationTrigger): NativeTrigger {
  if (userFacingTrigger instanceof Date) {
    return { type: 'date', value: userFacingTrigger.getTime() };
  } else if (typeof userFacingTrigger === 'number') {
    return { type: 'date', value: userFacingTrigger };
  } else if ('seconds' in userFacingTrigger) {
    if (!userFacingTrigger.repeats) {
      // not-repeating interval is just a single date
      return { type: 'date', value: new Date().getTime() + userFacingTrigger.seconds };
    } else {
      return { type: 'interval', value: userFacingTrigger.seconds };
    }
  } else {
    return { type: 'calendar', value: userFacingTrigger };
  }
}
