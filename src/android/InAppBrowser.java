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
package org.apache.cordova.inappbrowser;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import org.apache.cordova.inappbrowser.InAppBrowserDialog;
import org.apache.cordova.inappbrowser.GifMovieView;
import org.apache.cordova.inappbrowser.AnimatedView;
import org.apache.cordova.inappbrowser.JavaScriptInterface;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.provider.Browser;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.HttpAuthHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.Config;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaHttpAuthHandler;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginManager;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressLint("SetJavaScriptEnabled")
public class InAppBrowser extends CordovaPlugin {

    private static final String NULL = "null";
    protected static final String LOG_TAG = "InAppBrowser";
    private static final String SELF = "_self";
    private static final String SYSTEM = "_system";
    private static final String EXIT_EVENT = "exit";
    private static final String SHARE_EVENT = "share";
    private static final String FAV_EVENT = "fav";
    private static final String LOCATION = "location";
    private static final String ZOOM = "zoom";
    private static final String HIDDEN = "hidden";
    private static final String LOAD_START_EVENT = "loadstart";
    private static final String LOAD_STOP_EVENT = "loadstop";
    private static final String LOAD_ERROR_EVENT = "loaderror";
    private static final String CLEAR_ALL_CACHE = "clearcache";
    private static final String CLEAR_SESSION_CACHE = "clearsessioncache";
    private static final String HIDE_FAV = "hidefav";
    private static final String TEXT_MODE = "textmode";
    private static final String HARDWARE_BACK_BUTTON = "hardwareback";
    private static final String MEDIA_PLAYBACK_REQUIRES_USER_ACTION = "mediaPlaybackRequiresUserAction";
    private static final String SHOULD_PAUSE = "shouldPauseOnSuspend";

    private InAppBrowserDialog dialog;
    private LinearLayout main;
    private RelativeLayout toolbar;
    private WebView inAppWebView;
    private WebView textWebView;
    private EditText edittext;
    private CallbackContext callbackContext;
    private boolean showLocationBar = true;
    private boolean showZoomControls = true;
    private boolean openWindowHidden = false;
    private boolean clearAllCache= false;
    private boolean clearSessionCache=false;
    private boolean hideFav = false;
    private boolean textModeActivated = false;
    private boolean didLoad = false;
    private ImageButton activateTextModeButton;
    private ImageButton fav;
    private ImageButton share;
    private ImageButton back;
    private ImageButton backText;
    private ImageButton forward;

    private AnimatedView fastViewContainter;
    private Animation slide;
    private String HTMLFastText = "";
    private boolean hadwareBackButton = true;
    private boolean mediaPlaybackRequiresUserGesture = false;
    private boolean shouldPauseInAppBrowser = false;

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action the action to execute.
     * @param args JSONArry of arguments for the plugin.
     * @param callbackContext the callbackContext used when calling back into JavaScript.
     * @return A PluginResult object with a status and message.
     */
    public boolean execute(String action, CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("open")) {
            this.callbackContext = callbackContext;
            final String url = args.getString(0);
            String t = args.optString(1);
            if (t == null || t.equals("") || t.equals(NULL)) {
                t = SELF;
            }
            final String target = t;
            final HashMap<String, Boolean> features = parseFeature(args.optString(2));

            HTMLFastText = args.optString(3);
            textModeActivated = false;

            LOG.d(LOG_TAG, "target = " + target);

            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String result = "";
                    // SELF
                    if (SELF.equals(target)) {
                        LOG.d(LOG_TAG, "in self");
                        /* This code exists for compatibility between 3.x and 4.x versions of Cordova.
                         * Previously the Config class had a static method, isUrlWhitelisted(). That
                         * responsibility has been moved to the plugins, with an aggregating method in
                         * PluginManager.
                         */
                        Boolean shouldAllowNavigation = null;
                        if (url.startsWith("javascript:")) {
                            shouldAllowNavigation = true;
                        }
                        if (shouldAllowNavigation == null) {
                            try {
                                Method iuw = Config.class.getMethod("isUrlWhiteListed", String.class);
                                shouldAllowNavigation = (Boolean)iuw.invoke(null, url);
                            } catch (NoSuchMethodException e) {
                                LOG.d(LOG_TAG, e.getLocalizedMessage());
                            } catch (IllegalAccessException e) {
                                LOG.d(LOG_TAG, e.getLocalizedMessage());
                            } catch (InvocationTargetException e) {
                                LOG.d(LOG_TAG, e.getLocalizedMessage());
                            }
                        }
                        if (shouldAllowNavigation == null) {
                            try {
                                Method gpm = webView.getClass().getMethod("getPluginManager");
                                PluginManager pm = (PluginManager)gpm.invoke(webView);
                                Method san = pm.getClass().getMethod("shouldAllowNavigation", String.class);
                                shouldAllowNavigation = (Boolean)san.invoke(pm, url);
                            } catch (NoSuchMethodException e) {
                                LOG.d(LOG_TAG, e.getLocalizedMessage());
                            } catch (IllegalAccessException e) {
                                LOG.d(LOG_TAG, e.getLocalizedMessage());
                            } catch (InvocationTargetException e) {
                                LOG.d(LOG_TAG, e.getLocalizedMessage());
                            }
                        }
                        // load in webview
                        if (Boolean.TRUE.equals(shouldAllowNavigation)) {
                            LOG.d(LOG_TAG, "loading in webview");
                            inAppWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                            webView.loadUrl(url);
                        }
                        //Load the dialer
                        else if (url.startsWith(WebView.SCHEME_TEL))
                        {
                            try {
                                LOG.d(LOG_TAG, "loading in dialer");
                                Intent intent = new Intent(Intent.ACTION_DIAL);
                                intent.setData(Uri.parse(url));
                                cordova.getActivity().startActivity(intent);
                            } catch (android.content.ActivityNotFoundException e) {
                                LOG.e(LOG_TAG, "Error dialing " + url + ": " + e.toString());
                            }
                        }
                        // load in InAppBrowser
                        else {
                            LOG.d(LOG_TAG, "loading in InAppBrowser");
                            result = showWebPage(url, features);
                        }
                    }
                    // SYSTEM
                    else if (SYSTEM.equals(target)) {
                        LOG.d(LOG_TAG, "in system");
                        result = openExternal(url);
                    }
                    // BLANK - or anything else
                    else {
                        LOG.d(LOG_TAG, "in blank");
                        result = showWebPage(url, features);
                    }

                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
                    pluginResult.setKeepCallback(true);
                    callbackContext.sendPluginResult(pluginResult);
                }
            });
        }
        else if (action.equals("close")) {
            closeDialog();
        }
        else if (action.equals("injectScriptCode")) {
            String jsWrapper = null;
            if (args.getBoolean(1)) {
                jsWrapper = String.format("(function(){prompt(JSON.stringify([eval(%%s)]), 'gap-iab://%s')})()", callbackContext.getCallbackId());
            }
            injectDeferredObject(args.getString(0), jsWrapper);
        }
        else if (action.equals("injectScriptFile")) {
            String jsWrapper;
            if (args.getBoolean(1)) {
                jsWrapper = String.format("(function(d) { var c = d.createElement('script'); c.src = %%s; c.onload = function() { prompt('', 'gap-iab://%s'); }; d.body.appendChild(c); })(document)", callbackContext.getCallbackId());
            } else {
                jsWrapper = "(function(d) { var c = d.createElement('script'); c.src = %s; d.body.appendChild(c); })(document)";
            }
            injectDeferredObject(args.getString(0), jsWrapper);
        }
        else if (action.equals("injectStyleCode")) {
            String jsWrapper;
            if (args.getBoolean(1)) {
                jsWrapper = String.format("(function(d) { var c = d.createElement('style'); c.innerHTML = %%s; d.body.appendChild(c); prompt('', 'gap-iab://%s');})(document)", callbackContext.getCallbackId());
            } else {
                jsWrapper = "(function(d) { var c = d.createElement('style'); c.innerHTML = %s; d.body.appendChild(c); })(document)";
            }
            injectDeferredObject(args.getString(0), jsWrapper);
        }
        else if (action.equals("injectStyleFile")) {
            String jsWrapper;
            if (args.getBoolean(1)) {
                jsWrapper = String.format("(function(d) { var c = d.createElement('link'); c.rel='stylesheet'; c.type='text/css'; c.href = %%s; d.head.appendChild(c); prompt('', 'gap-iab://%s');})(document)", callbackContext.getCallbackId());
            } else {
                jsWrapper = "(function(d) { var c = d.createElement('link'); c.rel='stylesheet'; c.type='text/css'; c.href = %s; d.head.appendChild(c); })(document)";
            }
            injectDeferredObject(args.getString(0), jsWrapper);
        }
        else if (action.equals("show")) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog.show();
                }
            });
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
            pluginResult.setKeepCallback(true);
            this.callbackContext.sendPluginResult(pluginResult);
        }
        else {
            return false;
        }
        return true;
    }

    /**
     * Called when the view navigates.
     */
    @Override
    public void onReset() {
        closeDialog();
    }

    /**
     * Called when the system is about to start resuming a previous activity.
     */
    @Override
    public void onPause(boolean multitasking) {
        if (shouldPauseInAppBrowser) {
            inAppWebView.onPause();
        }
    }

    /**
     * Called when the activity will start interacting with the user.
     */
    @Override
    public void onResume(boolean multitasking) {
        if (shouldPauseInAppBrowser) {
            inAppWebView.onResume();
        }
    }

    /**
     * Called by AccelBroker when listener is to be shut down.
     * Stop listener.
     */
    public void onDestroy() {
        closeDialog();
    }

    /**
     * Inject an object (script or style) into the InAppBrowser WebView.
     *
     * This is a helper method for the inject{Script|Style}{Code|File} API calls, which
     * provides a consistent method for injecting JavaScript code into the document.
     *
     * If a wrapper string is supplied, then the source string will be JSON-encoded (adding
     * quotes) and wrapped using string formatting. (The wrapper string should have a single
     * '%s' marker)
     *
     * @param source      The source object (filename or script/style text) to inject into
     *                    the document.
     * @param jsWrapper   A JavaScript string to wrap the source string in, so that the object
     *                    is properly injected, or null if the source string is JavaScript text
     *                    which should be executed directly.
     */
    private void injectDeferredObject(String source, String jsWrapper) {
        final String scriptToInject;
        if (jsWrapper != null) {
            org.json.JSONArray jsonEsc = new org.json.JSONArray();
            jsonEsc.put(source);
            String jsonRepr = jsonEsc.toString();
            String jsonSourceString = jsonRepr.substring(1, jsonRepr.length()-1);
            scriptToInject = String.format(jsWrapper, jsonSourceString);
        } else {
            scriptToInject = source;
        }
        LOG.d(LOG_TAG, "MONOKU injecting: "+source);
        final String finalScriptToInject = scriptToInject;
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @SuppressLint("NewApi")
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    // This action will have the side-effect of blurring the currently focused element
                    inAppWebView.loadUrl("javascript:" + finalScriptToInject);
                    textWebView.loadUrl("javascript:" + finalScriptToInject);
                } else {
                    inAppWebView.evaluateJavascript(finalScriptToInject, null);
                    textWebView.evaluateJavascript(finalScriptToInject, null);
                }
                LOG.d(LOG_TAG, finalScriptToInject);
            }
        });
    }

    /**
     * Put the list of features into a hash map
     *
     * @param optString
     * @return
     */
    private HashMap<String, Boolean> parseFeature(String optString) {
        if (optString.equals(NULL)) {
            return null;
        } else {
            HashMap<String, Boolean> map = new HashMap<String, Boolean>();
            StringTokenizer features = new StringTokenizer(optString, ",");
            StringTokenizer option;
            while(features.hasMoreElements()) {
                option = new StringTokenizer(features.nextToken(), "=");
                if (option.hasMoreElements()) {
                    String key = option.nextToken();
                    Boolean value = option.nextToken().equals("no") ? Boolean.FALSE : Boolean.TRUE;
                    map.put(key, value);
                }
            }
            return map;
        }
    }

    /**
     * Display a new browser with the specified URL.
     *
     * @param url the url to load.
     * @return "" if ok, or error message.
     */
    public String openExternal(String url) {
        try {
            Intent intent = null;
            intent = new Intent(Intent.ACTION_VIEW);
            // Omitting the MIME type for file: URLs causes "No Activity found to handle Intent".
            // Adding the MIME type to http: URLs causes them to not be handled by the downloader.
            Uri uri = Uri.parse(url);
            if ("file".equals(uri.getScheme())) {
                intent.setDataAndType(uri, webView.getResourceApi().getMimeType(uri));
            } else {
                intent.setData(uri);
            }
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, cordova.getActivity().getPackageName());
            this.cordova.getActivity().startActivity(intent);
            return "";
        } catch (android.content.ActivityNotFoundException e) {
            LOG.d(LOG_TAG, "InAppBrowser: Error loading url "+url+":"+ e.toString());
            return e.toString();
        }
    }

    /**
     * Closes the dialog
     */
    public void closeDialog() {
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final WebView childView = inAppWebView;
                // The JS protects against multiple calls, so this should happen only when
                // closeDialog() is called by other native code.
                if (childView == null) {
                    return;
                }

                childView.setWebViewClient(new WebViewClient() {
                    // NB: wait for about:blank before dismissing
                    public void onPageFinished(WebView view, String url) {
                        if (dialog != null) {
                            dialog.dismiss();
                            dialog = null;
                        }
                    }
                });
                // NB: From SDK 19: "If you call methods on WebView from any thread
                // other than your app's UI thread, it can cause unexpected results."
                // http://developer.android.com/guide/webapps/migrating.html#Threads
                childView.loadUrl("about:blank");

                try {
                    JSONObject obj = new JSONObject();
                    obj.put("type", EXIT_EVENT);
                    sendUpdate(obj, false);
                } catch (JSONException ex) {
                    LOG.d(LOG_TAG, "Should never happen");
                }
            }
        });

        try {
            JSONObject obj = new JSONObject();
            obj.put("type", EXIT_EVENT);
            sendUpdate(obj, false);
        } catch (JSONException ex) {
            LOG.d(LOG_TAG, "Should never happen");
        }
    }

    /**
     * Notifies Sharing
     */
    public void sharePage(){
        try {
            JSONObject obj = new JSONObject();
            obj.put("type", SHARE_EVENT);
            obj.put("url", this.inAppWebView.getUrl());
            LOG.d(LOG_TAG, ">>>>>>>>>>>>>>>>>>>>> SHARE");
            sendUpdate(obj, true);
//            injectDeferredObject("try{window.cordovaInappBrowserShareCallBack();}catch(e){}", null);
        } catch (JSONException ex) {
            LOG.d(LOG_TAG, "Should never happen SHARE");
        }
    }

    public void showAllButtons(){
        fav.setVisibility(View.VISIBLE);
        share.setVisibility(View.VISIBLE);
        if( !textModeActivated ) {
            back.setVisibility(View.VISIBLE);
            forward.setVisibility(View.VISIBLE);
            backText.setVisibility(View.GONE);
        }else{
            back.setVisibility(View.GONE);
            forward.setVisibility(View.GONE);
            backText.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Activates TextMode
     */
    public void textMode(){
        try {
            textModeActivated = true;
            // activateTextModeButton.setAlpha(0.7f);
            activateTextModeButton.setEnabled(false);
            // inAppWebView.stopLoading();
            // textWebView.setVisibility(View.VISIBLE);
            // inAppWebView.setVisibility(View.GONE);
//            fastViewContainter.setVisibility(View.GONE);

            ValueAnimator va = ValueAnimator.ofInt(fastViewContainter.getHeight(), 0);
            va.setDuration(250);
            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    Integer value = (Integer) animation.getAnimatedValue();
                    // LOG.d(LOG_TAG, "animating: "+value);
                    fastViewContainter.getLayoutParams().height = value.intValue();
//                    fastViewContainter.setTranslationY(-value.intValue());
//                    fastViewContainter.requestLayout();
                    fastViewContainter.requestLayout();
                }
            });
            va.start();

            showAllButtons();

            // WebSettings webSettings = inAppWebView.getSettings();
            // webSettings.setJavaScriptEnabled(false);
//            inAppWebView.destroy();
//            inAppWebView.loadDataWithBaseURL(null, null, "text/html", "utf-8", null);
//            fastViewContainter.getLayoutParams().height = 250;
            LOG.d(LOG_TAG, "=====TEXTMODE");
            JSONObject obj = new JSONObject();
            obj.put("type", TEXT_MODE);
            LOG.d(LOG_TAG, ">>>>>>>>>>>>>>>>>>>>> TEXT MODE");
            sendUpdate(obj, true);
        } catch (JSONException ex) {
            LOG.d(LOG_TAG, "Should never happen TEXTMODE");
        } finally {
//            fastViewContainter.setVisibility(LinearLayout.GONE);
//            fastViewContainter.startAnimation(slide);
//            ValueAnimator va = ValueAnimator.ofInt(0, fastViewContainter.getMeasuredHeight());
//            va.setDuration(800);
//            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//                public void onAnimationUpdate(ValueAnimator animation) {
//                    Integer value = (Integer) animation.getAnimatedValue();
//                    fastViewContainter.getLayoutParams().height = value.intValue();
////                    fastViewContainter.setTranslationY(-value.intValue());
////                    fastViewContainter.requestLayout();
//                    fastViewContainter.requestLayout();
//                }
//            });
//            this.inAppWebView.setAlpha(1);
//            injectDeferredObject("try{document.querySelector('html').style.display='block';}catch(e){}", null);

        }
    }

    /**
     * Notifies Sharing
     */
    public void favPage(){
        try {
            JSONObject obj = new JSONObject();
            obj.put("type", FAV_EVENT);
            obj.put("url", this.inAppWebView.getUrl());
            sendUpdate(obj, true);
            injectDeferredObject("try{window.cordovaInappBrowserFavCallBack();}catch(e){}", null);
        } catch (Exception ex) {
            LOG.d(LOG_TAG, "Should never happen SHARE");
            LOG.d(LOG_TAG, "MONOKU, "+ ex.getMessage());
        }
    }

    /**
     * Checks to see if it is possible to go back one page in history, then does so.
     */
    public void goBack() {
        if (this.inAppWebView.canGoBack()) {
            this.inAppWebView.goBack();
        }
    }

    /**
     * Checks to see if it is possible to go back one page in history, then does so.
     */
    private void goBackText() {
        textModeActivated = false;
        activateTextModeButton.setEnabled(true);
        if(!didLoad){
            ValueAnimator va = ValueAnimator.ofInt(0, main.getHeight()-toolbar.getHeight());
            va.setDuration(250);
            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    Integer value = (Integer) animation.getAnimatedValue();
                    LOG.d(LOG_TAG, "animating: "+value);
                    fastViewContainter.getLayoutParams().height = value.intValue();
    //                    fastViewContainter.setTranslationY(-value.intValue());
    //                    fastViewContainter.requestLayout();
                    fastViewContainter.requestLayout();
                }
            });
            va.start();
            back.setVisibility(View.GONE);
            forward.setVisibility(View.GONE);
            backText.setVisibility(View.GONE);
            share.setVisibility(View.GONE);
            fav.setVisibility(View.GONE);
        }else{
            textWebView.setVisibility(View.GONE);
    //         ValueAnimator va = ValueAnimator.ofInt(textWebView.getHeight(), 0);
    //         va.setDuration(250);
    //         va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
    //             public void onAnimationUpdate(ValueAnimator animation) {
    //                 Integer value = (Integer) animation.getAnimatedValue();
    //                 LOG.d(LOG_TAG, "animating: "+value);
    //                 textWebView.getLayoutParams().height = value.intValue();
    // //                    fastViewContainter.setTranslationY(-value.intValue());
    // //                    fastViewContainter.requestLayout();
    //                 textWebView.requestLayout();
    //             }
    //         });
            showAllButtons();
        }
        // if (this.inAppWebView.canGoBack()) {
        //     this.inAppWebView.goBack();
        // }
    }
    /**
     * Can the web browser go back?
     * @return boolean
     */
    public boolean canGoBack() {
        return this.inAppWebView.canGoBack();
    }

    /**
     * Has the user set the hardware back button to go back
     * @return boolean
     */
    public boolean hardwareBack() {
        return hadwareBackButton;
    }

    /**
     * Checks to see if it is possible to go forward one page in history, then does so.
     */
    private void goForward() {
        if (this.inAppWebView.canGoForward()) {
            this.inAppWebView.goForward();
        }
    }

    /**
     * Navigate to the new page
     *
     * @param url to load
     */
    private void navigate(String url) {
        InputMethodManager imm = (InputMethodManager)this.cordova.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edittext.getWindowToken(), 0);

        if (!url.startsWith("http") && !url.startsWith("file:")) {
            this.inAppWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            this.inAppWebView.loadUrl("http://" + url);
        } else {
            this.inAppWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            this.inAppWebView.loadUrl(url);
        }
        this.inAppWebView.requestFocus();
    }


    /**
     * Should we show the location bar?
     *
     * @return boolean
     */
    private boolean getShowLocationBar() {
        return this.showLocationBar;
    }

    private InAppBrowser getInAppBrowser(){
        return this;
    }

    /**
     * Display a new browser with the specified URL.
     *
     * @param url the url to load.
     * @param features jsonObject
     */
    public String showWebPage(final String url, HashMap<String, Boolean> features) {
        // Determine if we should hide the location bar.
        showLocationBar = true;
        showZoomControls = true;
        openWindowHidden = false;
        mediaPlaybackRequiresUserGesture = false;

        if (features != null) {
            Boolean show = features.get(LOCATION);
            if (show != null) {
                showLocationBar = show.booleanValue();
            }
            Boolean zoom = features.get(ZOOM);
            if (zoom != null) {
                showZoomControls = zoom.booleanValue();
            }
            Boolean hidden = features.get(HIDDEN);
            if (hidden != null) {
                openWindowHidden = hidden.booleanValue();
            }
            Boolean hardwareBack = features.get(HARDWARE_BACK_BUTTON);
            if (hardwareBack != null) {
                hadwareBackButton = hardwareBack.booleanValue();
            }
            hideFav = features.get(HIDE_FAV) != null ? true : false;

            Boolean mediaPlayback = features.get(MEDIA_PLAYBACK_REQUIRES_USER_ACTION);
            if (mediaPlayback != null) {
                mediaPlaybackRequiresUserGesture = mediaPlayback.booleanValue();
            }
            // Boolean cache = features.get(CLEAR_ALL_CACHE);
            // if (cache != null) {
            //     clearAllCache = cache.booleanValue();
            // } else {
            //     cache = features.get(CLEAR_SESSION_CACHE);
            //     if (cache != null) {
            //         clearSessionCache = cache.booleanValue();
            //     }
            // }
            Boolean shouldPause = features.get(SHOULD_PAUSE);
            if (shouldPause != null) {
                shouldPauseInAppBrowser = shouldPause.booleanValue();
            }
        }

        final CordovaWebView thatWebView = this.webView;

        // Create dialog in new thread
        Runnable runnable = new Runnable() {
            /**
             * Convert our DIP units to Pixels
             *
             * @return int
             */
            private int dpToPixels(int dipValue) {
                int value = (int) TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP,
                                                            (float) dipValue,
                                                            cordova.getActivity().getResources().getDisplayMetrics()
                );

                return value;
            }

            @SuppressLint("NewApi")
            public void run() {

                // CB-6702 InAppBrowser hangs when opening more than one instance
                if (dialog != null) {
                    dialog.dismiss();
                };

                // Let's create the main dialog
                dialog = new InAppBrowserDialog(cordova.getActivity(), android.R.style.Theme_NoTitleBar);
                dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
                dialog. getWindow().setFlags(LayoutParams.FLAG_FULLSCREEN,
                        LayoutParams.FLAG_FULLSCREEN);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setCancelable(true);
                dialog.setInAppBroswer(getInAppBrowser());

                // Main container layout
                main = new LinearLayout(cordova.getActivity());
                main.setOrientation(LinearLayout.VERTICAL);

                // Toolbar layout
                RelativeLayout toolbar = new RelativeLayout(cordova.getActivity());
                //Please, no more black!
                toolbar.setBackgroundColor(Color.WHITE);
                toolbar.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                toolbar.setPadding(this.dpToPixels(8), this.dpToPixels(10), this.dpToPixels(8), this.dpToPixels(10));

                toolbar.setHorizontalGravity(Gravity.LEFT);
                toolbar.setVerticalGravity(Gravity.TOP);

                // Action Button Container layout
                RelativeLayout actionButtonContainer = new RelativeLayout(cordova.getActivity());
                actionButtonContainer.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                actionButtonContainer.setHorizontalGravity(Gravity.LEFT);
                actionButtonContainer.setVerticalGravity(Gravity.TOP);
                actionButtonContainer.setId(Integer.valueOf(1));

                // Action Button Container layout
                RelativeLayout secondButtonContainer = new RelativeLayout(cordova.getActivity());
                secondButtonContainer.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                secondButtonContainer.setHorizontalGravity(Gravity.RIGHT);
                secondButtonContainer.setVerticalGravity(Gravity.CENTER_VERTICAL);
                secondButtonContainer.setId(Integer.valueOf(8));

                // Back button
                back = new ImageButton(cordova.getActivity());
                RelativeLayout.LayoutParams backLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                backLayoutParams.addRule(RelativeLayout.ALIGN_LEFT);
                back.setLayoutParams(backLayoutParams);
                // back.setWidth(this.dpToPixels(100));
                back.setContentDescription("Back Button");
                back.setId(Integer.valueOf(2));
                back.setVisibility(View.GONE);
                Resources activityRes = cordova.getActivity().getResources();
                int backResId = activityRes.getIdentifier("icon_arrow_left", "drawable", cordova.getActivity().getPackageName());
                Drawable backIcon = activityRes.getDrawable(backResId);
                if (Build.VERSION.SDK_INT >= 16)
                    back.setBackground(backIcon);
                else
                    back.setBackgroundDrawable(backIcon);
                back.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        goBack();
                    }
                });

                // Back button
                backText = new ImageButton(cordova.getActivity());
                RelativeLayout.LayoutParams backTextLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                backTextLayoutParams.addRule(RelativeLayout.ALIGN_LEFT);
                backText.setLayoutParams(backTextLayoutParams);
                // backText.setWidth(this.dpToPixels(100));
                backText.setContentDescription("BackText Button");
                backText.setId(Integer.valueOf(21));
                backText.setVisibility(View.GONE);
                int backTextResId = activityRes.getIdentifier("icon_arrow_left_active", "drawable", cordova.getActivity().getPackageName());
                Drawable backTextIcon = activityRes.getDrawable(backTextResId);
                if (Build.VERSION.SDK_INT >= 16)
                    backText.setBackground(backTextIcon);
                else
                    backText.setBackgroundDrawable(backTextIcon);
                backText.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        goBackText();
                    }
                });



                // Forward button
                forward = new ImageButton(cordova.getActivity());
                RelativeLayout.LayoutParams forwardLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                forwardLayoutParams.setMargins(this.dpToPixels(65), 0, 0, 0);
                forwardLayoutParams.addRule(RelativeLayout.ALIGN_LEFT);
                forward.setLayoutParams(forwardLayoutParams);
                forward.setContentDescription("Forward Button");
                forward.setId(Integer.valueOf(3));
                forward.setVisibility(View.GONE);
                int fwdResId = activityRes.getIdentifier("icon_arrow_right", "drawable", cordova.getActivity().getPackageName());
                Drawable fwdIcon = activityRes.getDrawable(fwdResId);
                if (Build.VERSION.SDK_INT >= 16)
                    forward.setBackground(fwdIcon);
                else
                    forward.setBackgroundDrawable(fwdIcon);
                forward.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        goForward();
                    }
                });

                // Edit Text Box
                edittext = new EditText(cordova.getActivity());
                RelativeLayout.LayoutParams textLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                textLayoutParams.addRule(RelativeLayout.LEFT_OF, 5);
                edittext.setLayoutParams(textLayoutParams);
                edittext.setId(Integer.valueOf(4));
                edittext.setSingleLine(true);
                edittext.setText(url);
                edittext.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
                edittext.setImeOptions(EditorInfo.IME_ACTION_GO);
                edittext.setInputType(InputType.TYPE_NULL); // Will not except input... Makes the text NON-EDITABLE
                edittext.setOnKeyListener(new View.OnKeyListener() {
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        // If the event is a key-down event on the "enter" button
                        if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                          navigate(edittext.getText().toString());
                          return true;
                        }
                        return false;
                    }
                });

                // Close/Done button
                ImageButton close = new ImageButton(cordova.getActivity());
                RelativeLayout.LayoutParams closeLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                closeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 5);
                close.setLayoutParams(closeLayoutParams);
                close.setContentDescription("Close Button");
                close.setId(Integer.valueOf(5));
                int closeResId = activityRes.getIdentifier("icon_close", "drawable", cordova.getActivity().getPackageName());
                Drawable closeIcon = activityRes.getDrawable(closeResId);
                if (Build.VERSION.SDK_INT >= 16)
                    close.setBackground(closeIcon);
                else
                    close.setBackgroundDrawable(closeIcon);
                // back.setPadding(0, this.dpToPixels(10), 0, this.dpToPixels(10));
                // if (Build.VERSION.SDK_INT >= 16)
                //     close.getAdjustViewBounds();

                close.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        closeDialog();
                    }
                });

                // Share button
                share = new ImageButton(cordova.getActivity());
                RelativeLayout.LayoutParams shareLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                shareLayoutParams.addRule(RelativeLayout.LEFT_OF, 5);
                shareLayoutParams.setMargins(0, 0, this.dpToPixels(10), 0);
                share.setLayoutParams(shareLayoutParams);
                share.setContentDescription("Share Button");
                share.setId(Integer.valueOf(10));
                share.setVisibility(View.GONE);
                int shareResId = activityRes.getIdentifier("icon_share", "drawable", cordova.getActivity().getPackageName());
                Drawable shareIcon = activityRes.getDrawable(shareResId);
                if (Build.VERSION.SDK_INT >= 16)
                    share.setBackground(shareIcon);
                else
                    share.setBackgroundDrawable(shareIcon);
                share.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        sharePage();
                    }
                });

                // Fav button
                fav = new ImageButton(cordova.getActivity());
                RelativeLayout.LayoutParams favLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                favLayoutParams.addRule(RelativeLayout.LEFT_OF, 10);
                favLayoutParams.setMargins(0, 0, this.dpToPixels(12), 0);
                fav.setLayoutParams(favLayoutParams);
                fav.setContentDescription("Fav Button");
                fav.setId(Integer.valueOf(9));
                fav.setVisibility(View.GONE);
                int favResId = activityRes.getIdentifier("icon_fav", "drawable", cordova.getActivity().getPackageName());
                Drawable favIcon = activityRes.getDrawable(favResId);
                if (Build.VERSION.SDK_INT >= 16)
                    fav.setBackground(favIcon);
                else
                    fav.setBackgroundDrawable(favIcon);
                fav.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        favPage();
                    }
                });

                // *********************************
                // ****** FAST VIEW
                // *********************************

                fastViewContainter = new AnimatedView(cordova.getActivity());
                fastViewContainter.setOrientation(LinearLayout.VERTICAL);
                fastViewContainter.setBackgroundColor(Color.parseColor("#f0f0f0"));
                fastViewContainter.setGravity(Gravity.CENTER_HORIZONTAL);
                RelativeLayout.LayoutParams fastViewContainterParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

                activateTextModeButton = new ImageButton(cordova.getActivity());
                RelativeLayout.LayoutParams activateTextModeButtonParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                activateTextModeButtonParams.addRule(RelativeLayout.ALIGN_LEFT);
                activateTextModeButton.setLayoutParams(activateTextModeButtonParams);
                activateTextModeButton.setContentDescription("Fast Preview");
                LinearLayout.LayoutParams fvlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                fvlp.setMargins(0, 180, 0, 0);
                activateTextModeButton.setLayoutParams(fvlp);
                activateTextModeButton.setId(Integer.valueOf(20));
                int fastResId = activityRes.getIdentifier("icon_fast", "drawable", cordova.getActivity().getPackageName());
                Drawable fastIcon = activityRes.getDrawable(fastResId);
                if (Build.VERSION.SDK_INT >= 16)
                    activateTextModeButton.setBackground(fastIcon);
                else
                    activateTextModeButton.setBackgroundDrawable(fastIcon);
                activateTextModeButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        textMode();
                    }
                });
                fastViewContainter.addView(activateTextModeButton);

                TextView fastText = new TextView(cordova.getActivity());
                fastText.setText("Tap Offline View to see content faster.");
                fastText.setTextColor(Color.parseColor("#004251"));
                fastText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                fastText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                fastText.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams ftlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                ftlp.setMargins(0, 0, 0, 20);
                fastText.setLayoutParams(ftlp);
                fastViewContainter.addView(fastText);

                InputStream stream = null;
                try {
                    int loaderResId = activityRes.getIdentifier("loader", "raw", cordova.getActivity().getPackageName());
                    stream = cordova.getActivity().getApplicationContext().getResources().openRawResource(loaderResId);
                    LOG.d(LOG_TAG, String.valueOf(stream));
                }catch( Exception e ){
                    e.printStackTrace();
                }
                GifMovieView gifView = new GifMovieView(cordova.getActivity().getApplicationContext(), stream);
                gifView.setLayoutParams(new LinearLayout.LayoutParams(5, 5));
                fastViewContainter.addView(gifView);



                // WebView
                inAppWebView = new WebView(cordova.getActivity());
                textWebView = new WebView(cordova.getActivity());
                inAppWebView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                textWebView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                inAppWebView.setId(Integer.valueOf(6));
                textWebView.setId(Integer.valueOf(66));

                WebViewClient client = new InAppBrowserClient(thatWebView, edittext);
                inAppWebView.setWebViewClient(client);
                inAppWebView.setWebChromeClient(new InAppChromeClient(thatWebView, (InAppBrowserClient)client));
                textWebView.setWebChromeClient(new InAppChromeClient(thatWebView, (InAppBrowserClient)client));

                WebSettings settings = inAppWebView.getSettings();
                settings.setJavaScriptEnabled(true);
                settings.setJavaScriptCanOpenWindowsAutomatically(true);

                settings.setBuiltInZoomControls(true);
                settings.setPluginState(android.webkit.WebSettings.PluginState.ON);
                
                if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    settings.setMediaPlaybackRequiresUserGesture(mediaPlaybackRequiresUserGesture);
                }
                String overrideUserAgent = preferences.getString("OverrideUserAgent", null);
                String appendUserAgent = preferences.getString("AppendUserAgent", null);

                if (overrideUserAgent != null) {
                    settings.setUserAgentString(overrideUserAgent);
                }
                if (appendUserAgent != null) {
                    settings.setUserAgentString(settings.getUserAgentString() + appendUserAgent);
                }

                WebSettings textsettings = textWebView.getSettings();
                textsettings.setJavaScriptEnabled(true);
                textsettings.setJavaScriptCanOpenWindowsAutomatically(true);
                textsettings.setBuiltInZoomControls(true);
                textsettings.setPluginState(android.webkit.WebSettings.PluginState.ON);

                //Toggle whether this is enabled or not!
                Bundle appSettings = cordova.getActivity().getIntent().getExtras();
                boolean enableDatabase = appSettings == null ? true : appSettings.getBoolean("InAppBrowserStorageEnabled", true);
                if (enableDatabase) {
                    String databasePath = cordova.getActivity().getApplicationContext().getDir("inAppBrowserDB", Context.MODE_PRIVATE).getPath();
                    settings.setDatabasePath(databasePath);
                    settings.setDatabaseEnabled(true);
                }
                settings.setDomStorageEnabled(true);

                if (clearAllCache) {
                    CookieManager.getInstance().removeAllCookie();
                } else if (clearSessionCache) {
                    CookieManager.getInstance().removeSessionCookie();
                }

                String mime = "text/html";
                String encoding = "utf-8";

                inAppWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                inAppWebView.loadUrl(url);
                textWebView.loadDataWithBaseURL(null, HTMLFastText, mime, encoding, null);
                inAppWebView.setId(Integer.valueOf(6));
                inAppWebView.getSettings().setLoadWithOverviewMode(true);
                inAppWebView.getSettings().setUseWideViewPort(true);
                inAppWebView.requestFocus();
                inAppWebView.requestFocusFromTouch();
                textWebView.requestFocus();
                textWebView.requestFocusFromTouch();

                // Add the back and forward buttons to our action button container layout
                actionButtonContainer.addView(back);
                actionButtonContainer.addView(backText);
                actionButtonContainer.addView(forward);

                // Add the back and forward buttons to our action button container layout
                secondButtonContainer.addView(close);
                if( hideFav ){
                    fav.setVisibility(View.GONE);
                }
                secondButtonContainer.addView(fav);
                secondButtonContainer.addView(share);

                // Add the views to our toolbar
                toolbar.addView(actionButtonContainer);
                toolbar.addView(secondButtonContainer);

                // Don't add the toolbar if its been disabled
                if (getShowLocationBar()) {
                    // Add our toolbar to our main view/layout
                    main.addView(toolbar);
                }

                // Add Fast View
                main.addView(fastViewContainter, fastViewContainterParams);

                // Add our webview to our main view/layout
                main.addView(textWebView);
                main.addView(inAppWebView);

                LayoutParams lp = new LayoutParams();
                lp.copyFrom(dialog.getWindow().getAttributes());
                lp.width = LayoutParams.MATCH_PARENT;
                lp.height = LayoutParams.MATCH_PARENT;

                dialog.setContentView(main);
                dialog.show();
                dialog.getWindow().setAttributes(lp);
                // the goal of openhidden is to load the url and not display it
                // Show() needs to be called to cause the URL to be loaded
                if(openWindowHidden) {
                    dialog.hide();
                }
            }
        };
        this.cordova.getActivity().runOnUiThread(runnable);
        return "";
    }

    /**
     * Create a new plugin success result and send it back to JavaScript
     *
     * @param obj a JSONObject contain event payload information
     */
    private void sendUpdate(JSONObject obj, boolean keepCallback) {
        sendUpdate(obj, keepCallback, PluginResult.Status.OK);
    }

    /**
     * Create a new plugin result and send it back to JavaScript
     *
     * @param obj a JSONObject contain event payload information
     * @param status the status code to return to the JavaScript environment
     */
    private void sendUpdate(JSONObject obj, boolean keepCallback, PluginResult.Status status) {
        LOG.d(LOG_TAG, "BEFORE");
        if (callbackContext != null) {
            LOG.d(LOG_TAG, "AFTER");
            PluginResult result = new PluginResult(status, obj);
            result.setKeepCallback(keepCallback);
            callbackContext.sendPluginResult(result);
            if (!keepCallback) {
                callbackContext = null;
            }
        }
    }

    /**
     * The webview client receives notifications about appView
     */
    public class InAppBrowserClient extends WebViewClient {
        EditText edittext;
        CordovaWebView webView;

        /**
         * Constructor.
         *
         * @param webView
         * @param mEditText
         */
        public InAppBrowserClient(CordovaWebView webView, EditText mEditText) {
            this.webView = webView;
            this.edittext = mEditText;
        }

        /**
         * Override the URL that should be loaded
         *
         * This handles a small subset of all the URIs that would be encountered.
         *
         * @param webView
         * @param url
         */
        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, String url) {
            if (url.startsWith(WebView.SCHEME_TEL)) {
                try {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse(url));
                    cordova.getActivity().startActivity(intent);
                    return true;
                } catch (android.content.ActivityNotFoundException e) {
                    LOG.e(LOG_TAG, "Error dialing " + url + ": " + e.toString());
                }
            } else if (url.startsWith("geo:") || url.startsWith(WebView.SCHEME_MAILTO) || url.startsWith("market:") || url.startsWith("intent:")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    cordova.getActivity().startActivity(intent);
                    return true;
                } catch (android.content.ActivityNotFoundException e) {
                    LOG.e(LOG_TAG, "Error with " + url + ": " + e.toString());
                }
            }
            // If sms:5551212?body=This is the message
            else if (url.startsWith("sms:")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);

                    // Get address
                    String address = null;
                    int parmIndex = url.indexOf('?');
                    if (parmIndex == -1) {
                        address = url.substring(4);
                    } else {
                        address = url.substring(4, parmIndex);

                        // If body, then set sms body
                        Uri uri = Uri.parse(url);
                        String query = uri.getQuery();
                        if (query != null) {
                            if (query.startsWith("body=")) {
                                intent.putExtra("sms_body", query.substring(5));
                            }
                        }
                    }
                    intent.setData(Uri.parse("sms:" + address));
                    intent.putExtra("address", address);
                    intent.setType("vnd.android-dir/mms-sms");
                    cordova.getActivity().startActivity(intent);
                    return true;
                } catch (android.content.ActivityNotFoundException e) {
                    LOG.e(LOG_TAG, "Error sending sms " + url + ":" + e.toString());
                }
            }
            return false;
        }

        /*
         * Animates the textWebView into the scene
         */
        public void animateView(){
            if( !textModeActivated ) {
                textWebView.setVisibility(View.GONE);
                ValueAnimator va = ValueAnimator.ofInt(fastViewContainter.getHeight(), 0);
                va.setDuration(250);
                va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        Integer value = (Integer) animation.getAnimatedValue();
                        fastViewContainter.getLayoutParams().height = value.intValue();
                        fastViewContainter.requestLayout();
                    }
                });
                va.start();
                showAllButtons();
            }
        }

        /*
         * onPageStarted fires the LOAD_START_EVENT
         *
         * @param view
         * @param url
         * @param favicon
         */
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            String newloc = "";
            if (url.startsWith("http:") || url.startsWith("https:") || url.startsWith("file:")) {
                newloc = url;
            }
            else
            {
                // Assume that everything is HTTP at this point, because if we don't specify,
                // it really should be.  Complain loudly about this!!!
                LOG.e(LOG_TAG, "Possible Uncaught/Unknown URI");
                newloc = "http://" + url;
            }

            // Update the UI if we haven't already
            if (!newloc.equals(edittext.getText().toString())) {
                edittext.setText(newloc);
            }

            this.updateBackAndForwardBtns();

            try {
                JSONObject obj = new JSONObject();
                obj.put("type", LOAD_START_EVENT);
                obj.put("url", newloc);
                sendUpdate(obj, true);
            } catch (JSONException ex) {
                LOG.e(LOG_TAG, "URI passed in has caused a JSON error.");
            }
        }

        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            
            didLoad = true;
            this.animateView();

            // CB-10395 InAppBrowser's WebView not storing cookies reliable to local device storage
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().flush();
            } else {
                CookieSyncManager.getInstance().sync();
            }

            this.updateBackAndForwardBtns();

            try {
                JSONObject obj = new JSONObject();
                obj.put("type", LOAD_STOP_EVENT);
                obj.put("url", url);

                sendUpdate(obj, true);
            } catch (JSONException ex) {
                LOG.d(LOG_TAG, "Should never happen");
            }
        }

        public void updateBackAndForwardBtns() {
            Drawable backIcon;
            Drawable forwardIcon;
            int backResId;
            int forwardResId;
            Resources activityRes = cordova.getActivity().getResources();

            if( inAppWebView.canGoBack() ){
                backResId = activityRes.getIdentifier("icon_arrow_left_active", "drawable", cordova.getActivity().getPackageName());
            }else{
                backResId = activityRes.getIdentifier("icon_arrow_left", "drawable", cordova.getActivity().getPackageName());
            }
            backIcon = activityRes.getDrawable(backResId);

            if( inAppWebView.canGoForward() ){
                forwardResId = activityRes.getIdentifier("icon_arrow_right_active", "drawable", cordova.getActivity().getPackageName());
            }else{
                forwardResId = activityRes.getIdentifier("icon_arrow_right", "drawable", cordova.getActivity().getPackageName());
            }

            forwardIcon = activityRes.getDrawable(forwardResId);

            if (Build.VERSION.SDK_INT >= 16){
                back.setBackground(backIcon);
                forward.setBackground(forwardIcon);
            }
            else{
                back.setBackgroundDrawable(backIcon);
                forward.setBackgroundDrawable(forwardIcon);
            }
        }

        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);

            try {
                JSONObject obj = new JSONObject();
                obj.put("type", LOAD_ERROR_EVENT);
                obj.put("url", failingUrl);
                obj.put("code", errorCode);
                obj.put("message", description);

                sendUpdate(obj, true, PluginResult.Status.ERROR);
            } catch (JSONException ex) {
                LOG.d(LOG_TAG, "Should never happen");
            }
        }

        /**
         * On received http auth request.
         */
        @Override
        public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {

            // Check if there is some plugin which can resolve this auth challenge
            PluginManager pluginManager = null;
            try {
                Method gpm = webView.getClass().getMethod("getPluginManager");
                pluginManager = (PluginManager)gpm.invoke(webView);
            } catch (NoSuchMethodException e) {
                LOG.d(LOG_TAG, e.getLocalizedMessage());
            } catch (IllegalAccessException e) {
                LOG.d(LOG_TAG, e.getLocalizedMessage());
            } catch (InvocationTargetException e) {
                LOG.d(LOG_TAG, e.getLocalizedMessage());
            }

            if (pluginManager == null) {
                try {
                    Field pmf = webView.getClass().getField("pluginManager");
                    pluginManager = (PluginManager)pmf.get(webView);
                } catch (NoSuchFieldException e) {
                    LOG.d(LOG_TAG, e.getLocalizedMessage());
                } catch (IllegalAccessException e) {
                    LOG.d(LOG_TAG, e.getLocalizedMessage());
                }
            }

            if (pluginManager != null && pluginManager.onReceivedHttpAuthRequest(webView, new CordovaHttpAuthHandler(handler), host, realm)) {
                return;
            }

            // By default handle 401 like we'd normally do!
            super.onReceivedHttpAuthRequest(view, handler, host, realm);
        }
    }
}
