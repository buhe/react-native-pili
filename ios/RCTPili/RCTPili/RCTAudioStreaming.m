//
//  RCTStreaming.m
//  RCTPili
//
//  Created by guguyanhua on 16/5/26.
//  Copyright © 2016年 pili. All rights reserved.
//

#import "RCTAudioStreaming.h"
#import "RCTBridgeModule.h"
#import "UIView+React.h"
#import "RCTEventDispatcher.h"


@implementation RCTAudioStreaming{
    RCTEventDispatcher *_eventDispatcher;
    BOOL _started;
    BOOL _muted;
}

const char *audioStateNames[] = {
    "Unknow",
    "Connecting",
    "Connected",
    "Disconnecting",
    "Disconnected",
    "Error"
};

const char *audioNetworkStatus[] = {
    "Not Reachable",
    "Reachable via WiFi",
    "Reachable via CELL"
};




- (instancetype)initWithEventDispatcher:(RCTEventDispatcher *)eventDispatcher
{
    if ((self = [super init])) {
        [PLStreamingEnv initEnv];
        _eventDispatcher = eventDispatcher;
        _started = YES;
        _muted = NO;

        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(reachabilityChanged:) name:kReachabilityChangedNotification object:nil];
        self.internetReachability = [Reachability reachabilityForInternetConnection];
        [self.internetReachability startNotifier];
        
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(handleInterruption:)
                                                     name:AVAudioSessionInterruptionNotification
                                                   object:[AVAudioSession sharedInstance]];
        CGSize videoSize = CGSizeMake(480 , 640);
        UIDeviceOrientation orientation = [[UIDevice currentDevice] orientation];
        if (orientation <= AVCaptureVideoOrientationLandscapeLeft) {
            if (orientation > AVCaptureVideoOrientationPortraitUpsideDown) {
                videoSize = CGSizeMake(640 , 480);
            }
        }
        self.sessionQueue = dispatch_queue_create("pili.queue.streaming", DISPATCH_QUEUE_SERIAL);
    }
    
    return self;
};

- (void) setRtmpURL:(NSString *)rtmpURL
{
    _rtmpURL = rtmpURL;
    [self setSourceAndProfile];
}

- (void)setProfile:(NSDictionary *)profile{
    _profile = profile;
    [self setSourceAndProfile];
}

- (void) setSourceAndProfile{
    if(self.profile && self.rtmpURL){
        
        void (^permissionBlock)(void) = ^{
            dispatch_async(self.sessionQueue, ^{
                NSDictionary *audio = self.profile[@"audio"];
                
                PLAudioCaptureConfiguration *audioCaptureConfiguration = [PLAudioCaptureConfiguration defaultConfiguration];
                // 音频编码配置
                PLAudioStreamingConfiguration *audioStreamingConfiguration = [PLAudioStreamingConfiguration defaultConfiguration];
                AVCaptureVideoOrientation orientation = (AVCaptureVideoOrientation)(([[UIDevice currentDevice] orientation] <= UIDeviceOrientationLandscapeRight && [[UIDevice currentDevice] orientation] != UIDeviceOrientationUnknown) ? [[UIDevice currentDevice] orientation]: UIDeviceOrientationPortrait);
                // 推流 session
                self.session = [[PLCameraStreamingSession alloc] initWithVideoCaptureConfiguration:nil audioCaptureConfiguration:audioCaptureConfiguration videoStreamingConfiguration:nil audioStreamingConfiguration:audioStreamingConfiguration stream:nil videoOrientation:orientation];
                self.session.delegate = self;
                self.session.bufferDelegate = self;
 
                dispatch_async(dispatch_get_main_queue(), ^{
                    
                    if(_muted){
                        [self setMuted:_muted];
                    }
                    
                    [self startSession];
                });
            });
        };
        void (^noAccessBlock)(void) = ^{
            UIAlertView *alertView = [[UIAlertView alloc] initWithTitle:NSLocalizedString(@"No Access", nil)
                                                                message:NSLocalizedString(@"!", nil)
                                                               delegate:nil
                                                      cancelButtonTitle:NSLocalizedString(@"Cancel", nil)
                                                      otherButtonTitles:nil];
            [alertView show];
        };
        
        switch ([PLCameraStreamingSession cameraAuthorizationStatus]) {
            case PLAuthorizationStatusAuthorized:
                permissionBlock();
                break;
            case PLAuthorizationStatusNotDetermined: {
                [PLCameraStreamingSession requestCameraAccessWithCompletionHandler:^(BOOL granted) {
                    granted ? permissionBlock() : noAccessBlock();
                }];
            }
                break;
            default:
                noAccessBlock();
                break;
        }

    }
}

- (void)setStarted:(BOOL) started {
    if(started != _started){
        if(started){
            [self startSession];
            _started = started;
        }else{
            [self stopSession];
            _started = started;
        }
    }
}

-(void)setMuted:(BOOL) muted {
    _muted = muted;
    [self.session setMuted:muted];
}


- (void)streamingSessionSendingBufferDidFull:(id)session {
    NSString *log = @"Buffer is full";
    NSLog(@"%@", log);
}

- (void)streamingSession:(id)session sendingBufferDidDropItems:(NSArray *)items {
    NSString *log = @"Frame dropped";
    NSLog(@"%@", log);
}



- (void)stopSession {
    dispatch_async(self.sessionQueue, ^{
        [self.session stop];
    });
}

- (void)startSession {
    dispatch_async(self.sessionQueue, ^{
        NSURL *streamURL = [NSURL URLWithString:self.rtmpURL];
        [self.session startWithPushURL:streamURL feedback:^(PLStreamStartStateFeedback feedback) {
            dispatch_async(dispatch_get_main_queue(), ^{
                NSLog(@"success ");
            });
        }];
    });
}

- (void)cameraStreamingSession:(PLCameraStreamingSession *)session streamStatusDidUpdate:(PLStreamStatus *)status {
    NSString *log = [NSString stringWithFormat:@"Stream Status: %@", status];
    NSLog(@"%@", log);
}

- (void)cameraStreamingSession:(PLCameraStreamingSession *)session streamStateDidChange:(PLStreamState)state {
    NSString *log = [NSString stringWithFormat:@"Stream State: %s", audioStateNames[state]];
    NSLog(@"%@", log);
    
    switch (state) {
        case PLStreamStateUnknow:
            [_eventDispatcher sendInputEventWithName:@"onLoading" body:@{@"target": self.reactTag}];
            break;
        case PLStreamStateConnecting:
            [_eventDispatcher sendInputEventWithName:@"onConnecting" body:@{@"target": self.reactTag}];
            break;
        case PLStreamStateConnected:
            [_eventDispatcher sendInputEventWithName:@"onStreaming" body:@{@"target": self.reactTag}];
            break;
        case PLStreamStateDisconnecting:
            
            break;
        case PLStreamStateDisconnected:
            [_eventDispatcher sendInputEventWithName:@"onDisconnected" body:@{@"target": self.reactTag}];
            [_eventDispatcher sendInputEventWithName:@"onShutdown" body:@{@"target": self.reactTag}]; //FIXME
            break;
        case PLStreamStateError:
            [_eventDispatcher sendInputEventWithName:@"onIOError" body:@{@"target": self.reactTag}];
            break;
        default:
            break;
    }

}
- (void)cameraStreamingSession:(PLCameraStreamingSession *)session didDisconnectWithError:(NSError *)error {
    NSString *log = [NSString stringWithFormat:@"Stream State: Error. %@", error];
    NSLog(@"%@", log);
    [self startSession];
}

- (void)reachabilityChanged:(NSNotification *)notif{
    Reachability *curReach = [notif object];
    NSParameterAssert([curReach isKindOfClass:[Reachability class]]);
    NetworkStatus status = [curReach currentReachabilityStatus];
    
    if (NotReachable == status) {
        // 对断网情况做处理
        [self stopSession];
    }
    
    NSString *log = [NSString stringWithFormat:@"Networkt Status: %s", audioNetworkStatus[status]];
    NSLog(@"%@", log);
}

- (void)handleInterruption:(NSNotification *)notification {
    if ([notification.name isEqualToString:AVAudioSessionInterruptionNotification]) {
        NSLog(@"Interruption notification");
        
        if ([[notification.userInfo valueForKey:AVAudioSessionInterruptionTypeKey] isEqualToNumber:[NSNumber numberWithInt:AVAudioSessionInterruptionTypeBegan]]) {
            NSLog(@"InterruptionTypeBegan");
        } else {
            // the facetime iOS 9 has a bug: 1 does not send interrupt end 2 you can use application become active, and repeat set audio session acitve until success.  ref http://blog.corywiles.com/broken-facetime-audio-interruptions-in-ios-9
            NSLog(@"InterruptionTypeEnded");
            AVAudioSession *session = [AVAudioSession sharedInstance];
            [session setActive:YES error:nil];
        }
    }
}
@end
