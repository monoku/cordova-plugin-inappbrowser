/*
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

#import "CDVInAppBrowser.h"
#import <Cordova/CDVPluginResult.h>
#import <Cordova/CDVUserAgentUtil.h>
// #import <Cordova/CDVJSON.h>
#import "CDVReachability.h"

#define    kInAppBrowserTargetSelf @"_self"
#define    kInAppBrowserTargetSystem @"_system"
#define    kInAppBrowserTargetBlank @"_blank"

#define    kInAppBrowserToolbarBarPositionBottom @"bottom"
#define    kInAppBrowserToolbarBarPositionTop @"top"

#define    TOOLBAR_HEIGHT 56
#define    LOCATIONBAR_HEIGHT 0//21.0
#define    FOOTER_HEIGHT 0//((TOOLBAR_HEIGHT) + (LOCATIONBAR_HEIGHT))

#pragma mark CDVInAppBrowser

@interface CDVInAppBrowser () {
    NSInteger _previousStatusBarStyle;
}
@end

@implementation CDVInAppBrowser

- (void)pluginInitialize
{
    _previousStatusBarStyle = -1;
    _callbackIdPattern = nil;
}

- (id)settingForKey:(NSString*)key
{
    return [self.commandDelegate.settings objectForKey:[key lowercaseString]];
}

- (void)onReset
{
    [self close:nil];
}

- (void)close:(CDVInvokedUrlCommand*)command
{

    NSLog(@"CLOSE CMD");

    if (self.inAppBrowserViewController == nil) {
        NSLog(@"IAB.close() called but it was already closed.");
        return;
    }
    // Things are cleaned up in browserExit.
    [self.inAppBrowserViewController close];
}

- (BOOL) isSystemUrl:(NSURL*)url
{
    if ([[url host] isEqualToString:@"itunes.apple.com"]) {
        return YES;
    }

    return NO;
}

- (void)open:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult;

    NSString* url = [command argumentAtIndex:0];
    NSString* target = [command argumentAtIndex:1 withDefault:kInAppBrowserTargetSelf];
    NSString* options = [command argumentAtIndex:2 withDefault:@"" andClass:[NSString class]];
    NSString* HTMLFastText = [command argumentAtIndex:3 withDefault:@"" andClass:[NSString class]];
    self.HTMLFastText = HTMLFastText;
    //    NSLog(@"-------------------=====--------------------------");
    //    NSLog(HTMLFastText);
    self.callbackId = command.callbackId;

    if (url != nil) {
#ifdef __CORDOVA_4_0_0
        NSURL* baseUrl = [self.webViewEngine URL];
#else
        NSURL* baseUrl = [self.webView.request URL];
#endif

        NSURL* absoluteUrl = [[NSURL URLWithString:url relativeToURL:baseUrl] absoluteURL];
        //        [self.inAppBrowserViewController.textWebView.loadRequest request];
        //        [self.webView loadHTMLString:HTMLFastText baseURL:nil];
        //        [self.inAppBrowserViewController.textWebView loadData:HTMLFastText MIMEType: @"text/html" textEncodingName: @"UTF-8" baseURL:nil];
        if ([self isSystemUrl:absoluteUrl]) {
            NSLog(@"-------------------isSystemUrl--------------------------");
            target = kInAppBrowserTargetSystem;
        }

        if ([target isEqualToString:kInAppBrowserTargetSelf]) {
            NSLog(@"-------------------kInAppBrowserTargetSelf--------------------------");
            [self openInCordovaWebView:absoluteUrl withOptions:options HTMLFastText:HTMLFastText];
        } else if ([target isEqualToString:kInAppBrowserTargetSystem]) {
            NSLog(@"-------------------kInAppBrowserTargetSystem--------------------------");
            [self openInSystem:absoluteUrl];
        } else { // _blank or anything else
            NSLog(@"-------------------blank--------------------------");
            [self openInInAppBrowser:absoluteUrl withOptions:options HTMLFastText:HTMLFastText];
        }

        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"incorrect number of arguments"];
    }

    [pluginResult setKeepCallback:[NSNumber numberWithBool:YES]];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    NSLog(@"-------------------opeeeeeen--------------------------");
}

- (void)openInInAppBrowser:(NSURL*)url withOptions:(NSString*)options HTMLFastText:(NSString*)HTMLFastText
{
    CDVInAppBrowserOptions* browserOptions = [CDVInAppBrowserOptions parseOptions:options];

    NSLog(@"-------------------opeeeeeenInnnnnn Appppp Browseeeer--------------------------");

    if (browserOptions.clearcache) {
        NSHTTPCookie *cookie;
        NSHTTPCookieStorage *storage = [NSHTTPCookieStorage sharedHTTPCookieStorage];
        for (cookie in [storage cookies])
        {
            if (![cookie.domain isEqual: @".^filecookies^"]) {
                [storage deleteCookie:cookie];
            }
        }
    }

    if (browserOptions.clearsessioncache) {
        NSHTTPCookie *cookie;
        NSHTTPCookieStorage *storage = [NSHTTPCookieStorage sharedHTTPCookieStorage];
        for (cookie in [storage cookies])
        {
            if (![cookie.domain isEqual: @".^filecookies^"] && cookie.isSessionOnly) {
                [storage deleteCookie:cookie];
            }
        }
    }

    if (self.inAppBrowserViewController == nil) {
        NSString* userAgent = [CDVUserAgentUtil originalUserAgent];
        NSString* overrideUserAgent = [self settingForKey:@"OverrideUserAgent"];
        NSString* appendUserAgent = [self settingForKey:@"AppendUserAgent"];
        if(overrideUserAgent){
            userAgent = overrideUserAgent;
        }
        if(appendUserAgent){
            userAgent = [userAgent stringByAppendingString: appendUserAgent];
        }
        self.inAppBrowserViewController = [[CDVInAppBrowserViewController alloc] initWithUserAgent:userAgent prevUserAgent:[self.commandDelegate userAgent] browserOptions: browserOptions];
        self.inAppBrowserViewController.navigationDelegate = self;

        if ([self.viewController conformsToProtocol:@protocol(CDVScreenOrientationDelegate)]) {
            self.inAppBrowserViewController.orientationDelegate = (UIViewController <CDVScreenOrientationDelegate>*)self.viewController;
        }
    }

    [self.inAppBrowserViewController showLocationBar:browserOptions.location];
    [self.inAppBrowserViewController showToolBar:browserOptions.toolbar :browserOptions.toolbarposition];
    if (browserOptions.closebuttoncaption != nil) {
        [self.inAppBrowserViewController setCloseButtonTitle:browserOptions.closebuttoncaption];
    }
    // Set Presentation Style
    UIModalPresentationStyle presentationStyle = UIModalPresentationFullScreen; // default
    if (browserOptions.presentationstyle != nil) {
        if ([[browserOptions.presentationstyle lowercaseString] isEqualToString:@"pagesheet"]) {
            presentationStyle = UIModalPresentationPageSheet;
        } else if ([[browserOptions.presentationstyle lowercaseString] isEqualToString:@"formsheet"]) {
            presentationStyle = UIModalPresentationFormSheet;
        }
    }
    self.inAppBrowserViewController.modalPresentationStyle = presentationStyle;

    // Set Transition Style
    UIModalTransitionStyle transitionStyle = UIModalTransitionStyleCoverVertical; // default
    if (browserOptions.transitionstyle != nil) {
        if ([[browserOptions.transitionstyle lowercaseString] isEqualToString:@"fliphorizontal"]) {
            transitionStyle = UIModalTransitionStyleFlipHorizontal;
        } else if ([[browserOptions.transitionstyle lowercaseString] isEqualToString:@"crossdissolve"]) {
            transitionStyle = UIModalTransitionStyleCrossDissolve;
        }
    }
    self.inAppBrowserViewController.modalTransitionStyle = transitionStyle;

    // prevent webView from bouncing
    if (browserOptions.disallowoverscroll) {
        if ([self.inAppBrowserViewController.webView respondsToSelector:@selector(scrollView)]) {
            ((UIScrollView*)[self.inAppBrowserViewController.webView scrollView]).bounces = NO;
        } else {
            for (id subview in self.inAppBrowserViewController.webView.subviews) {
                if ([[subview class] isSubclassOfClass:[UIScrollView class]]) {
                    ((UIScrollView*)subview).bounces = NO;
                }
            }
        }
    }

    // UIWebView options
    self.inAppBrowserViewController.webView.scalesPageToFit = browserOptions.enableviewportscale;
    self.inAppBrowserViewController.webView.mediaPlaybackRequiresUserAction = browserOptions.mediaplaybackrequiresuseraction;
    self.inAppBrowserViewController.webView.allowsInlineMediaPlayback = browserOptions.allowinlinemediaplayback;
    self.inAppBrowserViewController.textWebView.scalesPageToFit = browserOptions.enableviewportscale;
    self.inAppBrowserViewController.textWebView.mediaPlaybackRequiresUserAction = browserOptions.mediaplaybackrequiresuseraction;
    self.inAppBrowserViewController.textWebView.allowsInlineMediaPlayback = browserOptions.allowinlinemediaplayback;
    if (IsAtLeastiOSVersion(@"6.0")) {
        self.inAppBrowserViewController.webView.keyboardDisplayRequiresUserAction = browserOptions.keyboarddisplayrequiresuseraction;
        self.inAppBrowserViewController.webView.suppressesIncrementalRendering = browserOptions.suppressesincrementalrendering;
        self.inAppBrowserViewController.textWebView.keyboardDisplayRequiresUserAction = browserOptions.keyboarddisplayrequiresuseraction;
        self.inAppBrowserViewController.textWebView.suppressesIncrementalRendering = browserOptions.suppressesincrementalrendering;
    }

    [self.inAppBrowserViewController navigateTo:url HTMLFastText:HTMLFastText];

    if (!browserOptions.hidden) {
        [self show:nil];
    }
}

- (void)show:(CDVInvokedUrlCommand*)command
{
    // if (self.inAppBrowserViewController == nil) {
    //     NSLog(@"Tried to show IAB after it was closed.");
    //     return;
    // }
    // if (_previousStatusBarStyle != -1) {
    //     NSLog(@"Tried to show IAB while already shown");
    //     return;
    // }

    //    _previousStatusBarStyle = [UIApplication sharedApplication].statusBarStyle;

    __block CDVInAppBrowserNavigationController* nav = [[CDVInAppBrowserNavigationController alloc]
                                                        initWithRootViewController:self.inAppBrowserViewController];
    nav.orientationDelegate = self.inAppBrowserViewController;
    nav.navigationBarHidden = YES;
    nav.modalPresentationStyle = self.inAppBrowserViewController.modalPresentationStyle;

    if (IsAtLeastiOSVersion(@"7.0")) {
        //[[UIApplication sharedApplication] setStatusBarStyle:[self preferredStatusBarStyle]];
        [[UIApplication sharedApplication] setStatusBarHidden:YES];
    }

    __weak CDVInAppBrowser* weakSelf = self;

    // Run later to avoid the "took a long time" log message.
    dispatch_async(dispatch_get_main_queue(), ^{
        if (weakSelf.inAppBrowserViewController != nil) {
            [weakSelf.viewController presentViewController:nav animated:YES completion:nil];
        }
    });
}

- (void)hide:(CDVInvokedUrlCommand*)command
{
    if (self.inAppBrowserViewController == nil) {
        NSLog(@"Tried to hide IAB after it was closed.");
        return;


    }
    if (_previousStatusBarStyle == -1) {
        NSLog(@"Tried to hide IAB while already hidden");
        return;
    }

    _previousStatusBarStyle = [UIApplication sharedApplication].statusBarStyle;

    // Run later to avoid the "took a long time" log message.
    dispatch_async(dispatch_get_main_queue(), ^{
        if (self.inAppBrowserViewController != nil) {
            _previousStatusBarStyle = -1;
            [self.viewController dismissViewControllerAnimated:YES completion:nil];
        }
    });
}

- (void)openInCordovaWebView:(NSURL*)url withOptions:(NSString*)options HTMLFastText:(NSString*)HTMLFastText
{

    NSURLRequest* request = [NSURLRequest requestWithURL:url];

#ifdef __CORDOVA_4_0_0
    // the webview engine itself will filter for this according to <allow-navigation> policy
    // in config.xml for cordova-ios-4.0
    [self.webViewEngine loadRequest:request];
#else
    if ([self.commandDelegate URLIsWhitelisted:url]) {
        [self.webView loadRequest:request];
    } else { // this assumes the InAppBrowser can be excepted from the white-list
        [self openInInAppBrowser:url withOptions:options HTMLFastText:HTMLFastText];
    }
#endif
}

- (void)openInSystem:(NSURL*)url
{
    [[NSNotificationCenter defaultCenter] postNotification:[NSNotification notificationWithName:CDVPluginHandleOpenURLNotification object:url]];
    [[UIApplication sharedApplication] openURL:url];
}

// This is a helper method for the inject{Script|Style}{Code|File} API calls, which
// provides a consistent method for injecting JavaScript code into the document.
//
// If a wrapper string is supplied, then the source string will be JSON-encoded (adding
// quotes) and wrapped using string formatting. (The wrapper string should have a single
// '%@' marker).
//
// If no wrapper is supplied, then the source string is executed directly.

- (void)injectDeferredObject:(NSString*)source withWrapper:(NSString*)jsWrapper
{
    // Ensure an iframe bridge is created to communicate with the CDVInAppBrowserViewController
    [self.inAppBrowserViewController.webView stringByEvaluatingJavaScriptFromString:@"(function(d){_cdvIframeBridge=d.getElementById('_cdvIframeBridge');if(!_cdvIframeBridge) {var e = _cdvIframeBridge = d.createElement('iframe');e.id='_cdvIframeBridge'; e.style.display='none';d.body.appendChild(e);}})(document)"];

    if (jsWrapper != nil) {
        NSData* jsonData = [NSJSONSerialization dataWithJSONObject:@[source] options:0 error:nil];
        NSString* sourceArrayString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
        if (sourceArrayString) {
            NSString* sourceString = [sourceArrayString substringWithRange:NSMakeRange(1, [sourceArrayString length] - 2)];
            NSString* jsToInject = [NSString stringWithFormat:jsWrapper, sourceString];
            [self.inAppBrowserViewController.webView stringByEvaluatingJavaScriptFromString:jsToInject];
            [self.inAppBrowserViewController.textWebView stringByEvaluatingJavaScriptFromString:jsToInject];
        }
    } else {
        [self.inAppBrowserViewController.webView stringByEvaluatingJavaScriptFromString:source];
        [self.inAppBrowserViewController.textWebView stringByEvaluatingJavaScriptFromString:source];
    }
}

- (void)injectScriptCode:(CDVInvokedUrlCommand*)command
{
    NSString* jsWrapper = nil;

    if ((command.callbackId != nil) && ![command.callbackId isEqualToString:@"INVALID"]) {
        jsWrapper = [NSString stringWithFormat:@"_cdvIframeBridge.src='gap-iab://%@/'+encodeURIComponent(JSON.stringify([eval(%%@)]));", command.callbackId];
    }
    [self injectDeferredObject:[command argumentAtIndex:0] withWrapper:jsWrapper];
}

- (void)injectScriptFile:(CDVInvokedUrlCommand*)command
{
    NSString* jsWrapper;

    if ((command.callbackId != nil) && ![command.callbackId isEqualToString:@"INVALID"]) {
        jsWrapper = [NSString stringWithFormat:@"(function(d) { var c = d.createElement('script'); c.src = %%@; c.onload = function() { _cdvIframeBridge.src='gap-iab://%@'; }; d.body.appendChild(c); })(document)", command.callbackId];
    } else {
        jsWrapper = @"(function(d) { var c = d.createElement('script'); c.src = %@; d.body.appendChild(c); })(document)";
    }
    [self injectDeferredObject:[command argumentAtIndex:0] withWrapper:jsWrapper];
}

- (void)injectStyleCode:(CDVInvokedUrlCommand*)command
{
    NSString* jsWrapper;

    if ((command.callbackId != nil) && ![command.callbackId isEqualToString:@"INVALID"]) {
        jsWrapper = [NSString stringWithFormat:@"(function(d) { var c = d.createElement('style'); c.innerHTML = %%@; c.onload = function() { _cdvIframeBridge.src='gap-iab://%@'; }; d.body.appendChild(c); })(document)", command.callbackId];
    } else {
        jsWrapper = @"(function(d) { var c = d.createElement('style'); c.innerHTML = %@; d.body.appendChild(c); })(document)";
    }
    [self injectDeferredObject:[command argumentAtIndex:0] withWrapper:jsWrapper];
}

- (void)injectStyleFile:(CDVInvokedUrlCommand*)command
{
    NSString* jsWrapper;

    if ((command.callbackId != nil) && ![command.callbackId isEqualToString:@"INVALID"]) {
        jsWrapper = [NSString stringWithFormat:@"(function(d) { var c = d.createElement('link'); c.rel='stylesheet'; c.type='text/css'; c.href = %%@; c.onload = function() { _cdvIframeBridge.src='gap-iab://%@'; }; d.body.appendChild(c); })(document)", command.callbackId];
    } else {
        jsWrapper = @"(function(d) { var c = d.createElement('link'); c.rel='stylesheet', c.type='text/css'; c.href = %@; d.body.appendChild(c); })(document)";
    }
    [self injectDeferredObject:[command argumentAtIndex:0] withWrapper:jsWrapper];
}

- (BOOL)isValidCallbackId:(NSString *)callbackId
{
    NSError *err = nil;
    // Initialize on first use
    if (self.callbackIdPattern == nil) {
        self.callbackIdPattern = [NSRegularExpression regularExpressionWithPattern:@"^InAppBrowser[0-9]{1,10}$" options:0 error:&err];
        if (err != nil) {
            // Couldn't initialize Regex; No is safer than Yes.
            return NO;
        }
    }
    if ([self.callbackIdPattern firstMatchInString:callbackId options:0 range:NSMakeRange(0, [callbackId length])]) {
        return YES;
    }
    return NO;
}

/**
 * The iframe bridge provided for the InAppBrowser is capable of executing any oustanding callback belonging
 * to the InAppBrowser plugin. Care has been taken that other callbacks cannot be triggered, and that no
 * other code execution is possible.
 *
 * To trigger the bridge, the iframe (or any other resource) should attempt to load a url of the form:
 *
 * gap-iab://<callbackId>/<arguments>
 *
 * where <callbackId> is the string id of the callback to trigger (something like "InAppBrowser0123456789")
 *
 * If present, the path component of the special gap-iab:// url is expected to be a URL-escaped JSON-encoded
 * value to pass to the callback. [NSURL path] should take care of the URL-unescaping, and a JSON_EXCEPTION
 * is returned if the JSON is invalid.
 */
- (BOOL)webView:(UIWebView*)theWebView shouldStartLoadWithRequest:(NSURLRequest*)request navigationType:(UIWebViewNavigationType)navigationType
{
    NSURL* url = request.URL;
    BOOL isTopLevelNavigation = [request.URL isEqual:[request mainDocumentURL]];

    // See if the url uses the 'gap-iab' protocol. If so, the host should be the id of a callback to execute,
    // and the path, if present, should be a JSON-encoded value to pass to the callback.
    if ([[url scheme] isEqualToString:@"gap-iab"]) {
        NSString* scriptCallbackId = [url host];
        CDVPluginResult* pluginResult = nil;

        if ([self isValidCallbackId:scriptCallbackId]) {
            NSString* scriptResult = [url path];
            NSError* __autoreleasing error = nil;

            // The message should be a JSON-encoded array of the result of the script which executed.
            if ((scriptResult != nil) && ([scriptResult length] > 1)) {
                scriptResult = [scriptResult substringFromIndex:1];
                NSData* decodedResult = [NSJSONSerialization JSONObjectWithData:[scriptResult dataUsingEncoding:NSUTF8StringEncoding] options:kNilOptions error:&error];
                if ((error == nil) && [decodedResult isKindOfClass:[NSArray class]]) {
                    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:(NSArray*)decodedResult];
                } else {
                    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_JSON_EXCEPTION];
                }
            } else {
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:@[]];
            }
            [self.commandDelegate sendPluginResult:pluginResult callbackId:scriptCallbackId];
            return NO;
        }
    } 
    //if is an app store link, let the system handle it, otherwise it fails to load it
    else if ([[ url scheme] isEqualToString:@"itms-appss"] || [[ url scheme] isEqualToString:@"itms-apps"]) {
        [theWebView stopLoading];
        [self openInSystem:url];
        return NO;
    }
    else if ((self.callbackId != nil) && isTopLevelNavigation) {
        // Send a loadstart event for each top-level navigation (includes redirects).
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                                      messageAsDictionary:@{@"type":@"loadstart", @"url":[url absoluteString]}];
        [pluginResult setKeepCallback:[NSNumber numberWithBool:YES]];

        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
    }

    return YES;
}

- (void)webViewDidStartLoad:(UIWebView*)theWebView
{
}

- (void)webViewDidFinishLoad:(UIWebView*)theWebView
{
    NSLog(@"============webViewDidFinishLoad========");
    if (self.callbackId != nil) {
        // TODO: It would be more useful to return the URL the page is actually on (e.g. if it's been redirected).
        NSString* url = [self.inAppBrowserViewController.currentURL absoluteString];
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                                      messageAsDictionary:@{@"type":@"loadstop", @"url":url}];
        [pluginResult setKeepCallback:[NSNumber numberWithBool:YES]];

        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
    }
}

- (void)webView:(UIWebView*)theWebView didFailLoadWithError:(NSError*)error
{
    if (self.callbackId != nil) {
        NSString* url = [self.inAppBrowserViewController.currentURL absoluteString];
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                                      messageAsDictionary:@{@"type":@"loaderror", @"url":url, @"code": [NSNumber numberWithInteger:error.code], @"message": error.localizedDescription}];
        [pluginResult setKeepCallback:[NSNumber numberWithBool:YES]];

        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
    }
}

- (void)browserExit
{
    if (self.callbackId != nil) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                                      messageAsDictionary:@{@"type":@"exit"}];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
        self.callbackId = nil;
    }
    // Set navigationDelegate to nil to ensure no callbacks are received from it.
    self.inAppBrowserViewController.navigationDelegate = nil;
    // Don't recycle the ViewController since it may be consuming a lot of memory.
    // Also - this is required for the PDF/User-Agent bug work-around.
    self.inAppBrowserViewController = nil;

    if (IsAtLeastiOSVersion(@"7.0")) {
        if (_previousStatusBarStyle != -1) {
            [[UIApplication sharedApplication] setStatusBarHidden:YES];
        }
    }

    _previousStatusBarStyle = -1; // this value was reset before reapplying it. caused statusbar to stay black on ios7
}

@end

#pragma mark CDVInAppBrowserViewController

@implementation CDVInAppBrowserViewController

@synthesize currentURL;

- (id)initWithUserAgent:(NSString*)userAgent prevUserAgent:(NSString*)prevUserAgent browserOptions: (CDVInAppBrowserOptions*) browserOptions
{
    self = [super init];
    if (self != nil) {
        _userAgent = userAgent;
        _prevUserAgent = prevUserAgent;
        _browserOptions = browserOptions;
#ifdef __CORDOVA_4_0_0
        _webViewDelegate = [[CDVUIWebViewDelegate alloc] initWithDelegate:self];
#else
        _webViewDelegate = [[CDVWebViewDelegate alloc] initWithDelegate:self];
#endif

        [self createViews];
    }

    return self;
}

// Prevent crashes on closing windows
-(void)dealloc {
    self.webView.delegate = nil;
}

- (void)createViews
{
    // We create the views in code for primarily for ease of upgrades and not requiring an external .xib to be included
    CGRect webViewBounds = self.view.bounds;
    BOOL toolbarIsAtBottom = ![_browserOptions.toolbarposition isEqualToString:kInAppBrowserToolbarBarPositionTop];

    webViewBounds.size.height -= _browserOptions.location ? FOOTER_HEIGHT : TOOLBAR_HEIGHT;

    float toolbarY = toolbarIsAtBottom ? self.view.bounds.size.height - TOOLBAR_HEIGHT : 0.0;
    CGRect toolbarFrame = CGRectMake(0.0, toolbarY, self.view.bounds.size.width, TOOLBAR_HEIGHT);

    self.toolbar = [[UIToolbar alloc] initWithFrame:toolbarFrame];
    self.toolbar.alpha = 1.000;
    self.toolbar.autoresizesSubviews = YES;
    self.toolbar.autoresizingMask = toolbarIsAtBottom ? (UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleTopMargin) : UIViewAutoresizingFlexibleWidth;
    //self.toolbar.barStyle = UIBarStyle;
    self.toolbar.tintColor = [UIColor colorWithRed:1 green:1 blue:1 alpha:0];
    self.toolbar.tintColor = [UIColor whiteColor];
    self.toolbar.translucent = NO;
    self.toolbar.clearsContextBeforeDrawing = NO;
    self.toolbar.clipsToBounds = NO;
    self.toolbar.contentMode = UIViewContentModeScaleToFill;
    self.toolbar.hidden = NO;
    self.toolbar.multipleTouchEnabled = NO;
    self.toolbar.opaque = NO;
    self.toolbar.userInteractionEnabled = YES;

    self.webView = [[UIWebView alloc] initWithFrame:CGRectMake(0.0, [self getStatusBarOffset] + self.toolbar.frame.size.height + self.view.frame.size.height, self.view.frame.size.width, self.view.frame.size.height - 55)];
    self.webView.autoresizingMask = (UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight);
    self.webView.delegate = _webViewDelegate;
    self.webView.backgroundColor = [UIColor whiteColor];
    self.webView.clearsContextBeforeDrawing = YES;
    self.webView.clipsToBounds = YES;
    self.webView.contentMode = UIViewContentModeScaleToFill;
    self.webView.multipleTouchEnabled = YES;
    self.webView.opaque = YES;
    self.webView.scalesPageToFit = NO;
    self.webView.userInteractionEnabled = YES;

    self.textWebView = [[UIWebView alloc] initWithFrame:CGRectMake(0.0, [self getStatusBarOffset] + self.toolbar.frame.size.height + self.view.frame.size.height, self.view.frame.size.width, self.view.frame.size.height)];
    self.textWebView.autoresizingMask = (UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight);
    self.textWebView.backgroundColor = [UIColor whiteColor];
    self.textWebView.clearsContextBeforeDrawing = YES;
    self.textWebView.clipsToBounds = YES;
    self.textWebView.contentMode = UIViewContentModeScaleToFill;
    self.textWebView.multipleTouchEnabled = YES;
    self.textWebView.opaque = YES;
    self.textWebView.scalesPageToFit = YES;
    self.textWebView.userInteractionEnabled = YES;

    self.spinner = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleGray];
    // self.spinner.alpha = 1.000;
    // self.spinner.autoresizesSubviews = YES;
    // self.spinner.autoresizingMask = (UIViewAutoresizingFlexibleLeftMargin | UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleBottomMargin | UIViewAutoresizingFlexibleRightMargin);
    // self.spinner.clearsContextBeforeDrawing = NO;
    // self.spinner.clipsToBounds = NO;
    // self.spinner.contentMode = UIViewContentModeScaleToFill;
    self.spinner.frame = CGRectMake(self.view.frame.size.width / 2 - 10, self.view.frame.size.height/ 2 - 10, 20.0, 20.0);
    self.spinner.hidden = YES;
    self.spinner.hidesWhenStopped = YES;
    //self.spinner.multipleTouchEnabled = NO;
    //self.spinner.opaque = NO;
    //self.spinner.userInteractionEnabled = NO;
    [self.spinner stopAnimating];

    NSString *pathC = @"icon_close.jpg";
    UIImage *imgC = [[UIImage imageNamed:pathC] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];
    self.closeButton = [[UIBarButtonItem alloc] initWithImage:imgC
                                                        style:UIBarButtonItemStylePlain
                                                       target:self
                                                       action:@selector(close)];
    self.closeButton.enabled = YES;

    NSString *path = @"icon_fav.jpg";
    UIImage *img = [[UIImage imageNamed:path] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];

    self.favButton = [[UIBarButtonItem alloc] initWithImage:img
                                                      style:UIBarButtonItemStylePlain
                                                     target:self
                                                     action:@selector(webViewFavPage)];
    self.favButton.enabled = YES;


    NSString *pathba = @"icon_arrow_left_active.jpg";
    UIImage *imgba = [[UIImage imageNamed:pathba] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];
    self.backButtonText = [[UIBarButtonItem alloc] initWithImage:imgba
                                                           style:UIBarButtonItemStylePlain
                                                          target:self
                                                          action:@selector(goBackText)];
    self.backButtonText.enabled = YES;

    NSString *pathT = @"icon_fast.jpg";
    UIImage *imgT = [[UIImage imageNamed:pathT] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];

    self.textButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    [self.textButton addTarget:self action:@selector(webViewTextMode) forControlEvents:UIControlEventTouchUpInside];
    [self.textButton setImage:imgT forState:UIControlStateNormal];
    self.textButton.enabled = YES;
    self.textButton.frame= CGRectMake(self.view.bounds.size.width/2-374/2, 80, 374, 142);

    NSString *pathSh = @"icon_share.jpg";
    UIImage *imgSh = [[UIImage imageNamed:pathSh] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];

    self.shareButton = [[UIBarButtonItem alloc] initWithImage:imgSh
                                                        style:UIBarButtonItemStylePlain
                                                       target:self
                                                       action:@selector(webViewSharePage)];
    self.shareButton.enabled = YES;

    self.toolbarText = [[UIView alloc] initWithFrame:CGRectMake(0.0, [self getStatusBarOffset] + self.toolbar.frame.size.height, self.view.frame.size.width, self.view.frame.size.height)];
    self.toolbarText.autoresizesSubviews = NO;
    self.toolbarText.autoresizingMask = toolbarIsAtBottom ? (UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleTopMargin) : UIViewAutoresizingFlexibleWidth;
    //self.toolbar.barStyle = UIBarStyle;
    //self.toolbarText.tintColor = [UIColor colorWithRed:1 green:1 blue:1 alpha:0];
    //    self.toolbarText.tintColor = [UIColor colorWithRed:0.94 green:0.94 blue:0.94 alpha:1];
    //    self.toolbarText.barTintColor = [UIColor colorWithRed:0.94 green:0.94 blue:0.94 alpha:1];
    //    self.toolbarText.translucent = NO;
    [self.toolbarText setBackgroundColor:[UIColor colorWithRed:0.94 green:0.94 blue:0.94 alpha:1]];
    self.toolbarText.clearsContextBeforeDrawing = NO;
    self.toolbarText.clipsToBounds = YES;
    self.toolbarText.contentMode = UIViewContentModeScaleToFill;
    self.toolbarText.hidden = NO;
    self.toolbarText.multipleTouchEnabled = NO;
    self.toolbarText.opaque = YES;
    self.toolbarText.userInteractionEnabled = YES;

    NSArray *animationFrames = [NSArray arrayWithObjects:
                                [UIImage imageNamed:@"loader0001.png"],
                                [UIImage imageNamed:@"loader0002.png"],
                                [UIImage imageNamed:@"loader0003.png"],
                                [UIImage imageNamed:@"loader0004.png"],
                                [UIImage imageNamed:@"loader0005.png"],
                                [UIImage imageNamed:@"loader0006.png"],
                                [UIImage imageNamed:@"loader0007.png"],
                                [UIImage imageNamed:@"loader0008.png"],
                                [UIImage imageNamed:@"loader0009.png"],
                                [UIImage imageNamed:@"loader0010.png"],
                                [UIImage imageNamed:@"loader0011.png"],
                                [UIImage imageNamed:@"loader0012.png"],
                                [UIImage imageNamed:@"loader0013.png"],
                                [UIImage imageNamed:@"loader0014.png"],
                                [UIImage imageNamed:@"loader0015.png"],
                                [UIImage imageNamed:@"loader0016.png"],
                                [UIImage imageNamed:@"loader0017.png"],
                                [UIImage imageNamed:@"loader0018.png"],
                                [UIImage imageNamed:@"loader0019.png"],
                                [UIImage imageNamed:@"loader0020.png"],
                                [UIImage imageNamed:@"loader0021.png"],
                                [UIImage imageNamed:@"loader0022.png"],
                                [UIImage imageNamed:@"loader0023.png"],
                                nil];

    UIImageView *loader = [[UIImageView alloc] init];
    loader.animationImages = animationFrames;
    loader.animationDuration = 1.2;
    loader.animationRepeatCount = 0;
    [loader startAnimating];
    loader.frame= CGRectMake(self.view.bounds.size.width/2-23/2, 220, 17, 17);

    UILabel *fastText = [[UILabel alloc] initWithFrame:CGRectMake(0, 180, self.view.bounds.size.width, 40)];
    fastText.adjustsFontSizeToFitWidth = NO;
    fastText.alpha = 1.000;
    //    fastText.autoresizesSubviews = YES;
    fastText.backgroundColor = [UIColor clearColor];
    fastText.textColor = [UIColor colorWithRed:0.0 green:0.248 blue:0.317 alpha:1];
    fastText.baselineAdjustment = UIBaselineAdjustmentAlignCenters;
    //    fastText.clearsContextBeforeDrawing = YES;
    //    fastText.clipsToBounds = YES;
    //    fastText.contentMode = UIViewContentModeScaleToFill;
    //    fastText.enabled = YES;
    //    fastText.hidden = NO;
    //    fastText.lineBreakMode = NSLineBreakByTruncatingTail;
    fastText.text = NSLocalizedString(@"Tap Offline View to see content faster.", nil);
    fastText.textAlignment = NSTextAlignmentCenter;
    [fastText setFont:[UIFont fontWithName:@"HelveticaNeue" size:10]];

    CGFloat labelInset = 5.0;
    float locationBarY = toolbarIsAtBottom ? self.view.bounds.size.height - FOOTER_HEIGHT : self.view.bounds.size.height - LOCATIONBAR_HEIGHT;

    self.addressLabel = [[UILabel alloc] initWithFrame:CGRectMake(labelInset, locationBarY, self.view.bounds.size.width - labelInset, LOCATIONBAR_HEIGHT)];
    self.addressLabel.adjustsFontSizeToFitWidth = NO;
    self.addressLabel.alpha = 1.000;
    self.addressLabel.autoresizesSubviews = YES;
    self.addressLabel.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleRightMargin | UIViewAutoresizingFlexibleTopMargin;
    self.addressLabel.backgroundColor = [UIColor clearColor];
    self.addressLabel.baselineAdjustment = UIBaselineAdjustmentAlignCenters;
    self.addressLabel.clearsContextBeforeDrawing = YES;
    self.addressLabel.clipsToBounds = YES;
    self.addressLabel.contentMode = UIViewContentModeScaleToFill;
    self.addressLabel.enabled = YES;
    self.addressLabel.hidden = YES;
    self.addressLabel.lineBreakMode = NSLineBreakByTruncatingTail;

    if ([self.addressLabel respondsToSelector:NSSelectorFromString(@"setMinimumScaleFactor:")]) {
        [self.addressLabel setValue:@(10.0/[UIFont labelFontSize]) forKey:@"minimumScaleFactor"];
    } else if ([self.addressLabel respondsToSelector:NSSelectorFromString(@"setMinimumFontSize:")]) {
        [self.addressLabel setValue:@(10.0) forKey:@"minimumFontSize"];
    }

    self.addressLabel.multipleTouchEnabled = NO;
    self.addressLabel.numberOfLines = 1;
    self.addressLabel.opaque = NO;
    self.addressLabel.shadowOffset = CGSizeMake(0.0, -1.0);
    self.addressLabel.text = NSLocalizedString(@"Loading...", nil);
    self.addressLabel.textAlignment = NSTextAlignmentLeft;
    self.addressLabel.textColor = [UIColor colorWithWhite:1.000 alpha:1.000];
    self.addressLabel.userInteractionEnabled = NO;

    //    NSString* frontArrowString = NSLocalizedString(@"►", nil); // create arrow from Unicode char
    //    self.forwardButton = [[UIBarButtonItem alloc] initWithTitle:frontArrowString style:UIBarButtonItemStylePlain target:self action:@selector(goForward:)];
    //    self.forwardButton.enabled = YES;
    //    self.forwardButton.imageInsets = UIEdgeInsetsZero;

    NSString *pathf = @"icon_arrow_right.jpg";
    UIImage *imgf = [[UIImage imageNamed:pathf] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];

    self.forwardButton = [[UIBarButtonItem alloc] initWithImage:imgf
                                                          style:UIBarButtonItemStylePlain
                                                         target:self.webView
                                                         action:@selector(goForward)];


    //    NSString* backArrowString = NSLocalizedString(@"◄", nil); // create arrow from Unicode char
    //    self.backButton = [[UIBarButtonItem alloc] initWithTitle:backArrowString style:UIBarButtonItemStylePlain target:self action:@selector(goBack:)];
    //    self.backButton.enabled = YES;
    //    self.backButton.imageInsets = UIEdgeInsetsZero;

    NSString *pathb = @"icon_arrow_left.jpg";
    UIImage *imgb = [[UIImage imageNamed:pathb] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];

    self.backButton = [[UIBarButtonItem alloc] initWithImage:imgb
                                                       style:UIBarButtonItemStylePlain
                                                      target:self.webView
                                                      action:@selector(goBack)];


    //    if (_browserOptions.hidefav != nil) {
    //        [self.toolbar setItems:@[negativeSpacerLarge, self.backButton, fixedSpaceButton, self.forwardButton, flexibleSpaceButton, self.shareButton, fixedSpaceButton, fixedSpaceButton, self.closeButton, negativeSpacer]];
    //    }else{
    //        [self.toolbar setItems:@[negativeSpacerLarge, self.backButton, fixedSpaceButton, self.forwardButton, flexibleSpaceButton, self.shareButton, fixedSpaceButton, self.favButton, fixedSpaceButton, self.closeButton, negativeSpacer]];
    //    }

    UIBarButtonItem* flexibleSpaceButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace target:nil action:nil];
    UIBarButtonItem* negativeSpacer = [[UIBarButtonItem alloc]
                                       initWithBarButtonSystemItem:UIBarButtonSystemItemFixedSpace
                                       target:nil action:nil];
    negativeSpacer.width = -5;

    [self.toolbar setItems:@[flexibleSpaceButton, self.closeButton, negativeSpacer]];

    // [self.toolbarText addSubview:loader];

    //    [self.textButton setCenter:self.toolbarText.center];

    self.view.backgroundColor = [UIColor grayColor];

    [self.view addSubview:self.toolbar];
    [self.view addSubview:self.addressLabel];

    [self.view addSubview:self.webView];
    [self.view addSubview:self.spinner];
    [self.view addSubview:self.toolbarText];
    [self.view addSubview:self.textWebView];

}

-(void) showWebView {
    NSLog(@"========showView===========");
    self.addressLabel.text = [self.currentURL absoluteString];
    self.backButton.enabled = self.webView.canGoBack;
    self.forwardButton.enabled = self.webView.canGoForward;
    self.textButton.enabled = NO;

    if( !self.textModeActivated ){
        [self.textWebView removeFromSuperview];
    }

    //if( !self.didInit ){
    self.didInit = YES;
    // [self.webView setFrame:CGRectMake(self.webView.frame.origin.x, [self getStatusBarOffset] + self.toolbar.frame.size.height + self.webView.frame.size.height , self.webView.frame.size.width, self.webView.frame.size.height)];
    [UIView animateWithDuration:0.25 animations:^{
        [self.webView setFrame:CGRectMake(0.0, [self getStatusBarOffset] + self.toolbar.frame.size.height, self.view.frame.size.width, self.view.frame.size.height - 55)];
        // [self.textWebView setFrame:CGRectMake(self.textWebView.frame.origin.x, [self getStatusBarOffset] + self.toolbar.frame.size.height, self.textWebView.frame.size.width, self.textWebView.frame.size.height)];
        [self.toolbarText setFrame:CGRectMake(0.0, [self getStatusBarOffset] + self.toolbar.frame.size.height, self.toolbarText.frame.size.width, 0)];
    } completion:^(BOOL finished) {
        if( !self.textModeActivated ){
            [self.toolbarText setHidden:YES];
        }
    }];
    //}

    [self showAllButtons];
}

-(void) showAllButtons {
    // if( !self.didInit ){
    UIBarButtonItem* flexibleSpaceButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace target:nil action:nil];

    UIBarButtonItem* negativeSpacer = [[UIBarButtonItem alloc]
                                       initWithBarButtonSystemItem:UIBarButtonSystemItemFixedSpace
                                       target:nil action:nil];
    negativeSpacer.width = -5;


    UIBarButtonItem* negativeSpacerLarge = [[UIBarButtonItem alloc]
                                            initWithBarButtonSystemItem:UIBarButtonSystemItemFixedSpace
                                            target:nil action:nil];
    negativeSpacerLarge.width = -10;

    UIBarButtonItem* fixedSpaceButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFixedSpace target:nil action:nil];
    fixedSpaceButton.width = 4;


    if (_browserOptions.hidefav != nil) {
        if (self.textModeActivated) {
            [self.toolbar setItems:@[negativeSpacerLarge, self.backButtonText, flexibleSpaceButton, self.shareButton, fixedSpaceButton, fixedSpaceButton, self.closeButton, negativeSpacer]];
        }else{
            [self.toolbar setItems:@[negativeSpacerLarge, self.backButton, fixedSpaceButton, self.forwardButton, flexibleSpaceButton, self.shareButton, fixedSpaceButton, fixedSpaceButton, self.closeButton, negativeSpacer]];
        }
    }else{
        if (self.textModeActivated) {
            [self.toolbar setItems:@[negativeSpacerLarge, self.backButtonText, flexibleSpaceButton, self.favButton, fixedSpaceButton, self.shareButton, fixedSpaceButton, self.closeButton, negativeSpacer]];
        }else{
            [self.toolbar setItems:@[negativeSpacerLarge, self.backButton, fixedSpaceButton, self.forwardButton, flexibleSpaceButton, self.favButton, fixedSpaceButton, self.shareButton, fixedSpaceButton, self.closeButton, negativeSpacer]];
        }
    }

    // NSInteger height = self.toolbarText.frame.size.height;
    // [UIView animateWithDuration:0.25 animations:^{
    //     [self.webView setFrame:CGRectMake(self.webView.frame.origin.x, [self getStatusBarOffset] + self.toolbar.frame.size.height, self.webView.frame.size.width, self.webView.frame.size.height)];
    //     [self.textWebView setFrame:CGRectMake(self.textWebView.frame.origin.x, [self getStatusBarOffset] + self.toolbar.frame.size.height, self.textWebView.frame.size.width, self.textWebView.frame.size.height)];
    //     [self.toolbarText setFrame:CGRectMake(self.webView.frame.origin.x, [self getStatusBarOffset] + self.toolbar.frame.size.height, self.toolbarText.frame.size.width, 0)];
    // } completion:^(BOOL finished) {
    //     [self.toolbarText setHidden:YES];
    //     [self.toolbarText removeFromSuperview];
    // }];
    // }

    // self.didInit = YES;
}

- (void) setWebViewFrame : (CGRect) frame {
    NSLog(@"Setting the WebView's frame to %@", NSStringFromCGRect(frame));
    //[self.webView setFrame:frame];
    //[self.textWebView setFrame:frame];
}

- (void)setCloseButtonTitle:(NSString*)title
{
    // the advantage of using UIBarButtonSystemItemDone is the system will localize it for you automatically
    // but, if you want to set this yourself, knock yourself out (we can't set the title for a system Done button, so we have to create a new one)
    self.closeButton = nil;
    self.closeButton = [[UIBarButtonItem alloc] initWithTitle:title style:UIBarButtonItemStyleBordered target:self action:@selector(close)];
    self.closeButton.enabled = YES;
    self.closeButton.tintColor = [UIColor colorWithRed:60.0 / 255.0 green:136.0 / 255.0 blue:230.0 / 255.0 alpha:1];

    NSMutableArray* items = [self.toolbar.items mutableCopy];
    [items replaceObjectAtIndex:0 withObject:self.closeButton];
    [self.toolbar setItems:items];
}

- (void)showLocationBar:(BOOL)show
{
    CGRect locationbarFrame = self.addressLabel.frame;

    BOOL toolbarVisible = !self.toolbar.hidden;

    // prevent double show/hide
    if (show == !(self.addressLabel.hidden)) {
        return;
    }

    if (show) {
        self.addressLabel.hidden = NO;

        if (toolbarVisible) {
            // toolBar at the bottom, leave as is
            // put locationBar on top of the toolBar

            CGRect webViewBounds = self.view.bounds;
            webViewBounds.size.height -= FOOTER_HEIGHT;
            [self setWebViewFrame:webViewBounds];

            locationbarFrame.origin.y = webViewBounds.size.height;
            self.addressLabel.frame = locationbarFrame;
        } else {
            // no toolBar, so put locationBar at the bottom

            CGRect webViewBounds = self.view.bounds;
            webViewBounds.size.height -= LOCATIONBAR_HEIGHT;
            [self setWebViewFrame:webViewBounds];

            locationbarFrame.origin.y = webViewBounds.size.height;
            self.addressLabel.frame = locationbarFrame;
        }
    } else {
        self.addressLabel.hidden = YES;

        if (toolbarVisible) {
            // locationBar is on top of toolBar, hide locationBar

            // webView take up whole height less toolBar height
            CGRect webViewBounds = self.view.bounds;
            webViewBounds.size.height -= TOOLBAR_HEIGHT;
            [self setWebViewFrame:webViewBounds];
        } else {
            // no toolBar, expand webView to screen dimensions
            [self setWebViewFrame:self.view.bounds];
        }
    }
}

- (void)showToolBar:(BOOL)show : (NSString *) toolbarPosition
{
    CGRect toolbarFrame = self.toolbar.frame;
    CGRect locationbarFrame = self.addressLabel.frame;

    BOOL locationbarVisible = !self.addressLabel.hidden;

    // prevent double show/hide
    if (show == !(self.toolbar.hidden)) {
        return;
    }

    if (show) {
        self.toolbar.hidden = NO;
        CGRect webViewBounds = self.view.bounds;

        if (locationbarVisible) {
            // locationBar at the bottom, move locationBar up
            // put toolBar at the bottom
            webViewBounds.size.height -= FOOTER_HEIGHT;
            locationbarFrame.origin.y = webViewBounds.size.height;
            self.addressLabel.frame = locationbarFrame;
            self.toolbar.frame = toolbarFrame;
        } else {
            // no locationBar, so put toolBar at the bottom
            CGRect webViewBounds = self.view.bounds;
            webViewBounds.size.height -= TOOLBAR_HEIGHT;
            self.toolbar.frame = toolbarFrame;
        }

        if ([toolbarPosition isEqualToString:kInAppBrowserToolbarBarPositionTop]) {
            toolbarFrame.origin.y = 0;
            webViewBounds.origin.y += toolbarFrame.size.height;
            //[self setWebViewFrame:webViewBounds];
        } else {
            toolbarFrame.origin.y = (webViewBounds.size.height + LOCATIONBAR_HEIGHT);
        }
        //[self setWebViewFrame:webViewBounds];

    } else {
        self.toolbar.hidden = YES;

        if (locationbarVisible) {
            // locationBar is on top of toolBar, hide toolBar
            // put locationBar at the bottom

            // webView take up whole height less locationBar height
            CGRect webViewBounds = self.view.bounds;
            webViewBounds.size.height -= LOCATIONBAR_HEIGHT;
            //[self setWebViewFrame:webViewBounds];

            // move locationBar down
            locationbarFrame.origin.y = webViewBounds.size.height;
            self.addressLabel.frame = locationbarFrame;
        } else {
            // no locationBar, expand webView to screen dimensions
            //[self setWebViewFrame:self.view.bounds];
        }
    }
}

- (void)viewDidLoad
{
    [super viewDidLoad];
}

- (void)viewDidUnload
{
    NSLog(@"============viewDidUnload=============");
    [self.webView loadHTMLString:nil baseURL:nil];
    [self.textWebView loadHTMLString:nil baseURL:nil];
    [CDVUserAgentUtil releaseLock:&_userAgentLockToken];
    [super viewDidUnload];
}

- (UIStatusBarStyle)preferredStatusBarStyle
{
    return UIStatusBarStyleDefault;
}

- (BOOL)prefersStatusBarHidden {
    return YES;
}

- (void)close
{
    // Dispatch touch close event here
    [self.navigationDelegate.commandDelegate evalJs:@"cordova.fireDocumentEvent('in-app-browser-touch-close')"];

    [CDVUserAgentUtil releaseLock:&_userAgentLockToken];
    self.currentURL = nil;

    NSLog(@"IS NOT BACKRGOUND");

    if ((self.navigationDelegate != nil) && [self.navigationDelegate respondsToSelector:@selector(browserExit)]) {
        [self.navigationDelegate browserExit];
    }

    __weak UIViewController* weakSelf = self;

    // Run later to avoid the "took a long time" log message.
    dispatch_async(dispatch_get_main_queue(), ^{
        if ([weakSelf respondsToSelector:@selector(presentingViewController)]) {
            [[weakSelf presentingViewController] dismissViewControllerAnimated:YES completion:nil];
        } else {
            [[weakSelf parentViewController] dismissViewControllerAnimated:YES completion:nil];
        }
    });
}



- (void)webViewFavPage
{
    if (self.navigationDelegate.callbackId != nil) {
        // TODO: It would be more useful to return the URL the page is actually on (e.g. if it's been redirected).
        NSString* url = [self.navigationDelegate.inAppBrowserViewController.currentURL absoluteString];
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                                      messageAsDictionary:@{@"type":@"fav", @"url":url}];
        [pluginResult setKeepCallback:[NSNumber numberWithBool:YES]];

        [self.navigationDelegate.commandDelegate sendPluginResult:pluginResult callbackId:self.navigationDelegate.callbackId];

        NSString *js = @"try{window.cordovaInappBrowserFavCallBack();}catch(e){}";
        [self.navigationDelegate injectDeferredObject:js
                                          withWrapper:nil];
    }
}

- (void)goBackText
{
    self.textModeActivated = NO;
    self.textButton.enabled = YES;

    UIBarButtonItem* flexibleSpaceButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace target:nil action:nil];

    UIBarButtonItem* negativeSpacer = [[UIBarButtonItem alloc]
                                       initWithBarButtonSystemItem:UIBarButtonSystemItemFixedSpace
                                       target:nil action:nil];
    negativeSpacer.width = -5;

    [self.toolbar setItems:@[flexibleSpaceButton, self.closeButton, negativeSpacer]];
    // self.backButton.enabled = NO;
    // self.backButton.image = [[UIImage imageNamed:@"icon_arrow_left.jpg"] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];
    // [self.toolbarText setHidden:NO];
    // [self.toolbarText removeFromSuperview];
    // [self.view addSubview:self.toolbarText];
    [UIView animateWithDuration:0.25 animations:^{
        [self.textWebView setFrame:CGRectMake(0.0, [self getStatusBarOffset] + self.toolbar.frame.size.height + self.view.frame.size.height, self.view.frame.size.width, self.view.frame.size.height)];
        // [self.toolbarText setFrame:CGRectMake(self.webView.frame.origin.x, [self getStatusBarOffset] + self.toolbar.frame.size.height, self.toolbarText.frame.size.width, self.textWebView.frame.size.width)];
    } completion:^(BOOL finished) {
        if( self.didInit ){
            [self showAllButtons];
        }
        // [self.textWebView setHidden:YES];
    }];
}

- (void)webViewTextMode
{
    if (self.navigationDelegate.callbackId != nil) {
        self.textModeActivated = YES;
        self.textButton.enabled = NO;
        self.backButton.enabled = YES;
        self.backButton.image = [[UIImage imageNamed:@"icon_arrow_left_active.jpg"] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];
        // TODO: It would be more useful to return the URL the page is actually on (e.g. if it's been redirected).
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                                      messageAsDictionary:@{@"type":@"text"}];
        [pluginResult setKeepCallback:[NSNumber numberWithBool:YES]];

        [self.navigationDelegate.commandDelegate sendPluginResult:pluginResult callbackId:self.navigationDelegate.callbackId];

        [self showAllButtons];

        // [self.webView removeFromSuperview];
        // [self.webView loadHTMLString:nil baseURL:nil];
        // [self.webView setHidden:YES];
        // [self.textWebView setHidden:NO];

        // [self.textWebView setFrame:CGRectMake(self.textWebView.frame.origin.x, [self getStatusBarOffset] + self.toolbar.frame.size.height + self.textWebView.frame.size.height, self.textWebView.frame.size.width, self.textWebView.frame.size.height)];
        //NSInteger height = self.toolbarText.frame.size.height;
        [UIView animateWithDuration:0.25 animations:^{
            [self.textWebView setFrame:CGRectMake(0.0, [self getStatusBarOffset] + self.toolbar.frame.size.height, self.view.frame.size.width, self.view.frame.size.height)];
        } completion:^(BOOL finished) {
        }];

        //        NSString *js = @"try{cordovaInappBrowserInsertText();}catch(e){}";
        //        [self.navigationDelegate injectDeferredObject:js
        //                                          withWrapper:nil];
    }
}

- (void)webViewSharePage
{
    if (self.navigationDelegate.callbackId != nil) {
        // TODO: It would be more useful to return the URL the page is actually on (e.g. if it's been redirected).
        NSString* url = [self.navigationDelegate.inAppBrowserViewController.currentURL absoluteString];
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                                      messageAsDictionary:@{@"type":@"share", @"url":url}];
        [pluginResult setKeepCallback:[NSNumber numberWithBool:YES]];
        [self.navigationDelegate.commandDelegate sendPluginResult:pluginResult callbackId:self.navigationDelegate.callbackId];

        //        [self close];

        //        NSString *js = @"try{window.cordovaInappBrowserShareCallBack();}catch(e){}";
        //        [self.navigationDelegate injectDeferredObject:js
        //                                          withWrapper:nil];

    }
}

- (void)navigateTo:(NSURL*)url HTMLFastText:(NSString*)HTMLFastText
{
    NSLog(@"======================navigate toooooo=============================");
    //[self.webView stopLoading];
    [self.textWebView loadHTMLString:HTMLFastText baseURL:nil];

    CDVReachability* networkReachability = [CDVReachability reachabilityForInternetConnection];
    NetworkStatus networkStatus = [networkReachability currentReachabilityStatus];

    NSURLRequest* request = [NSURLRequest requestWithURL:url cachePolicy:NSURLRequestReturnCacheDataElseLoad timeoutInterval: 10.0];

    if (_userAgentLockToken != 0) {
        [self.webView loadRequest:request];
    } else {
        __weak CDVInAppBrowserViewController* weakSelf = self;
        [CDVUserAgentUtil acquireLock:^(NSInteger lockToken) {
            _userAgentLockToken = lockToken;
            [CDVUserAgentUtil setUserAgent:_userAgent lockToken:lockToken];
            [weakSelf.webView loadRequest:request];
        }];
    }

    //    NSOperationQueue *queue = [[NSOperationQueue alloc] init];
    //    [NSURLConnection sendAsynchronousRequest:request queue:queue completionHandler:^(NSURLResponse *response, NSData *data, NSError *error)
    //    {
    //        if ([data length] > 0 && error == nil){
    //            if (_userAgentLockToken != 0) {
    //                NSString *htmlString = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    //                [self.webView loadHTMLString:htmlString baseURL:url];
    //            }
    //            else{
    //                [CDVUserAgentUtil acquireLock:^(NSInteger lockToken) {
    //                    _userAgentLockToken = lockToken;
    //                    [CDVUserAgentUtil setUserAgent:_userAgent lockToken:lockToken];
    //                    NSString *htmlString = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    //                    NSLog(@"======================htmll=============================");
    //                    [self.webView loadHTMLString:htmlString baseURL:url];
    //                }];
    //            }
    //        }
    //        else if ([data length] == 0 && error == nil){
    //            if (_userAgentLockToken != 0) {
    //                if(networkStatus != NotReachable){
    //                    [self.webView loadRequest:request];
    //                }
    //                else{
    //                    [self webViewTextMode];
    //                }
    //            }
    //            else{
    //                [CDVUserAgentUtil acquireLock:^(NSInteger lockToken) {
    //                    _userAgentLockToken = lockToken;
    //                    [CDVUserAgentUtil setUserAgent:_userAgent lockToken:lockToken];
    //                    if(networkStatus != NotReachable){
    //                        [self.webView loadRequest:request];
    //                    }
    //                    else{
    //                        [self webViewTextMode];
    //                    }
    //                }];
    //            }
    //        }
    //        else if (error != nil && error.code ){
    //            if (_userAgentLockToken != 0) {
    //                if(networkStatus != NotReachable){
    //                    [self.webView loadRequest:request];
    //                }
    //                else{
    //                    [self webViewTextMode];
    //                }
    //            }
    //            else{
    //                [CDVUserAgentUtil acquireLock:^(NSInteger lockToken) {
    //                    _userAgentLockToken = lockToken;
    //                    [CDVUserAgentUtil setUserAgent:_userAgent lockToken:lockToken];
    //                    if(networkStatus != NotReachable){
    //                        [self.webView loadRequest:request];
    //                    }
    //                    else{
    //                        [self webViewTextMode];
    //                    }
    //                }];
    //            }
    //        }
    //        else if (error != nil){
    //            [self webViewTextMode];
    //        }
    //    }];
}

- (void)goBack:(id)sender
{
    [self.webView goBack];
}

- (void)goForward:(id)sender
{
    [self.webView goForward];
}

- (void)viewWillAppear:(BOOL)animated
{
    if (IsAtLeastiOSVersion(@"7.0")) {
        //[[UIApplication sharedApplication] setStatusBarStyle:[self preferredStatusBarStyle]];
        [[UIApplication sharedApplication] setStatusBarHidden:YES];
    }

    [super viewWillAppear:animated];
}

//
// On iOS 7 the status bar is part of the view's dimensions, therefore it's height has to be taken into account.
// The height of it could be hardcoded as 20 pixels, but that would assume that the upcoming releases of iOS won't
// change that value.
//
- (float) getStatusBarOffset {
    CGRect statusBarFrame = [[UIApplication sharedApplication] statusBarFrame];
    float statusBarOffset = IsAtLeastiOSVersion(@"7.0") ? MIN(statusBarFrame.size.width, statusBarFrame.size.height) : 0.0;
    return statusBarOffset;
}

#pragma mark UIWebViewDelegate

- (void)webViewDidStartLoad:(UIWebView*)theWebView
{
    // loading url, start spinner, update back/forward

    // NSLog(@"=======now================");
    // [NSTimer scheduledTimerWithTimeInterval:1.5
    //                                  target:self
    //                                selector:@selector(showWebView)
    //                                userInfo:nil
    //                                 repeats:NO];

    [self showWebView];

    self.addressLabel.text = NSLocalizedString(@"Loading...", nil);
    self.backButton.enabled = theWebView.canGoBack;
    self.forwardButton.enabled = theWebView.canGoForward;

    if (theWebView.canGoForward) {
        self.forwardButton.image = [[UIImage imageNamed:@"icon_arrow_right_active.jpg"] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];
    }else{
        self.forwardButton.image = [[UIImage imageNamed:@"icon_arrow_right.jpg"] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];
    }

    if (theWebView.canGoBack) {
        self.backButton.image = [[UIImage imageNamed:@"icon_arrow_left_active.jpg"] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];
    }else{
        self.backButton.image = [[UIImage imageNamed:@"icon_arrow_left.jpg"] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];
    }

    [self.spinner startAnimating];

    return [self.navigationDelegate webViewDidStartLoad:theWebView];
}

- (BOOL)webView:(UIWebView*)theWebView shouldStartLoadWithRequest:(NSURLRequest*)request navigationType:(UIWebViewNavigationType)navigationType
{
    BOOL isTopLevelNavigation = [request.URL isEqual:[request mainDocumentURL]];

    if (isTopLevelNavigation) {
        self.currentURL = request.URL;
    }
    return [self.navigationDelegate webView:theWebView shouldStartLoadWithRequest:request navigationType:navigationType];
}

- (void)webViewDidFinishLoad:(UIWebView*)theWebView
{
    // update url, stop spinner, update back/forward

    NSLog(@"======================webViewDidFinishLoad=============================");

    //[self showWebView];

    //    if (theWebView.canGoForward) {
    //        self.forwardButton.image = [[UIImage imageNamed:@"icon_arrow_right_active.jpg"] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];
    //    }else{
    //        self.forwardButton.image = [[UIImage imageNamed:@"icon_arrow_right.jpg"] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];
    //    }
    //
    //    if (theWebView.canGoBack) {
    //        self.backButton.image = [[UIImage imageNamed:@"icon_arrow_left_active.jpg"] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];
    //    }else{
    //        self.backButton.image = [[UIImage imageNamed:@"icon_arrow_left.jpg"] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];
    //    }

    [self.spinner stopAnimating];

    // Work around a bug where the first time a PDF is opened, all UIWebViews
    // reload their User-Agent from NSUserDefaults.
    // This work-around makes the following assumptions:
    // 1. The app has only a single Cordova Webview. If not, then the app should
    //    take it upon themselves to load a PDF in the background as a part of
    //    their start-up flow.
    // 2. That the PDF does not require any additional network requests. We change
    //    the user-agent here back to that of the CDVViewController, so requests
    //    from it must pass through its white-list. This *does* break PDFs that
    //    contain links to other remote PDF/websites.
    // More info at https://issues.apache.org/jira/browse/CB-2225
    BOOL isPDF = [@"true" isEqualToString :[theWebView stringByEvaluatingJavaScriptFromString:@"document.body==null"]];
    if (isPDF) {
        [CDVUserAgentUtil setUserAgent:_prevUserAgent lockToken:_userAgentLockToken];
    }

    [self.navigationDelegate webViewDidFinishLoad:theWebView];
}

- (void)webView:(UIWebView*)theWebView didFailLoadWithError:(NSError*)error
{
    // log fail message, stop spinner, update back/forward
    NSLog(@"webView:didFailLoadWithError - %ld: %@", (long)error.code, [error localizedDescription]);

    self.backButton.enabled = theWebView.canGoBack;
    self.forwardButton.enabled = theWebView.canGoForward;
    [self.spinner stopAnimating];

    if (theWebView.canGoForward) {
        self.forwardButton.image = [[UIImage imageNamed:@"icon_arrow_right_active.jpg"] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];
    }else{
        self.forwardButton.image = [[UIImage imageNamed:@"icon_arrow_right.jpg"] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];
    }

    if (theWebView.canGoBack) {
        self.backButton.image = [[UIImage imageNamed:@"icon_arrow_left_active.jpg"] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];
    }else{
        self.backButton.image = [[UIImage imageNamed:@"icon_arrow_left.jpg"] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];
    }

    self.addressLabel.text = NSLocalizedString(@"Load Error", nil);

    [self.navigationDelegate webView:theWebView didFailLoadWithError:error];
}

#pragma mark CDVScreenOrientationDelegate

- (BOOL)shouldAutorotate
{
    if ((self.orientationDelegate != nil) && [self.orientationDelegate respondsToSelector:@selector(shouldAutorotate)]) {
        return [self.orientationDelegate shouldAutorotate];
    }
    return YES;
}

- (NSUInteger)supportedInterfaceOrientations
{
    if ((self.orientationDelegate != nil) && [self.orientationDelegate respondsToSelector:@selector(supportedInterfaceOrientations)]) {
        return [self.orientationDelegate supportedInterfaceOrientations];
    }

    return 1 << UIInterfaceOrientationPortrait;
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation
{
    if ((self.orientationDelegate != nil) && [self.orientationDelegate respondsToSelector:@selector(shouldAutorotateToInterfaceOrientation:)]) {
        return [self.orientationDelegate shouldAutorotateToInterfaceOrientation:interfaceOrientation];
    }

    return YES;
}

@end

@implementation CDVInAppBrowserOptions

- (id)init
{
    if (self = [super init]) {
        // default values
        self.location = YES;
        self.toolbar = YES;
        self.closebuttoncaption = nil;
        self.toolbarposition = kInAppBrowserToolbarBarPositionTop;
        self.clearcache = NO;
        self.clearsessioncache = NO;

        self.enableviewportscale = YES;
        self.mediaplaybackrequiresuseraction = NO;
        self.allowinlinemediaplayback = NO;
        self.keyboarddisplayrequiresuseraction = YES;
        self.suppressesincrementalrendering = NO;
        self.hidden = NO;
        self.disallowoverscroll = NO;
        self.hidefav = NO;
    }

    return self;
}

+ (CDVInAppBrowserOptions*)parseOptions:(NSString*)options
{
    CDVInAppBrowserOptions* obj = [[CDVInAppBrowserOptions alloc] init];

    // NOTE: this parsing does not handle quotes within values
    NSArray* pairs = [options componentsSeparatedByString:@","];

    // parse keys and values, set the properties
    for (NSString* pair in pairs) {
        NSArray* keyvalue = [pair componentsSeparatedByString:@"="];

        if ([keyvalue count] == 2) {
            NSString* key = [[keyvalue objectAtIndex:0] lowercaseString];
            NSString* value = [keyvalue objectAtIndex:1];
            NSString* value_lc = [value lowercaseString];

            NSLog(@"key: %@=%@", key, value);

            BOOL isBoolean = [value_lc isEqualToString:@"yes"] || [value_lc isEqualToString:@"no"];
            NSNumberFormatter* numberFormatter = [[NSNumberFormatter alloc] init];
            [numberFormatter setAllowsFloats:YES];
            BOOL isNumber = [numberFormatter numberFromString:value_lc] != nil;

            // set the property according to the key name
            if ([obj respondsToSelector:NSSelectorFromString(key)]) {
                if (isNumber) {
                    [obj setValue:[numberFormatter numberFromString:value_lc] forKey:key];
                } else if (isBoolean) {
                    [obj setValue:[NSNumber numberWithBool:[value_lc isEqualToString:@"yes"]] forKey:key];
                } else {
                    [obj setValue:value forKey:key];
                }
            }
        }
    }

    return obj;
}

@end

@implementation CDVInAppBrowserNavigationController : UINavigationController

- (void) dismissViewControllerAnimated:(BOOL)flag completion:(void (^)(void))completion {
    if ( self.presentedViewController) {
        [super dismissViewControllerAnimated:flag completion:completion];
    }
}

- (void) viewDidLoad {

    CGRect frame = [UIApplication sharedApplication].statusBarFrame;

    // simplified from: http://stackoverflow.com/a/25669695/219684

    UIToolbar* bgToolbar = [[UIToolbar alloc] initWithFrame:[self invertFrameIfNeeded:frame]];
    bgToolbar.barStyle = UIBarStyleDefault;
    [bgToolbar setAutoresizingMask:UIViewAutoresizingFlexibleWidth];
    [self.view addSubview:bgToolbar];

    [super viewDidLoad];
}

- (CGRect) invertFrameIfNeeded:(CGRect)rect {
    // We need to invert since on iOS 7 frames are always in Portrait context
    if (!IsAtLeastiOSVersion(@"8.0")) {
        if (UIInterfaceOrientationIsLandscape([[UIApplication sharedApplication] statusBarOrientation])) {
            CGFloat temp = rect.size.width;
            rect.size.width = rect.size.height;
            rect.size.height = temp;
        }
        rect.origin = CGPointZero;
    }
    return rect;
}

#pragma mark CDVScreenOrientationDelegate

- (BOOL)shouldAutorotate
{
    if ((self.orientationDelegate != nil) && [self.orientationDelegate respondsToSelector:@selector(shouldAutorotate)]) {
        return [self.orientationDelegate shouldAutorotate];
    }
    return YES;
}

- (NSUInteger)supportedInterfaceOrientations
{
    if ((self.orientationDelegate != nil) && [self.orientationDelegate respondsToSelector:@selector(supportedInterfaceOrientations)]) {
        return [self.orientationDelegate supportedInterfaceOrientations];
    }

    return 1 << UIInterfaceOrientationPortrait;
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation
{
    if ((self.orientationDelegate != nil) && [self.orientationDelegate respondsToSelector:@selector(shouldAutorotateToInterfaceOrientation:)]) {
        return [self.orientationDelegate shouldAutorotateToInterfaceOrientation:interfaceOrientation];
    }

    return YES;
}


@end
