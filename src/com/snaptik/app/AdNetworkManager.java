package com.snaptik.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebView;
import org.json.JSONObject;

public class AdNetworkManager {

    private static final String TAG = "AdsterraSnapTik";
    private static final String PREFS_NAME = "snaptik_pro_prefs_v2";
    private static final String KEY_AGE_VERIFIED = "age_verified_v2";
    private static final String KEY_ADULT_18_PLUS = "is_adult_18_plus_v2";

    private static int downloadCount = 0;
    private static int adClickCount = 0;
    private static final int INTERSTITIAL_FREQUENCY = 2;

    private static final String ACTIVE_NETWORK = "Adsterra Smartlinks (High eCPM)";

    // Smartlinks LIMPIOS (Smartlink 2 y 3; Smartlink 1, 4 y 5 aislados en pool +18)
    public static final String[] SAFE_SMARTLINKS = {
        "https://asiafilm.org/4/ed7357907f05efc3d793c5aedbdf0c5b", // Smartlink_3 (ID: 31224747)
        "https://asiafilm.org/4/b70dfeb9161b32fbda5a5d24daca1943"  // Smartlink_2 (ID: 31224644)
    };

    // Todos los Smartlinks incluyendo Smartlink 1, 4 y 5 (+18 Adultos)
    public static final String[] ALL_SMARTLINKS = {
        "https://asiafilm.org/4/ed7357907f05efc3d793c5aedbdf0c5b", // Smartlink_3 (ID: 31224747)
        "https://asiafilm.org/4/b70dfeb9161b32fbda5a5d24daca1943", // Smartlink_2 (ID: 31224644)
        "https://asiafilm.org/4/7f20a7584445d119b20cbbaa4fc4012c", // Smartlink_5 (ID: 32351410 - +18)
        "https://auctionr.org/4/ceb82fb7e10796fc95547d9b50cfe003", // Smartlink_1 (ID: 31224274 - +18)
        "https://auctionr.org/4/048d8aa6b801685694fdae38b9f9cb5b"  // Smartlink_4 (ID: 32351281 - +18)
    };

    public static final String[] SMARTLINKS = ALL_SMARTLINKS;

    private static int currentLinkIndex = 0;
    private static boolean adult18PlusEnabled = false;

    public static synchronized void initFromPrefs(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        adult18PlusEnabled = prefs.getBoolean(KEY_ADULT_18_PLUS, false);
    }

    public static synchronized boolean isAgeVerified(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_AGE_VERIFIED, false);
    }

    public static synchronized boolean isAdult18PlusEnabled(Context context) {
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            adult18PlusEnabled = prefs.getBoolean(KEY_ADULT_18_PLUS, false);
        }
        return adult18PlusEnabled;
    }

    public static synchronized void setAgePreference(Context context, boolean isAdult18Plus) {
        adult18PlusEnabled = isAdult18Plus;
        currentLinkIndex = 0;
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                .putBoolean(KEY_AGE_VERIFIED, true)
                .putBoolean(KEY_ADULT_18_PLUS, isAdult18Plus)
                .apply();
        }
    }

    private static synchronized String[] getActivePool(Context context) {
        if (context != null) {
            initFromPrefs(context);
        }
        return adult18PlusEnabled ? ALL_SMARTLINKS : SAFE_SMARTLINKS;
    }

    /**
     * Obtiene el siguiente Smartlink activo según el filtro de edad.
     * Si +18 está desactivado, NUNCA usa los enlaces de auctionr.org (Smartlink_1 ni Smartlink_4).
     */
    public static synchronized String getNextSmartlink(Context context) {
        String[] pool = getActivePool(context);
        if (currentLinkIndex >= pool.length) {
            currentLinkIndex = 0;
        }
        String link = pool[currentLinkIndex];
        currentLinkIndex = (currentLinkIndex + 1) % pool.length;
        return link;
    }

    public static synchronized String getNextSmartlink() {
        return getNextSmartlink(null);
    }

    public static synchronized String getCurrentSmartlink() {
        String[] pool = adult18PlusEnabled ? ALL_SMARTLINKS : SAFE_SMARTLINKS;
        if (currentLinkIndex >= pool.length) {
            currentLinkIndex = 0;
        }
        return pool[currentLinkIndex];
    }

    public static synchronized boolean recordDownloadAndCheckAdTrigger() {
        if (!adult18PlusEnabled) {
            return false;
        }
        downloadCount++;
        return (downloadCount % INTERSTITIAL_FREQUENCY == 0);
    }

    public static synchronized boolean recordDownloadAndCheckAdTrigger(Context context) {
        downloadCount++;
        if (StartIoRewardManager.isConfigured()) {
            return true; // Mostrar interstitial Start.io tras cada descarga completada
        }
        if (!isAdult18PlusEnabled(context)) {
            return false;
        }
        return (downloadCount % INTERSTITIAL_FREQUENCY == 0);
    }

    public static synchronized void openDirectSponsorUrl(Context context, String customUrl) {
        if (context == null) return;
        if (!isAdult18PlusEnabled(context)) {
            Log.d(TAG, "Modo Seguro (+18=false): apertura externa de anuncio bloqueada.");
            return;
        }
        try {
            String target = (customUrl != null && !customUrl.trim().isEmpty()) ? customUrl.trim() : getNextSmartlink(context);
            adClickCount++;
            Log.d(TAG, "Abriendo Adsterra Smartlink (" + adClickCount + ", +18=" + adult18PlusEnabled + "): " + target);

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(target));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error abriendo Smartlink: " + e.getMessage());
        }
    }

    public static void triggerAdsterraReward(Activity activity, WebView webView) {
        if (activity == null) return;
        // Prioridad 1: Video Reward nativo de Start.io (funciona fuera de Play Store)
        if (StartIoRewardManager.isConfigured()) {
            boolean shown = StartIoRewardManager.showRewardedVideo(activity, webView);
            if (shown) {
                return;
            }
        }
        // Respaldo: Adsterra Smartlink si modo +18 está activo
        if (isAdult18PlusEnabled(activity)) {
            openDirectSponsorUrl(activity, null);
        } else {
            Log.d(TAG, "Modo Seguro (+18=false): descarga desbloqueada sin anuncio externo.");
        }
        if (webView != null) {
            webView.post(new EvaluateJsRunnable(webView, "if(window.onAdsterraRewardUnlocked) window.onAdsterraRewardUnlocked();"));
        }
    }

    public static void triggerInterstitial(Activity activity, WebView webView) {
        if (activity == null || webView == null) return;
        if (StartIoRewardManager.isConfigured()) {
            StartIoRewardManager.showInterstitial(activity);
            return;
        }
        if (!isAdult18PlusEnabled(activity)) return;
        activity.runOnUiThread(new EvaluateJsRunnable(webView, "if(window.showMonochromeInterstitial) window.showMonochromeInterstitial();"));
    }

    public static String getAdConfig() {
        JSONObject obj = new JSONObject();
        try {
            String[] pool = adult18PlusEnabled ? ALL_SMARTLINKS : SAFE_SMARTLINKS;
            obj.put("network", ACTIVE_NETWORK);
            obj.put("format", "Adsterra Smartlink (Direct Link)");
            obj.put("activeLinksCount", pool.length);
            obj.put("adult18Plus", adult18PlusEnabled);
            obj.put("currentLink", getCurrentSmartlink());
            obj.put("frequency", INTERSTITIAL_FREQUENCY);
            obj.put("downloadsCompleted", downloadCount);
            obj.put("adsOpened", adClickCount);
            obj.put("nextAdIn", INTERSTITIAL_FREQUENCY - (downloadCount % INTERSTITIAL_FREQUENCY));
        } catch (Exception ignored) {}
        return obj.toString();
    }
}
