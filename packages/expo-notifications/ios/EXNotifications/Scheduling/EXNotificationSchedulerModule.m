// Copyright 2018-present 650 Industries. All rights reserved.

#import <EXNotifications/EXNotificationSchedulerModule.h>

#import <UserNotifications/UserNotifications.h>

@implementation EXNotificationSchedulerModule

UM_EXPORT_MODULE(NotificationScheduler);

# pragma mark - Exported methods

UM_EXPORT_METHOD_AS(scheduleNotificationAsync,
                     scheduleNotification:(NSString *)identifier notificationSpec:(NSDictionary *)notificationSpec triggerSpec:(NSDictionary *)triggerSpec resolve:(UMPromiseResolveBlock)resolve rejecting:(UMPromiseRejectBlock)reject)
{
  reject(@"ERR_NOT_IMPLEMENTED", @"This function has not been implemented yet.", nil);
}

- (UNCalendarNotificationTrigger *)triggerFromParams:(NSDictionary *)triggerSpec
{
  if ([triggerSpec[@"type"] isEqualToString:@"calendar"]) {
    NSDateComponents *dateComponents = [NSDateComponents new];
    dateComponents.calendar = [NSCalendar calendarWithIdentifier:NSCalendarIdentifierGregorian];
    // TODO: Timezone
//    dateComponents.timeZone;
    NSArray<NSString *> *automaticallySetValues = @[
      @"era",
      @"year",
      @"month",
      @"day",
      @"hour",
      @"minute",
      @"second",
      @"weekday",
      @"weekOfMonth",
      @"weekOfYear",
      @"weekdayOrdinal"
    ];
    for (NSString *name in automaticallySetValues) {
      if (triggerSpec[@"value"][name]) {
        NSInteger value =[triggerSpec[@"value"][name] integerValue];
        if ([name isEqualToString:@"month"]) {
          value += 1;
        }
        [dateComponents setValue:value forComponent:[self calendarUnitFor:name]];
      } else {
        [dateComponents setValue:NSDateComponentUndefined forComponent:[self calendarUnitFor:name]];
      }
    }
    UNCalendarNotificationTrigger *trigger = [UNCalendarNotificationTrigger triggerWithDateMatchingComponents:dateComponents repeats:[triggerSpec[@"repeats"] boolValue]];
    return trigger;
  }
  return nil;
}

- (NSCalendarUnit)calendarUnitFor:(NSString *)key
{
  static NSDictionary *map;
  if (!map) {
    map = @{
      @"era": @(NSCalendarUnitEra),
      @"year": @(NSCalendarUnitYear),
      @"month": @(NSCalendarUnitMonth),
      @"day": @(NSCalendarUnitDay),
      @"hour": @(NSCalendarUnitHour),
      @"minute": @(NSCalendarUnitMinute),
      @"second": @(NSCalendarUnitSecond),
      @"weekday": @(NSCalendarUnitWeekday),
      @"weekOfMonth": @(NSCalendarUnitWeekOfMonth),
      @"weekOfYear": @(NSCalendarUnitWeekOfYear),
      @"weekdayOrdinal": @(NSCalendarUnitWeekdayOrdinal)
    };
  }
  return [map[key] unsignedIntegerValue];
}

@end
