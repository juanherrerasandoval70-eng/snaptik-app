package com.snaptik.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebView;
import org.json.JSONObject;

public class AdNetworkManager {

    private static final String TAG = "AdsterraSnapTik";
    private static int downloadCount = 0;
    private static int adClickCount = 0;
    private static final int INTERSTITIAL_FREQUENCY = 2; // Cada 2 descargas

    // Red Activa: Adsterra Smartlinks de Alto eCPM
    private static final String ACTIVE_NETWORK = "Adsterra Smartlinks (High eCPM)";

    // 5 Smartlinks Activos configurados por el usuario
    public static final String[] SMARTLINKS = {
        "https://asiafilm.org/4/ed7357907f05efc3d793c5aedbdf0c5b", // Smartlink_3 (ID: 31224747)
        "https://asiafilm.org/4/b70dfeb9161b32fbda5a5d24daca1943", // Smartlink_2 (ID: 31224644)
        "https://auctionr.org/4/ceb82fb7e10796fc95547d9b50cfe003", // Smartlink_1 (ID: 31224274)
        "https://auctionr.org/4/048d8aa6b801685694fdae38b9f9cb5b", // Smartlink_4 (ID: 32351281)
        "https://asiafilm.org/4/7f20a7584445d119b20cbbaa4fc4012c"  // Smartlink_5 (ID: 32351410)
    };

    private static int currentLinkIndex = 0;

    /**
     * Obtiene el siguiente Smartlink activo en rotación balanceada.
     */
    public static synchronized String getNextSmartlink() {
        String link = SMARTLINKS[currentLinkIndex];
        currentLinkIndex = (currentLinkIndex + 1) % SMARTLINKS.length;
        return link;
    }

    public static synchronized String getCurrentSmartlink() {
        return SMARTLINKS[currentLinkIndex];
    }

    /**
     * Registra una descarga completada e incrementa contador.
     */
    public static synchronized boolean recordDownloadAndCheckAdTrigger() {
        downloadCount++;
        return (downloadCount % INTERSTITIAL_FREQUENCY == 0);
    }

    /**
     * Abre el Smartlink de Adsterra en el navegador del dispositivo para monetizar al usuario.
     */
    public static synchronized void openDirectSponsorUrl(Context context, String customUrl) {
        if (context == null) return;
        try {
            String target = (customUrl != null && !customUrl.trim().isEmpty()) ? customUrl.trim() : getNextSmartlink();
            adClickCount++;
            Log.d(TAG, "Abriendo Adsterra Smartlink (" + adClickCount + "): " + target);

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(target));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error abriendo Smartlink: " + e.getMessage());
        }
    }

    /**
     * Dispara la recompensa/patrocinador de Adsterra y luego autoriza la descarga 1080p.
     */
    public static void triggerAdsterraReward(Activity activity, WebView webView) {
        if (activity == null) return;
        openDirectSponsorUrl(activity, null);
        if (webView != null) {
            webView.post(new EvaluateJsRunnable(webView, "if(window.onAdsterraRewardUnlocked) window.onAdsterraRewardUnlocked();"));
        }
    }

    /**
     * Dispara el anuncio interstitial nativo en la interfaz WebView si corresponde.
     */
    public static void triggerInterstitial(Activity activity, WebView webView) {
        if (activity == null || webView == null) return;
        activity.runOnUiThread(new EvaluateJsRunnable(webView, "if(window.showMonochromeInterstitial) window.showMonochromeInterstitial();"));
    }

    /**
     * Devuelve la configuración de monetización en formato JSON para la vista de Ajustes.
     */
    public static String getAdConfig() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("network", ACTIVE_NETWORK);
            obj.put("format", "Adsterra Smartlink (Direct Link)");
            obj.put("activeLinksCount", SMARTLINKS.length);
            obj.put("currentLink", getCurrentSmartlink());
            obj.put("frequency", INTERSTITIAL_FREQUENCY);
            obj.put("downloadsCompleted", downloadCount);
            obj.put("adsOpened", adClickCount);
            obj.put("nextAdIn", INTERSTITIAL_FREQUENCY - (downloadCount % INTERSTITIAL_FREQUENCY));
        } catch (Exception ignored) {}
        return obj.toString();
    }
}
