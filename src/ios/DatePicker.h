#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>

@interface DatePicker : CDVPlugin <UIPopoverControllerDelegate> {

}

- (void)show:(CDVInvokedUrlCommand*)command;
- (void)close:(CDVInvokedUrlCommand*)command;

@end
