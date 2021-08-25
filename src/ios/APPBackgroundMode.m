/*
  Copyright 2013-2017 appPlant GmbH

  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
*/

#import "APPMethodMagic.h"
#import "APPBackgroundMode.h"
#import <Cordova/CDVAvailability.h>

@implementation APPBackgroundMode

#pragma mark -
#pragma mark Constants

NSString* const kAPPBackgroundJsNamespace = @"cordova.plugins.backgroundMode";
NSString* const kAPPBackgroundEventActivate = @"activate";
NSString* const kAPPBackgroundEventDeactivate = @"deactivate";


#pragma mark -
#pragma mark Life Cycle

/**
 * Called by runtime once the Class has been loaded.
 * Exchange method implementations to hook into their execution.
 */
+ (void) load
{
    [self swizzleWKWebViewEngine];
}

/**
 * Initialize the plugin.
 */
- (void) pluginInitialize
{
    enabled = NO;
    //[self configureAudioPlayer];
    //[self configureAudioSession];
    NSLog(@"cdvbgmode pluginInitialize done");
    [self observeLifeCycle];
}

/**
 * Register the listener for pause and resume events.
 */
- (void) observeLifeCycle
{

     @try {
         NSNotificationCenter* listener = [NSNotificationCenter
                                      defaultCenter];

        [listener addObserver:self
                     selector:@selector(keepAwake)
                         name:UIApplicationDidEnterBackgroundNotification
                       object:nil];

        [listener addObserver:self
                     selector:@selector(stopKeepingAwake)
                         name:UIApplicationWillEnterForegroundNotification
                       object:nil];

        [listener addObserver:self
                     selector:@selector(handleAudioSessionInterruption:)
                         name:AVAudioSessionInterruptionNotification
                       object:nil];
     }
     @catch (NSException *exception) {
        NSLog(@"observeLifeCycle err: %@", exception.reason);
     }
     @finally {
        NSLog(@"observeLifeCycle Finally done");
     }
   
}

#pragma mark -
#pragma mark Interface

/**
 * Enable the mode to stay awake
 * when switching to background for the next time.
 */
- (void) enable:(CDVInvokedUrlCommand*)command
{
    if (enabled)
        return;

    @try {
        NSLog(@"cdvbgmode enable done");
        enabled = YES;
        [self execCallback:command];
    }
    @catch (NSException *exception) {
        NSLog(@"disable err: %@", exception.reason);
    }
    @finally {
        NSLog(@"disable Finally done");
    }
    
}

/**
 * Disable the background mode
 * and stop being active in background.
 */
- (void) disable:(CDVInvokedUrlCommand*)command
{
    if (!enabled)
        return;

    @try {
        enabled = NO;
        NSLog(@"cdvbgmode disable done");
        [self stopKeepingAwake];
        [self execCallback:command];
    }
    @catch (NSException *exception) {
        NSLog(@"disable err: %@", exception.reason);
    }
    @finally {
        NSLog(@"disable Finally done");
    }
    
}

#pragma mark -
#pragma mark Core

/**
 * Keep the app awake.
 */
- (void) keepAwake
{
    if (!enabled)
        return;

    @try {
        //[audioPlayer play];
        NSLog(@"cdvbgmode keepAwake done");
        [self fireEvent:kAPPBackgroundEventActivate];
    }
    @catch (NSException *exception) {
        NSLog(@"keepAwake err: %@", exception.reason);
    }
    @finally {
        NSLog(@"keepAwake Finally done");
    }
    
}

/**
 * Let the app going to sleep.
 */
- (void) stopKeepingAwake
{
     @try {
         if (TARGET_IPHONE_SIMULATOR) {
            NSLog(@"BackgroundMode: On simulator apps never pause in background!");
        }

        // if (audioPlayer.isPlaying) {
        //     [self fireEvent:kAPPBackgroundEventDeactivate];
        // }

        //[audioPlayer pause];
     }
     @catch (NSException *exception) {
        NSLog(@"stopKeepingAwake err: %@", exception.reason);
     }
     @finally {
        NSLog(@"stopKeepingAwake Finally done");
     }
   
}

/**
 * Configure the audio player.
 */
- (void) configureAudioPlayer
{
    @try {
        NSString* path = [[NSBundle mainBundle]
                      pathForResource:@"appbeep" ofType:@"wav"];

        NSURL* url = [NSURL fileURLWithPath:path];


        audioPlayer = [[AVAudioPlayer alloc]
                    initWithContentsOfURL:url error:NULL];

        audioPlayer.volume        = 0;
        audioPlayer.numberOfLoops = -1;
     }
     @catch (NSException *exception) {
        NSLog(@"configureAudioPlayer err: %@", exception.reason);
     }
     @finally {
        NSLog(@"configureAudioPlayer Finally done");
     }
   
};

/**
 * Configure the audio session.
 */
- (void) configureAudioSession
{
     @try {
        AVAudioSession* session = [AVAudioSession
                               sharedInstance];

        // Don't activate the audio session yet
        [session setActive:NO error:NULL];

        // Play music even in background and dont stop playing music
        // even another app starts playing sound
        [session setCategory:AVAudioSessionCategoryPlayback
                    error:NULL];

        // Active the audio session
        [session setActive:YES error:NULL];
        
     }
     @catch (NSException *exception) {
        NSLog(@"configureAudioSession err: %@", exception.reason);
     }
     @finally {
        NSLog(@"configureAudioSession Finally done");
     }
   
};

#pragma mark -
#pragma mark Helper

/**
 * Simply invokes the callback without any parameter.
 */
- (void) execCallback:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult *result = [CDVPluginResult
                               resultWithStatus:CDVCommandStatus_OK];

    [self.commandDelegate sendPluginResult:result
                                callbackId:command.callbackId];
}

/**
 * Restart playing sound when interrupted by phone calls.
 */
- (void) handleAudioSessionInterruption:(NSNotification*)notification
{
    [self fireEvent:kAPPBackgroundEventDeactivate];
    [self keepAwake];
}

/**
 * Find out if the app runs inside the webkit powered webview.
 */
+ (BOOL) isRunningWebKit
{
    return IsAtLeastiOSVersion(@"8.0") && NSClassFromString(@"CDVWKWebViewEngine");
}

/**
 * Method to fire an event with some parameters in the browser.
 */
- (void) fireEvent:(NSString*)event
{

    @try { 
        NSString* active =
        [event isEqualToString:kAPPBackgroundEventActivate] ? @"true" : @"false";

        NSString* flag = [NSString stringWithFormat:@"%@._isActive=%@;",
                        kAPPBackgroundJsNamespace, active];

        NSString* depFn = [NSString stringWithFormat:@"%@.on('%@');",
                        kAPPBackgroundJsNamespace, event];

        NSString* fn = [NSString stringWithFormat:@"%@.fireEvent('%@');",
                        kAPPBackgroundJsNamespace, event];

        NSString* js = [NSString stringWithFormat:@"%@%@%@", flag, depFn, fn];

        [self.commandDelegate evalJs:js];
    }
    @catch (NSException *exception) {
        NSLog(@"fireEvent err: %@", exception.reason);
    }
    @finally {
        NSLog(@"fireEvent Finally done");
    }
    
}

#pragma mark -
#pragma mark Swizzling

/**
 * Method to swizzle.
 */
+ (NSString*) wkProperty
{
    NSString* str = @"YWx3YXlzUnVuc0F0Rm9yZWdyb3VuZFByaW9yaXR5";
    NSData* data  = [[NSData alloc] initWithBase64EncodedString:str options:0];

    return [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
}

/**
 * Swizzle some implementations of CDVWKWebViewEngine.
 */
+ (void) swizzleWKWebViewEngine
{
    
    if (![self isRunningWebKit])
        return;


    @try {
        Class wkWebViewEngineCls = NSClassFromString(@"CDVWKWebViewEngine");
        SEL selector = NSSelectorFromString(@"createConfigurationFromSettings:");

        SwizzleSelectorWithBlock_Begin(wkWebViewEngineCls, selector)
        ^(CDVPlugin *self, NSDictionary *settings) {
            id obj = ((id (*)(id, SEL, NSDictionary*))_imp)(self, _cmd, settings);

            [obj setValue:[NSNumber numberWithBool:YES]
                forKey:[APPBackgroundMode wkProperty]];

            [obj setValue:[NSNumber numberWithBool:NO]
                forKey:@"requiresUserActionForMediaPlayback"];

            return obj;
        }
        SwizzleSelectorWithBlock_End;
    
    }
    @catch (NSException *exception) {
        NSLog(@"swizzleWKWebViewEngine err: %@", exception.reason);
    }
    @finally {
        NSLog(@"swizzleWKWebViewEngine Finally done");
    }
   
}

@end
