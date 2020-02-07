declare type NotificationRequest = unknown;
export declare type ScheduleId = string;
export declare type TimeZoneId = string;
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
    seconds: number;
}
export declare type DateTrigger = Date | number;
export declare type NotificationTrigger = TimeIntervalTrigger | CalendarTrigger | DateTrigger;
export default function scheduleNotificationAsync(identifier: string, notification: NotificationRequest, trigger: NotificationTrigger): Promise<void>;
export {};
