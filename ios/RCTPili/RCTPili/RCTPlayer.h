//
//  RCTPlayer.h
//  RCTPili
//
//  Created by buhe on 16/5/12.
//  Copyright © 2016年 pili. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "RCTView.h"
#import "PLPlayer.h"

@class RCTEventDispatcher;

@interface RCTPlayer : UIView<PLPlayerDelegate>

@property (nonatomic, assign) int reconnectCount;


- (instancetype)initWithEventDispatcher:(RCTEventDispatcher *)eventDispatcher NS_DESIGNATED_INITIALIZER;


@end
