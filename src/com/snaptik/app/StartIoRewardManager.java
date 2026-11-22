package com.snaptik.app;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.startapp.sdk.ads.banner.Banner;
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

    private static boolean initialized = false;
    private static StartAppAd rewardedAd = null;
    private static boolean isRewardedReady = false;

    private static StartAppAd interstitialAd = null;
    private static boolean isInterstitialReady = false;

    private static NativeAdDetails activeNativeAd = null;

    public static boolean isConfigured() {
        return STARTIO_APP_ID != null && STARTIO_APP_ID.trim().length() >= 6;
    }

    public static void init(Activity activity, ViewGroup bannerContainer, WebView webView) {
        if (activity == null || !isConfigured()) return;
        try {
            if (!initialized) {
                StartAppSDK.setUserConsent(activity, "pas", System.currentTimeMillis(), true);
                StartAppSDK.init(activity, STARTIO_APP_ID.trim(), true);
                initialized = true;
                Log.d(TAG, "Start.io SDK 4.11.5 inicializado con App ID: " + STARTIO_APP_ID);
            }
            attachBanner(activity, bannerContainer);
            preloadRewardedVideo(activity);
            preloadInterstitial(activity);
            loadNativeAd(activity, webView);
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando Start.io: " + e.getMessage());
        }
    }

    public static void attachBanner(Activity activity, ViewGroup bannerContainer) {
        if (activity == null || bannerContainer == null || !initialized) return;
        try {
            bannerContainer.removeAllViews();
            Banner banner = new Banner(activity);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            );
            bannerContainer.addView(banner, params);
            banner.loadAd();
            Log.d(TAG, "Start.io Banner montado y cargando.");
        } catch (Exception e) {
            Log.e(TAG, "Error montando Banner: " + e.getMessage());
        }
    }

    public static void preloadRewardedVideo(Activity activity) {
        if (activity == null || !initialized) return;
        try {
            isRewardedReady = false;
            rewardedAd = new StartAppAd(activity);
            rewardedAd.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, new RewardLoadListener());
        } catch (Exception e) {
            isRewardedReady = false;
        }
    }

    public static void preloadInterstitial(Activity activity) {
        if (activity == null || !initialized) return;
        try {
            isInterstitialReady = false;
            interstitialAd = new StartAppAd(activity);
            interstitialAd.loadAd(StartAppAd.AdMode.AUTOMATIC, new InterstitialLoadListener());
        } catch (Exception e) {
            isInterstitialReady = false;
        }
    }

    public static void loadNativeAd(Activity activity, WebView webView) {
        if (activity == null || !initialized) return;
        try {
            StartAppNativeAd nativeAd = new StartAppNativeAd(activity);
            NativeAdPreferences prefs = new NativeAdPreferences()
                .setAdsNumber(1)
                .setAutoBitmapDownload(false)
                .setPrimaryImageSize(2);
            nativeAd.loadAd(prefs, new NativeLoadListener(nativeAd, webView));
        } catch (Exception e) {
            Log.e(TAG, "Error cargando Native Ad: " + e.getMessage());
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
            // Intento directo del SDK si aún no terminó precarga
            StartAppAd.showAd(activity);
            preloadInterstitial(activity);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error mostrando Interstitial: " + e.getMessage());
            return false;
        }
    }

    public static boolean showRewardedVideo(Activity activity, WebView webView) {
        if (activity == null || !isConfigured() || !initialized) {
            return false;
        }
        try {
            // 1. Prioridad: Video Reward precargado
            if (rewardedAd != null && isRewardedReady) {
                final StartAppAd adToShow = rewardedAd;
                isRewardedReady = false;
                rewardedAd = null;

                RewardCompletionState state = new RewardCompletionState();
                adToShow.setVideoListener(new RewardVideoListener(activity, webView, state));
                boolean shown = adToShow.showAd(new RewardDisplayListener(activity, webView, state));
                if (shown) {
                    return true;
                }
            }
            // 2. Respaldo inmediato: Interstitial Start.io precargado y desbloquea al cerrar
            if (interstitialAd != null && isInterstitialReady) {
                final StartAppAd adToShow = interstitialAd;
                isInterstitialReady = false;
                interstitialAd = null;
                RewardCompletionState state = new RewardCompletionState();
                state.rewarded = true;
                boolean shown = adToShow.showAd(new RewardDisplayListener(activity, webView, state));
                preloadRewardedVideo(activity);
                preloadInterstitial(activity);
                if (shown) {
                    return true;
                }
            }
            preloadRewardedVideo(activity);
            preloadInterstitial(activity);
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
            isRewardedReady = true;
            Log.d(TAG, "Start.io Video Reward LISTO.");
        }

        @Override
        public void onFailedToReceiveAd(Ad ad) {
            isRewardedReady = false;
            Log.d(TAG, "Video Reward sin fill por ahora.");
        }
    }

    private static class InterstitialLoadListener implements AdEventListener {
        @Override
        public void onReceiveAd(Ad ad) {
            isInterstitialReady = true;
            Log.d(TAG, "Start.io Interstitial LISTO.");
        }

        @Override
        public void onFailedToReceiveAd(Ad ad) {
            isInterstitialReady = false;
        }
    }

    private static class InterstitialDisplayListener implements AdDisplayListener {
        private final Activity activity;
        InterstitialDisplayListener(Activity activity) { this.activity = activity; }
        @Override public void adHidden(Ad ad) { preloadInterstitial(activity); }
        @Override public void adDisplayed(Ad ad) {}
        @Override public void adClicked(Ad ad) {}
        @Override public void adNotDisplayed(Ad ad) { preloadInterstitial(activity); }
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
                    Log.d(TAG, "Start.io Native Ad cargado: " + d.getTitle());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error procesando Native Ad: " + e.getMessage());
            }
        }

        @Override
        public void onFailedToReceiveAd(Ad ad) {
            Log.d(TAG, "Native Ad sin fill por ahora.");
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
            preloadRewardedVideo(activity);
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
            preloadRewardedVideo(activity);
            preloadInterstitial(activity);
        }

        @Override public void adDisplayed(Ad ad) {}
        @Override public void adClicked(Ad ad) {}

        @Override
        public void adNotDisplayed(Ad ad) {
            if (!state.unlocked) {
                state.unlocked = true;
                dispatchUnlock(webView);
            }
            preloadRewardedVideo(activity);
            preloadInterstitial(activity);
        }
    }
}
