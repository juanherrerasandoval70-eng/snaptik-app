package com.snaptik.app;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.startapp.sdk.ads.banner.Banner;
import com.startapp.sdk.ads.banner.BannerListener;
import com.startapp.sdk.ads.nativead.NativeAdDetails;
import com.startapp.sdk.ads.nativead.NativeAdPreferences;
import com.startapp.sdk.ads.nativead.StartAppNativeAd;
import com.startapp.sdk.adsbase.Ad;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;
import com.startapp.sdk.adsbase.adlisteners.VideoListener;

import org.json.JSONObject;
import java.util.ArrayList;

public class StartIoRewardManager {
    private static final String TAG = "StartIoAllAds";

    // App ID oficial de Start.io (portal.start.io)
    public static String STARTIO_APP_ID = "208514697";

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static boolean initialized = false;

    private static StartAppAd rewardedAd = null;
    private static boolean isRewardedReady = false;

    private static StartAppAd interstitialAd = null;
    private static boolean isInterstitialReady = false;

    private static NativeAdDetails activeNativeAd = null;

    // Live diagnostics for UI
    public static String bannerStatus = "CARGANDO...";
    public static String rewardedStatus = "CARGANDO...";
    public static String interstitialStatus = "CARGANDO...";
    public static String nativeStatus = "CARGANDO...";

    public static boolean isConfigured() {
        return STARTIO_APP_ID != null && STARTIO_APP_ID.trim().length() >= 6;
    }

    public static String getDiagnosticsJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("appId", STARTIO_APP_ID);
            obj.put("initialized", initialized);
            obj.put("banner", bannerStatus);
            obj.put("rewarded", rewardedStatus);
            obj.put("interstitial", interstitialStatus);
            obj.put("nativeAd", nativeStatus);
        } catch (Exception ignored) {}
        return obj.toString();
    }

    public static void init(final Activity activity, final ViewGroup bannerContainer, final WebView webView) {
        if (activity == null || !isConfigured()) return;
        try {
            if (!initialized) {
                StartAppSDK.setUserConsent(activity, "pas", System.currentTimeMillis(), true);
                StartAppSDK.init(activity, STARTIO_APP_ID.trim(), true);
                StartAppSDK.enableReturnAds(true);
                initialized = true;
                Log.d(TAG, "Start.io SDK " + StartAppSDK.getVersion() + " inicializado con App ID: " + STARTIO_APP_ID);
            }
            // Montar banner y precargar anuncios con espera breve para que metadata sincronice
            attachBanner(activity, bannerContainer);
            MAIN.postDelayed(new DelayedLoadRunnable(activity, webView), 2200);
        } catch (Exception e) {
            bannerStatus = "ERR: " + e.getMessage();
            Log.e(TAG, "Error inicializando Start.io: " + e.getMessage());
        }
    }

    private static class DelayedLoadRunnable implements Runnable {
        private final Activity activity;
        private final WebView webView;
        DelayedLoadRunnable(Activity activity, WebView webView) {
            this.activity = activity;
            this.webView = webView;
        }
        @Override
        public void run() {
            preloadRewardedVideo(activity, 0);
            preloadInterstitial(activity, 0);
            loadNativeAd(activity, webView);
        }
    }

    public static void attachBanner(Activity activity, ViewGroup bannerContainer) {
        if (activity == null || bannerContainer == null || !initialized) return;
        try {
            bannerContainer.removeAllViews();
            bannerStatus = "SOLICITANDO...";
            Banner banner = new Banner(activity, new LiveBannerListener());
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            );
            bannerContainer.addView(banner, params);
            banner.loadAd(320, 50);
        } catch (Exception e) {
            bannerStatus = "ERR: " + e.getMessage();
            Log.e(TAG, "Error montando Banner: " + e.getMessage());
        }
    }

    public static void preloadRewardedVideo(Activity activity, int retryCount) {
        if (activity == null || !initialized) return;
        try {
            isRewardedReady = false;
            rewardedStatus = "CARGANDO...";
            rewardedAd = new StartAppAd(activity);
            rewardedAd.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, new RewardLoadListener(activity, retryCount));
        } catch (Exception e) {
            isRewardedReady = false;
            rewardedStatus = "ERR: " + e.getMessage();
        }
    }

    public static void preloadInterstitial(Activity activity, int retryCount) {
        if (activity == null || !initialized) return;
        try {
            isInterstitialReady = false;
            interstitialStatus = "CARGANDO...";
            interstitialAd = new StartAppAd(activity);
            interstitialAd.loadAd(StartAppAd.AdMode.AUTOMATIC, new InterstitialLoadListener(activity, retryCount));
        } catch (Exception e) {
            isInterstitialReady = false;
            interstitialStatus = "ERR: " + e.getMessage();
        }
    }

    public static void loadNativeAd(Activity activity, WebView webView) {
        if (activity == null || !initialized) return;
        try {
            nativeStatus = "CARGANDO...";
            StartAppNativeAd nativeAd = new StartAppNativeAd(activity);
            NativeAdPreferences prefs = new NativeAdPreferences()
                .setAdsNumber(1)
                .setAutoBitmapDownload(false)
                .setPrimaryImageSize(2);
            nativeAd.loadAd(prefs, new NativeLoadListener(nativeAd, webView));
        } catch (Exception e) {
            nativeStatus = "ERR: " + e.getMessage();
        }
    }

    public static void clickNativeAd(Activity activity) {
        if (activity == null || activeNativeAd == null) return;
        try {
            View root = activity.getWindow().getDecorView();
            activeNativeAd.registerViewForInteraction(root);
            root.performClick();
        } catch (Exception ignored) {}
    }

    public static boolean showInterstitial(Activity activity) {
        if (activity == null || !initialized) return false;
        try {
            if (interstitialAd != null && isInterstitialReady) {
                StartAppAd adToShow = interstitialAd;
                isInterstitialReady = false;
                interstitialAd = null;
                boolean shown = adToShow.showAd(new InterstitialDisplayListener(activity));
                if (shown) return true;
            }
            StartAppAd.showAd(activity);
            preloadInterstitial(activity, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean showRewardedVideo(Activity activity, WebView webView) {
        if (activity == null || !isConfigured() || !initialized) {
            return false;
        }
        try {
            if (rewardedAd != null && isRewardedReady) {
                final StartAppAd adToShow = rewardedAd;
                isRewardedReady = false;
                rewardedAd = null;

                RewardCompletionState state = new RewardCompletionState();
                adToShow.setVideoListener(new RewardVideoListener(activity, webView, state));
                boolean shown = adToShow.showAd(new RewardDisplayListener(activity, webView, state));
                if (shown) return true;
            }
            if (interstitialAd != null && isInterstitialReady) {
                final StartAppAd adToShow = interstitialAd;
                isInterstitialReady = false;
                interstitialAd = null;
                RewardCompletionState state = new RewardCompletionState();
                state.rewarded = true;
                boolean shown = adToShow.showAd(new RewardDisplayListener(activity, webView, state));
                preloadRewardedVideo(activity, 0);
                preloadInterstitial(activity, 0);
                if (shown) return true;
            }
            preloadRewardedVideo(activity, 0);
            preloadInterstitial(activity, 0);
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

    private static class LiveBannerListener implements BannerListener {
        @Override public void onReceiveAd(View banner) { bannerStatus = "ACTIVO"; }
        @Override public void onFailedToReceiveAd(View banner) { bannerStatus = "SIN FILL"; }
        @Override public void onImpression(View banner) { bannerStatus = "IMPRESO"; }
        @Override public void onClick(View banner) {}
    }

    private static class RetryRewardRunnable implements Runnable {
        private final Activity activity;
        private final int nextRetry;
        RetryRewardRunnable(Activity activity, int nextRetry) { this.activity = activity; this.nextRetry = nextRetry; }
        @Override public void run() { preloadRewardedVideo(activity, nextRetry); }
    }

    private static class RetryInterstitialRunnable implements Runnable {
        private final Activity activity;
        private final int nextRetry;
        RetryInterstitialRunnable(Activity activity, int nextRetry) { this.activity = activity; this.nextRetry = nextRetry; }
        @Override public void run() { preloadInterstitial(activity, nextRetry); }
    }

    private static class RewardLoadListener implements AdEventListener {
        private final Activity activity;
        private final int retryCount;
        RewardLoadListener(Activity activity, int retryCount) { this.activity = activity; this.retryCount = retryCount; }

        @Override
        public void onReceiveAd(Ad ad) {
            isRewardedReady = true;
            rewardedStatus = "LISTO";
        }

        @Override
        public void onFailedToReceiveAd(Ad ad) {
            isRewardedReady = false;
            String err = (ad != null && ad.getErrorMessage() != null) ? ad.getErrorMessage() : "NO_FILL";
            rewardedStatus = err;
            if (retryCount < 2) {
                MAIN.postDelayed(new RetryRewardRunnable(activity, retryCount + 1), 6000);
            }
        }
    }

    private static class InterstitialLoadListener implements AdEventListener {
        private final Activity activity;
        private final int retryCount;
        InterstitialLoadListener(Activity activity, int retryCount) { this.activity = activity; this.retryCount = retryCount; }

        @Override
        public void onReceiveAd(Ad ad) {
            isInterstitialReady = true;
            interstitialStatus = "LISTO";
        }

        @Override
        public void onFailedToReceiveAd(Ad ad) {
            isInterstitialReady = false;
            String err = (ad != null && ad.getErrorMessage() != null) ? ad.getErrorMessage() : "NO_FILL";
            interstitialStatus = err;
            if (retryCount < 2) {
                MAIN.postDelayed(new RetryInterstitialRunnable(activity, retryCount + 1), 6000);
            }
        }
    }

    private static class InterstitialDisplayListener implements AdDisplayListener {
        private final Activity activity;
        InterstitialDisplayListener(Activity activity) { this.activity = activity; }
        @Override public void adHidden(Ad ad) { preloadInterstitial(activity, 0); }
        @Override public void adDisplayed(Ad ad) {}
        @Override public void adClicked(Ad ad) {}
        @Override public void adNotDisplayed(Ad ad) { preloadInterstitial(activity, 0); }
    }

    private static class NativeLoadListener implements AdEventListener {
        private final StartAppNativeAd nativeAd;
        private final WebView webView;

        NativeLoadListener(StartAppNativeAd nativeAd, WebView webView) {
            this.nativeAd = nativeAd;
            this.webView = webView;
        }

        @Override
        public void onReceiveAd(Ad ad) {
            try {
                ArrayList<NativeAdDetails> ads = nativeAd.getNativeAds();
                if (ads != null && !ads.isEmpty()) {
                    NativeAdDetails d = ads.get(0);
                    activeNativeAd = d;
                    nativeStatus = "ACTIVO";
                    JSONObject obj = new JSONObject();
                    obj.put("title", d.getTitle() != null ? d.getTitle() : "Recomendado");
                    obj.put("description", d.getDescription() != null ? d.getDescription() : "");
                    obj.put("imageUrl", d.getImageUrl() != null ? d.getImageUrl() : "");
                    obj.put("iconUrl", d.getSecondaryImageUrl() != null ? d.getSecondaryImageUrl() : "");
                    obj.put("cta", d.getCallToAction() != null ? d.getCallToAction() : "Abrir");
                    obj.put("rating", d.getRating());
                    if (webView != null) {
                        webView.post(new EvaluateJsRunnable(webView,
                            "if(window.onStartIoNativeAdLoaded){window.onStartIoNativeAdLoaded(" + obj.toString() + ");}"));
                    }
                } else {
                    nativeStatus = "SIN FILL";
                }
            } catch (Exception e) {
                nativeStatus = "ERR: " + e.getMessage();
            }
        }

        @Override
        public void onFailedToReceiveAd(Ad ad) {
            String err = (ad != null && ad.getErrorMessage() != null) ? ad.getErrorMessage() : "NO_FILL";
            nativeStatus = err;
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
            preloadRewardedVideo(activity, 0);
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
            if (!state.unlocked) {
                state.unlocked = true;
                dispatchUnlock(webView);
            }
            preloadRewardedVideo(activity, 0);
            preloadInterstitial(activity, 0);
        }

        @Override public void adDisplayed(Ad ad) {}
        @Override public void adClicked(Ad ad) {}

        @Override
        public void adNotDisplayed(Ad ad) {
            if (!state.unlocked) {
                state.unlocked = true;
                dispatchUnlock(webView);
            }
            preloadRewardedVideo(activity, 0);
            preloadInterstitial(activity, 0);
        }
    }
}
