//
//  CDVInAppBrowserAnimator.h
//  New Stand
//
//  Created by Miguel Martinez on 2/15/18.
//

#import <Foundation/Foundation.h>

@interface CDVInAppBrowserAnimator : NSObject <UIViewControllerAnimatedTransitioning> {
  BOOL visible;
  float duration;
};

@property BOOL visible;
@property float duration;

@end
