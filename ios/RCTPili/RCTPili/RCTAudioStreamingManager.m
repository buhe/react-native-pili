//
//  RCTStreamingManager.m
//  RCTPili
//
//  Created by guguyanhua on 16/5/26.
//  Copyright © 2016年 pili. All rights reserved.
//

#import "RCTAudioStreamingManager.h"
#import "RCTAudioStreaming.h"

@implementation RCTAudioStreamingManager
RCT_EXPORT_MODULE();

@synthesize bridge = _bridge;

- (UIView *)view
{
    return [[RCTAudioStreaming alloc] initWithEventDispatcher:self.bridge.eventDispatcher];
}

- (NSArray *)customDirectEventTypes
{
    return @[
             @"onReady",
             @"onConnecting",
             @"onStreaming",
             @"onShutdown",
             @"onIOError",
             @"onDisconnected"
             ];
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

RCT_EXPORT_VIEW_PROPERTY(rtmpURL, NSString);
RCT_EXPORT_VIEW_PROPERTY(profile, NSDictionary);
RCT_EXPORT_VIEW_PROPERTY(started, BOOL);
RCT_EXPORT_VIEW_PROPERTY(muted, BOOL);



@end
