//
//  RCTStreaming.h
//  RCTPili
//
//  Created by guguyanhua on 16/5/26.
//  Copyright © 2016年 pili. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "RCTView.h"
#import "PLCameraStreamingKit.h"
#import "Reachability.h"
#import <asl.h>

@class RCTEventDispatcher;

@interface RCTStreaming : UIView<PLCameraStreamingSessionDelegate,PLStreamingSendingBufferDelegate>

@property (nonatomic, strong) PLCameraStreamingSession  *session;
@property (nonatomic, strong) dispatch_queue_t sessionQueue;
@property (nonatomic, strong) NSArray<PLVideoCaptureConfiguration *>   *videoCaptureConfigurations;
@property (nonatomic, strong) NSArray<PLVideoStreamingConfiguration *>   *videoStreamingConfigurations;
@property (nonatomic, strong) Reachability *internetReachability;
@property (nonatomic, strong) NSDictionary  *profile;
@property (nonatomic, strong) NSDictionary  *source;

- (instancetype)initWithEventDispatcher:(RCTEventDispatcher *)eventDispatcher NS_DESIGNATED_INITIALIZER;

@end
