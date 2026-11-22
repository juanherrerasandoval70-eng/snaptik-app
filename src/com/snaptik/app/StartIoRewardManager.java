package com.snaptik.app;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.webkit.WebView;

import com.startapp.sdk.adsbase.Ad;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;
import com.startapp.sdk.adsbase.adlisteners.VideoListener;

public class StartIoRewardManager {
    private static final String TAG = "StartIoReward";

    // App ID oficial de Start.io (portal.start.io)
    public static String STARTIO_APP_ID = "208514697";

    private static boolean initialized = false;
    private static StartAppAd preloadedRewardedAd = null;
    private static boolean isAdReady = false;

    public static boolean isConfigured() {
        return STARTIO_APP_ID != null && STARTIO_APP_ID.trim().length() >= 6;
    }

    public static void init(Context context) {
        if (context == null || !isConfigured() || initialized) return;
        try {
            StartAppSDK.init(context.getApplicationContext(), STARTIO_APP_ID.trim(), false);
            StartAppAd.disableSplash();
            initialized = true;
            preloadRewardedVideo(context.getApplicationContext());
            Log.d(TAG, "Start.io SDK inicializado correctamente.");
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando Start.io: " + e.getMessage());
        }
    }

    public static void preloadRewardedVideo(Context context) {
        if (context == null || !initialized) return;
        try {
            isAdReady = false;
            preloadedRewardedAd = new StartAppAd(context);
            preloadedRewardedAd.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, new RewardLoadListener());
        } catch (Exception e) {
            isAdReady = false;
            Log.e(TAG, "Error precargando Video Reward: " + e.getMessage());
        }
    }

    public static boolean showRewardedVideo(Activity activity, WebView webView) {
        if (activity == null || !isConfigured() || !initialized) {
            return false;
        }
        try {
            if (preloadedRewardedAd != null && isAdReady) {
                final StartAppAd adToShow = preloadedRewardedAd;
                isAdReady = false;
                preloadedRewardedAd = null;

                RewardCompletionState state = new RewardCompletionState();
                adToShow.setVideoListener(new RewardVideoListener(activity, webView, state));
                boolean shown = adToShow.showAd(new RewardDisplayListener(activity, webView, state));
                if (shown) {
                    return true;
                }
            }
            // Si no estaba precargado aún, precargar para la siguiente descarga
            preloadRewardedVideo(activity.getApplicationContext());
        } catch (Exception e) {
            Log.e(TAG, "Error mostrando Video Reward: " + e.getMessage());
        }
        return false;
    }

    private static void dispatchUnlock(WebView webView) {
        if (webView != null) {
            webView.post(new EvaluateJsRunnable(webView, "if(window.onAdsterraRewardUnlocked){window.onAdsterraRewardUnlocked();}"));
        }
    }

    private static class RewardCompletionState {
        boolean rewarded = false;
        boolean unlocked = false;
    }

    private static class RewardLoadListener implements AdEventListener {
        @Override
        public void onReceiveAd(Ad ad) {
            isAdReady = true;
            Log.d(TAG, "Video Reward listo para mostrar.");
        }

        @Override
        public void onFailedToReceiveAd(Ad ad) {
            isAdReady = false;
            Log.d(TAG, "Sin inventario de Video Reward en este intento.");
        }
    }

    private static class RewardVideoListener implements VideoListener {
        private final Activity activity;
        private final WebView webView;
        private final RewardCompletionState state;

        RewardVideoListener(Activity activity, WebView webView, RewardCompletionState state) {
            this.activity = activity;
            this.webView = webView;
            this.state = state;
        }

        @Override
        public void onVideoCompleted() {
            state.rewarded = true;
            if (!state.unlocked) {
                state.unlocked = true;
                dispatchUnlock(webView);
            }
            preloadRewardedVideo(activity.getApplicationContext());
        }
    }

    private static class RewardDisplayListener implements AdDisplayListener {
        private final Activity activity;
        private final WebView webView;
        private final RewardCompletionState state;

        RewardDisplayListener(Activity activity, WebView webView, RewardCompletionState state) {
            this.activity = activity;
            this.webView = webView;
            this.state = state;
        }

        @Override
        public void adHidden(Ad ad) {
            if (state.rewarded && !state.unlocked) {
                state.unlocked = true;
                dispatchUnlock(webView);
            }
            preloadRewardedVideo(activity.getApplicationContext());
        }

        @Override
        public void adDisplayed(Ad ad) {}

        @Override
        public void adClicked(Ad ad) {}

        @Override
        public void adNotDisplayed(Ad ad) {
            if (!state.unlocked) {
                state.unlocked = true;
                dispatchUnlock(webView);
            }
            preloadRewardedVideo(activity.getApplicationContext());
        }
    }
}
