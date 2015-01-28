/**
 * Created by davsket on 1/26/15.
 */
package org.apache.cordova.inappbrowser;

import org.apache.cordova.inappbrowser.AnimatedView;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;

public class JavaScriptInterface {
    private Activity activity;
    private WebView webView;
    private AnimatedView fastView;
    protected static final String LOG_TAG = "InAppBrowser";

    public JavaScriptInterface(Activity activity, WebView webView, AnimatedView fastView) {
        this.activity = activity;
        this.webView = webView;
        this.fastView = fastView;
    }

    public void showWebView(){
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(LOG_TAG, "CALLED!!!!");
                    try{
                        fastView.setVisibility(View.GONE);
                        webView.setAlpha(1);
                        Log.d(LOG_TAG, "CALLED ended!!!!");
                    }catch (Exception e){
                        Log.d(LOG_TAG, e.getMessage());
                    }
                }
            });
    }
}
