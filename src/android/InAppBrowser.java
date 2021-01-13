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
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Parcelable;
import android.provider.Browser;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Color;
import android.net.http.SslError;
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
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    private static final String MESSAGE_EVENT = "message";
    private static final String CLEAR_ALL_CACHE = "clearcache";
    private static final String CLEAR_SESSION_CACHE = "clearsessioncache";
    private static final String HIDE_FAV = "hidefav";
    private static final String TEXT_MODE = "textmode";
    private static final String HARDWARE_BACK_BUTTON = "hardwareback";
    private static final String MEDIA_PLAYBACK_REQUIRES_USER_ACTION = "mediaPlaybackRequiresUserAction";
    private static final String SHOULD_PAUSE = "shouldPauseOnSuspend";
    private static final Boolean DEFAULT_HARDWARE_BACK = true;
    private static final String USER_WIDE_VIEW_PORT = "useWideViewPort";
    private static final String TOOLBAR_COLOR = "toolbarcolor";
    private static final String CLOSE_BUTTON_CAPTION = "closebuttoncaption";
    private static final String CLOSE_BUTTON_COLOR = "closebuttoncolor";
    private static final String LEFT_TO_RIGHT = "lefttoright";
    private static final String HIDE_NAVIGATION = "hidenavigationbuttons";
    private static final String NAVIGATION_COLOR = "navigationbuttoncolor";
    private static final String HIDE_URL = "hideurlbar";
    private static final String FOOTER = "footer";
    private static final String FOOTER_COLOR = "footercolor";
    private static final String BEFORELOAD = "beforeload";
    private static final String FULLSCREEN = "fullscreen";

    private static final List customizableOptions = Arrays.asList(CLOSE_BUTTON_CAPTION, TOOLBAR_COLOR, NAVIGATION_COLOR, CLOSE_BUTTON_COLOR, FOOTER_COLOR);

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
    private boolean useWideViewPort = true;
    private ValueCallback<Uri> mUploadCallback;
    private ValueCallback<Uri[]> mUploadCallbackLollipop;
    private final static int FILECHOOSER_REQUESTCODE = 1;
    private final static int FILECHOOSER_REQUESTCODE_LOLLIPOP = 2;
    private String closeButtonCaption = "";
    private String closeButtonColor = "";
    private boolean leftToRight = false;
    private int toolbarColor = android.graphics.Color.LTGRAY;
    private boolean hideNavigationButtons = false;
    private String navigationButtonColor = "";
    private boolean hideUrlBar = false;
    private boolean showFooter = false;
    private String footerColor = "";
    private String beforeload = "";
    private boolean fullscreen = true;
    private String[] allowedSchemes;
    private InAppBrowserClient currentClient;

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
            final HashMap<String, String> features = parseFeature(args.optString(2));

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
        else if (action.equals("loadAfterBeforeload")) {
            if (beforeload == null) {
                LOG.e(LOG_TAG, "unexpected loadAfterBeforeload called without feature beforeload=yes");
            }
            final String url = args.getString(0);
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @SuppressLint("NewApi")
                @Override
                public void run() {
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
                        currentClient.waitForBeforeload = false;
                        inAppWebView.setWebViewClient(currentClient);
                    } else {
                        ((InAppBrowserClient)inAppWebView.getWebViewClient()).waitForBeforeload = false;
                    }
                    inAppWebView.loadUrl(url);
                }
            });
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
                    if (dialog != null && !cordova.getActivity().isFinishing()) {
                        dialog.show();
                    }
                }
            });
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
            pluginResult.setKeepCallback(true);
            this.callbackContext.sendPluginResult(pluginResult);
        }
        else if (action.equals("hide")) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (dialog != null && !cordova.getActivity().isFinishing()) {
                        dialog.hide();
                    }
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
        if (inAppWebView!=null) {
            String scriptToInject;
            if (jsWrapper != null) {
                org.json.JSONArray jsonEsc = new org.json.JSONArray();
                jsonEsc.put(source);
                String jsonRepr = jsonEsc.toString();
                String jsonSourceString = jsonRepr.substring(1, jsonRepr.length()-1);
                scriptToInject = String.format(jsWrapper, jsonSourceString);
            } else {
                scriptToInject = source;
            }
            final String finalScriptToInject = scriptToInject;
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @SuppressLint("NewApi")
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                        // This action will have the side-effect of blurring the currently focused element
                        inAppWebView.loadUrl("javascript:" + finalScriptToInject);
                    } else {
                        inAppWebView.evaluateJavascript(finalScriptToInject, null);
                    }
                }
            });
        } else {
            LOG.d(LOG_TAG, "Can't inject code into the system browser");
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
    private HashMap<String, String> parseFeature(String optString) {
        if (optString.equals(NULL)) {
            return null;
        } else {
            HashMap<String, String> map = new HashMap<String, String>();
            StringTokenizer features = new StringTokenizer(optString, ",");
            StringTokenizer option;
            while(features.hasMoreElements()) {
                option = new StringTokenizer(features.nextToken(), "=");
                if (option.hasMoreElements()) {
                    String key = option.nextToken();
                    String value = option.nextToken();
                    if (!customizableOptions.contains(key)) {
                        value = value.equals("yes") || value.equals("no") ? value : "yes";
                    }
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
            // CB-10795: Avoid circular loops by preventing it from opening in the current app
            this.openExternalExcludeCurrentApp(intent);
            return "";
            // not catching FileUriExposedException explicitly because buildtools<24 doesn't know about it
        } catch (java.lang.RuntimeException e) {
            LOG.d(LOG_TAG, "InAppBrowser: Error loading url "+url+":"+ e.toString());
            return e.toString();
        }
    }

    /**
     * Opens the intent, providing a chooser that excludes the current app to avoid
     * circular loops.
     */
    private void openExternalExcludeCurrentApp(Intent intent) {
        String currentPackage = cordova.getActivity().getPackageName();
        boolean hasCurrentPackage = false;

        PackageManager pm = cordova.getActivity().getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
        ArrayList<Intent> targetIntents = new ArrayList<Intent>();

        for (ResolveInfo ri : activities) {
            if (!currentPackage.equals(ri.activityInfo.packageName)) {
                Intent targetIntent = (Intent)intent.clone();
                targetIntent.setPackage(ri.activityInfo.packageName);
                targetIntents.add(targetIntent);
            }
            else {
                hasCurrentPackage = true;
            }
        }

        // If the current app package isn't a target for this URL, then use
        // the normal launch behavior
        if (hasCurrentPackage == false || targetIntents.size() == 0) {
            this.cordova.getActivity().startActivity(intent);
        }
        // If there's only one possible intent, launch it directly
        else if (targetIntents.size() == 1) {
            this.cordova.getActivity().startActivity(targetIntents.get(0));
        }
        // Otherwise, show a custom chooser without the current app listed
        else if (targetIntents.size() > 0) {
            Intent chooser = Intent.createChooser(targetIntents.remove(targetIntents.size()-1), null);
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetIntents.toArray(new Parcelable[] {}));
            this.cordova.getActivity().startActivity(chooser);
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
                        if (dialog != null && !cordova.getActivity().isFinishing()) {
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
        fav.setVisibility(View.INVISIBLE);
        share.setVisibility(View.INVISIBLE);
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
            share.setVisibility(View.INVISIBLE);
            fav.setVisibility(View.INVISIBLE);
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

    private InAppBrowser getInAppBrowser() {
        return this;
    }

    /**
     * Display a new browser with the specified URL.
     *
     * @param url the url to load.
     * @param features jsonObject
     */
    public String showWebPage(final String url, HashMap<String, String> features) {
        // Determine if we should hide the location bar.
        showLocationBar = true;
        showZoomControls = true;
        openWindowHidden = false;
        mediaPlaybackRequiresUserGesture = false;

        if (features != null) {
            String show = features.get(LOCATION);
            if (show != null) {
                showLocationBar = show.equals("yes") ? true : false;
            }
            if(showLocationBar) {
                String hideNavigation = features.get(HIDE_NAVIGATION);
                String hideUrl = features.get(HIDE_URL);
                if(hideNavigation != null) hideNavigationButtons = hideNavigation.equals("yes") ? true : false;
                if(hideUrl != null) hideUrlBar = hideUrl.equals("yes") ? true : false;
            }
            String zoom = features.get(ZOOM);
            if (zoom != null) {
                showZoomControls = zoom.equals("yes") ? true : false;
            }
            String hidden = features.get(HIDDEN);
            if (hidden != null) {
                openWindowHidden = hidden.equals("yes") ? true : false;
            }
            String hardwareBack = features.get(HARDWARE_BACK_BUTTON);
            if (hardwareBack != null) {
                hadwareBackButton = hardwareBack.equals("yes") ? true : false;
            } else {
                hadwareBackButton = DEFAULT_HARDWARE_BACK;
            }
            hideFav = features.get(HIDE_FAV) != null ? true : false;
            String mediaPlayback = features.get(MEDIA_PLAYBACK_REQUIRES_USER_ACTION);
            if (mediaPlayback != null) {
                mediaPlaybackRequiresUserGesture = mediaPlayback.equals("yes") ? true : false;
            }
            // String cache = features.get(CLEAR_ALL_CACHE);
            // if (cache != null) {
            //     clearAllCache = cache.equals("yes") ? true : false;
            // } else {
            //     cache = features.get(CLEAR_SESSION_CACHE);
            //     if (cache != null) {
            //         clearSessionCache = cache.equals("yes") ? true : false;
            //     }
            // }
            String shouldPause = features.get(SHOULD_PAUSE);
            if (shouldPause != null) {
                shouldPauseInAppBrowser = shouldPause.equals("yes") ? true : false;
            }
            String wideViewPort = features.get(USER_WIDE_VIEW_PORT);
            if (wideViewPort != null ) {
                useWideViewPort = wideViewPort.equals("yes") ? true : false;
            }
            String closeButtonCaptionSet = features.get(CLOSE_BUTTON_CAPTION);
            if (closeButtonCaptionSet != null) {
                closeButtonCaption = closeButtonCaptionSet;
            }
            String closeButtonColorSet = features.get(CLOSE_BUTTON_COLOR);
            if (closeButtonColorSet != null) {
                closeButtonColor = closeButtonColorSet;
            }
            String leftToRightSet = features.get(LEFT_TO_RIGHT);
            leftToRight = leftToRightSet != null && leftToRightSet.equals("yes");

            String toolbarColorSet = features.get(TOOLBAR_COLOR);
            if (toolbarColorSet != null) {
                toolbarColor = android.graphics.Color.parseColor(toolbarColorSet);
            }
            String navigationButtonColorSet = features.get(NAVIGATION_COLOR);
            if (navigationButtonColorSet != null) {
                navigationButtonColor = navigationButtonColorSet;
            }
            String showFooterSet = features.get(FOOTER);
            if (showFooterSet != null) {
                showFooter = showFooterSet.equals("yes") ? true : false;
            }
            String footerColorSet = features.get(FOOTER_COLOR);
            if (footerColorSet != null) {
                footerColor = footerColorSet;
            }
            if (features.get(BEFORELOAD) != null) {
                beforeload = features.get(BEFORELOAD);
            }
            String fullscreenSet = features.get(FULLSCREEN);
            if (fullscreenSet != null) {
                fullscreen = fullscreenSet.equals("yes") ? true : false;
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

            private View createCloseButton(int id) {
                View _close;
                Resources activityRes = cordova.getActivity().getResources();

                if (closeButtonCaption != "") {
                    // Use TextView for text
                    TextView close = new TextView(cordova.getActivity());
                    close.setText(closeButtonCaption);
                    close.setTextSize(20);
                    if (closeButtonColor != "") close.setTextColor(android.graphics.Color.parseColor(closeButtonColor));
                    close.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    close.setPadding(this.dpToPixels(10), 0, this.dpToPixels(10), 0);
                    _close = close;
                }
                else {
                    ImageButton close = new ImageButton(cordova.getActivity());
                    int closeResId = activityRes.getIdentifier("ic_action_remove", "drawable", cordova.getActivity().getPackageName());
                    Drawable closeIcon = activityRes.getDrawable(closeResId);
                    if (closeButtonColor != "") close.setColorFilter(android.graphics.Color.parseColor(closeButtonColor));
                    close.setImageDrawable(closeIcon);
                    close.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    if (Build.VERSION.SDK_INT >= 16)
                        close.getAdjustViewBounds();

                    _close = close;
                }

                RelativeLayout.LayoutParams closeLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
                if (leftToRight) closeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                else closeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                _close.setLayoutParams(closeLayoutParams);

                if (Build.VERSION.SDK_INT >= 16)
                    _close.setBackground(null);
                else
                    _close.setBackgroundDrawable(null);

                _close.setContentDescription("Close Button");
                _close.setId(Integer.valueOf(id));
                _close.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        closeDialog();
                    }
                });

                return _close;
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
                if (fullscreen) {
                    dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
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
                RelativeLayout.LayoutParams actionButtonLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                if (leftToRight) actionButtonLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                else actionButtonLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                actionButtonContainer.setLayoutParams(actionButtonLayoutParams);
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
                if (navigationButtonColor != "") back.setColorFilter(android.graphics.Color.parseColor(navigationButtonColor));
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
                if (navigationButtonColor != "") forward.setColorFilter(android.graphics.Color.parseColor(navigationButtonColor));
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
                closeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                close.setLayoutParams(closeLayoutParams);
                close.setContentDescription("Close Button");
                close.setId(Integer.valueOf(5));
                int closeResId = activityRes.getIdentifier("icon_close", "drawable", cordova.getActivity().getPackageName());
                Drawable closeIcon = activityRes.getDrawable(closeResId);
                if (Build.VERSION.SDK_INT >= 16)
                    close.setBackground(closeIcon);
                else
                    close.setBackgroundDrawable(closeIcon);
                close.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        closeDialog();
                    }
                });

                // Fav button
                fav = new ImageButton(cordova.getActivity());
                RelativeLayout.LayoutParams shareLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                shareLayoutParams.addRule(RelativeLayout.LEFT_OF, 5);
                shareLayoutParams.setMargins(0, 0, this.dpToPixels(10), 0);
                fav.setLayoutParams(shareLayoutParams);
                fav.setContentDescription("Fav Button");
                fav.setId(Integer.valueOf(10));
                fav.setVisibility(View.INVISIBLE);
                int shareResId = activityRes.getIdentifier("icon_fav", "drawable", cordova.getActivity().getPackageName());
                Drawable shareIcon = activityRes.getDrawable(shareResId);
                if (Build.VERSION.SDK_INT >= 16)
                    fav.setBackground(shareIcon);
                else
                    fav.setBackgroundDrawable(shareIcon);
                fav.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        favPage();
                    }
                });

                // Share button
                share = new ImageButton(cordova.getActivity());
                RelativeLayout.LayoutParams favLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                favLayoutParams.addRule(RelativeLayout.LEFT_OF, 10);
                favLayoutParams.setMargins(0, 0, this.dpToPixels(12), 0);
                share.setLayoutParams(favLayoutParams);
                share.setContentDescription("Share Button");
                share.setId(Integer.valueOf(9));
                share.setVisibility(View.INVISIBLE);
                int favResId = activityRes.getIdentifier("icon_share", "drawable", cordova.getActivity().getPackageName());
                Drawable favIcon = activityRes.getDrawable(favResId);
                if (Build.VERSION.SDK_INT >= 16)
                    share.setBackground(favIcon);
                else
                    share.setBackgroundDrawable(favIcon);
                share.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        sharePage();
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
                // fastViewContainter.addView(activateTextModeButton);

                TextView fastText = new TextView(cordova.getActivity());
                fastText.setText("");
                fastText.setTextColor(Color.parseColor("#004251"));
                fastText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                fastText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                fastText.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams ftlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                ftlp.setMargins(0, 240, 0, 20);
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
                // fastViewContainter.addView(gifView);



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
                    inAppWebView.addJavascriptInterface(new JsObject(), "cordova_iab");
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
                // Enable Thirdparty Cookies on >=Android 5.0 device
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    CookieManager.getInstance().setAcceptThirdPartyCookies(inAppWebView,true);
                }

                inAppWebView.loadUrl(url);
                textWebView.loadDataWithBaseURL(null, HTMLFastText, mime, encoding, null);
                inAppWebView.setId(Integer.valueOf(6));
                inAppWebView.getSettings().setLoadWithOverviewMode(true);
                inAppWebView.getSettings().setUseWideViewPort(useWideViewPort);
                // Multiple Windows set to true to mitigate Chromium security bug.
                //  See: https://bugs.chromium.org/p/chromium/issues/detail?id=1083819
                inAppWebView.getSettings().setSupportMultipleWindows(true);
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
                if (hideFav) {
                    share.setVisibility(View.INVISIBLE);
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

                if (dialog != null) {
                    dialog.setContentView(main);
                    dialog.show();
                    dialog.getWindow().setAttributes(lp);
                }
                // the goal of openhidden is to load the url and not display it
                // Show() needs to be called to cause the URL to be loaded
                if (openWindowHidden && dialog != null) {
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
     * Receive File Data from File Chooser
     *
     * @param requestCode the requested code from chromeclient
     * @param resultCode the result code returned from android system
     * @param intent the data from android file chooser
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // For Android >= 5.0
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            LOG.d(LOG_TAG, "onActivityResult (For Android >= 5.0)");
            // If RequestCode or Callback is Invalid
            if(requestCode != FILECHOOSER_REQUESTCODE_LOLLIPOP || mUploadCallbackLollipop == null) {
                super.onActivityResult(requestCode, resultCode, intent);
                return;
            }
            mUploadCallbackLollipop.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
            mUploadCallbackLollipop = null;
        }
        // For Android < 5.0
        else {
            LOG.d(LOG_TAG, "onActivityResult (For Android < 5.0)");
            // If RequestCode or Callback is Invalid
            if(requestCode != FILECHOOSER_REQUESTCODE || mUploadCallback == null) {
                super.onActivityResult(requestCode, resultCode, intent);
                return;
            }

            if (null == mUploadCallback) return;
            Uri result = intent == null || resultCode != cordova.getActivity().RESULT_OK ? null : intent.getData();

            mUploadCallback.onReceiveValue(result);
            mUploadCallback = null;
        }
    }

    /**
     * The webview client receives notifications about appView
     */
    public class InAppBrowserClient extends WebViewClient {
        EditText edittext;
        CordovaWebView webView;
        String beforeload;
        boolean waitForBeforeload;

        /**
         * Constructor.
         *
         * @param webView
         * @param mEditText
         */
        public InAppBrowserClient(CordovaWebView webView, EditText mEditText, String beforeload) {
            this.webView = webView;
            this.edittext = mEditText;
            this.beforeload = beforeload;
            this.waitForBeforeload = beforeload != null;
        }

        /**
         * Override the URL that should be loaded
         *
         * Legacy (deprecated in API 24)
         * For Android 6 and below.
         *
         * @param webView
         * @param url
         */
        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, String url) {
            return shouldOverrideUrlLoading(url, null);
        }

        /**
         * Override the URL that should be loaded
         *
         * New (added in API 24)
         * For Android 7 and above.
         *
         * @param webView
         * @param request
         */
        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest request) {
            return shouldOverrideUrlLoading(request.getUrl().toString(), request.getMethod());
        }

        /**
         * Override the URL that should be loaded
         *
         * This handles a small subset of all the URIs that would be encountered.
         *
         * @param url
         * @param method
         */
        public boolean shouldOverrideUrlLoading(String url, String method) {
            boolean override = false;
            boolean useBeforeload = false;
            String errorMessage = null;

            if (beforeload.equals("yes") && method == null) {
                useBeforeload = true;
            } else if(beforeload.equals("yes")
                    //TODO handle POST requests then this condition can be removed:
                    && !method.equals("POST"))
            {
                useBeforeload = true;
            } else if(beforeload.equals("get") && (method == null || method.equals("GET"))) {
                useBeforeload = true;
            } else if(beforeload.equals("post") && (method == null || method.equals("POST"))) {
                //TODO handle POST requests
                errorMessage = "beforeload doesn't yet support POST requests";
            }

            // On first URL change, initiate JS callback. Only after the beforeload event, continue.
            if (useBeforeload && this.waitForBeforeload) {
                if(sendBeforeLoad(url, method)) {
                    return true;
                }
            }

            if(errorMessage != null) {
                try {
                    LOG.e(LOG_TAG, errorMessage);
                    JSONObject obj = new JSONObject();
                    obj.put("type", LOAD_ERROR_EVENT);
                    obj.put("url", url);
                    obj.put("code", -1);
                    obj.put("message", errorMessage);
                    sendUpdate(obj, true, PluginResult.Status.ERROR);
                } catch(Exception e) {
                    LOG.e(LOG_TAG, "Error sending loaderror for " + url + ": " + e.toString());
                }
            }

            if (url.startsWith(WebView.SCHEME_TEL)) {
                try {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse(url));
                    cordova.getActivity().startActivity(intent);
                    override = true;
                } catch (android.content.ActivityNotFoundException e) {
                    LOG.e(LOG_TAG, "Error dialing " + url + ": " + e.toString());
                }
            } else if (url.startsWith("geo:") || url.startsWith(WebView.SCHEME_MAILTO) || url.startsWith("market:") || url.startsWith("intent:")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    cordova.getActivity().startActivity(intent);
                    override = true;
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
                    override = true;
                } catch (android.content.ActivityNotFoundException e) {
                    LOG.e(LOG_TAG, "Error sending sms " + url + ":" + e.toString());
                }
            }
            // Test for whitelisted custom scheme names like mycoolapp:// or twitteroauthresponse:// (Twitter Oauth Response)
            else if (!url.startsWith("http:") && !url.startsWith("https:") && url.matches("^[A-Za-z0-9+.-]*://.*?$")) {
                if (allowedSchemes == null) {
                    String allowed = preferences.getString("AllowedSchemes", null);
                    if(allowed != null) {
                        allowedSchemes = allowed.split(",");
                    }
                }
                if (allowedSchemes != null) {
                    for (String scheme : allowedSchemes) {
                        if (url.startsWith(scheme)) {
                            try {
                                JSONObject obj = new JSONObject();
                                obj.put("type", "customscheme");
                                obj.put("url", url);
                                sendUpdate(obj, true);
                                override = true;
                            } catch (JSONException ex) {
                                LOG.e(LOG_TAG, "Custom Scheme URI passed in has caused a JSON error.");
                            }
                        }
                    }
                }
            }

            if (useBeforeload) {
                this.waitForBeforeload = true;
            }
            return override;
        }

        private boolean sendBeforeLoad(String url, String method) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("type", BEFORELOAD);
                obj.put("url", url);
                if(method != null) {
                    obj.put("method", method);
                }
                sendUpdate(obj, true);
                return true;
            } catch (JSONException ex) {
                LOG.e(LOG_TAG, "URI passed in has caused a JSON error.");
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

        /**
         * Legacy (deprecated in API 21)
         * For Android 4.4 and below.
         * @param view
         * @param url
         * @return
         */
        @SuppressWarnings("deprecation")
        @Override
        public WebResourceResponse shouldInterceptRequest (final WebView view, String url) {
            return shouldInterceptRequest(url, super.shouldInterceptRequest(view, url), null);
        }

        /**
         * New (added in API 21)
         * For Android 5.0 and above.
         *
         * @param webView
         * @param request
         */
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            return shouldInterceptRequest(request.getUrl().toString(), super.shouldInterceptRequest(view, request), request.getMethod());
        }

        public WebResourceResponse shouldInterceptRequest(String url, WebResourceResponse response, String method) {
            return response;
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

            // Set the namespace for postMessage()
            if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                injectDeferredObject("window.webkit={messageHandlers:{cordova_iab:cordova_iab}}", null);
            }

            // CB-10395 InAppBrowser's WebView not storing cookies reliable to local device storage
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().flush();
            } else {
                CookieSyncManager.getInstance().sync();
            }

            this.updateBackAndForwardBtns();
            // https://issues.apache.org/jira/browse/CB-11248
            view.clearFocus();
            view.requestFocus();

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

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            super.onReceivedSslError(view, handler, error);
            try {
                JSONObject obj = new JSONObject();
                obj.put("type", LOAD_ERROR_EVENT);
                obj.put("url", error.getUrl());
                obj.put("code", 0);
                obj.put("sslerror", error.getPrimaryError());
                String message;
                switch (error.getPrimaryError()) {
                case SslError.SSL_DATE_INVALID:
                    message = "The date of the certificate is invalid";
                    break;
                case SslError.SSL_EXPIRED:
                    message = "The certificate has expired";
                    break;
                case SslError.SSL_IDMISMATCH:
                    message = "Hostname mismatch";
                    break;
                default:
                case SslError.SSL_INVALID:
                    message = "A generic error occurred";
                    break;
                case SslError.SSL_NOTYETVALID:
                    message = "The certificate is not yet valid";
                    break;
                case SslError.SSL_UNTRUSTED:
                    message = "The certificate authority is not trusted";
                    break;
                }
                obj.put("message", message);

                sendUpdate(obj, true, PluginResult.Status.ERROR);
            } catch (JSONException ex) {
                LOG.d(LOG_TAG, "Should never happen");
            }
            handler.cancel();
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
