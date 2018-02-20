#import "CDVInAppBrowserAnimator.h"

@implementation CDVInAppBrowserAnimator

@synthesize visible, duration;

-(instancetype)init {
  self = [super init];
  
  if (self) {
    [self setVisible:NO];
    [self setDuration:0.5];
  }
  
  return self;
}

-(void)animateTransition:(id<UIViewControllerContextTransitioning>)transitionContext {
  UIView *fromView = [transitionContext viewForKey:UITransitionContextFromViewKey];
  UIView *toView = [transitionContext viewForKey:UITransitionContextToViewKey];
  UIViewAnimationOptions options = self.visible ? (
    UIViewAnimationOptionTransitionFlipFromRight
  ) : (
    UIViewAnimationOptionTransitionFlipFromLeft
  );

  dispatch_async(dispatch_get_main_queue(), ^{
    [UIView transitionFromView:fromView
                        toView:toView
                      duration:[self duration]
                       options:options
                    completion:^(BOOL completed) {
                      visible = !visible;
                      [transitionContext completeTransition:YES];
                    }];
  });
}

-(NSTimeInterval)transitionDuration:(id<UIViewControllerContextTransitioning>)transitionContext {
  return [self duration];
}

@end
