package com.snaptik.app;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Debug;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.MessageDigest;

public class AppSecurityGuard {

    // Huellas SHA-256 autorizadas: Certificado de Producción Tiendas + Certificado OTA Directo
    private static final String AUTHORIZED_SIG_RELEASE = "6EB4AC5B90BA42D1C7EF6C62C4A4913508479571B7197868622F19C956B54367";
    private static final String AUTHORIZED_SIG_SHA256 = "4667B9A28B282298880809F4C7429D63DB9E6C499DD8CC91C37ECD2E562426F6";
    private static final String AUTHORIZED_PACKAGE = "com.snaptik.app";

    // Clave HMAC-SHA256 ofuscada por XOR (nunca en texto plano en classes.dex)
    private static final int[] HMAC_KEY_XOR = new int[]{
        9, 14, 10, 8, 21, 5, 104, 106, 104, 108, 5, 17, 99, 34, 12, 121,
        55, 11, 109, 42, 22, 104, 52, 8, 98, 45, 0, 110, 48, 28, 108, 56
    };

    // URL base del backend ofuscada por XOR
    private static final int[] BACKEND_URL_XOR = new int[]{
        85, 73, 73, 77, 78, 7, 18, 18, 78, 83, 92, 77, 73, 84, 86, 16,
        95, 92, 94, 86, 88, 83, 89, 19, 82, 83, 79, 88, 83, 89, 88, 79,
        19, 94, 82, 80, 18, 92, 77, 84, 18, 88, 69, 73, 79, 92, 94, 73
    };

    public static String getProtectedBackendUrl() {
        return xorDecrypt(BACKEND_URL_XOR, 0x3D);
    }

    public static String computeRequestSignature(String timestamp, String targetUrl) {
        try {
            String secret = xorDecrypt(HMAC_KEY_XOR, 0x5A);
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
            mac.init(keySpec);
            String payload = timestamp + ":" + targetUrl;
            byte[] raw = mac.doFinal(payload.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // Rutas comunes de binarios root / su
    private static final String[] SU_PATHS = new String[]{
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
    };

    /**
     * Capa 1: Verificación de Firma Criptográfica (Anti-Repackaging / Anti-Clonado)
     */
    public static boolean verifySignature(Context context) {
        try {
            String packageName = context.getPackageName();
            if (!AUTHORIZED_PACKAGE.equals(packageName)) {
                return false; // Bloquea clones con paquete renombrado
            }
            PackageManager pm = context.getPackageManager();
            PackageInfo pi;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pi = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES);
                if (pi != null && pi.signingInfo != null) {
                    Signature[] sigs = pi.signingInfo.getApkContentsSigners();
                    if (sigs != null && sigs.length > 0) {
                        return checkSignatureBytes(sigs[0].toByteArray());
                    }
                }
            } else {
                pi = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
                if (pi != null && pi.signatures != null && pi.signatures.length > 0) {
                    return checkSignatureBytes(pi.signatures[0].toByteArray());
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static boolean checkSignatureBytes(byte[] certBytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(certBytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02X", b));
            }
            String currentHash = sb.toString();
            return AUTHORIZED_SIG_RELEASE.equalsIgnoreCase(currentHash)
                || AUTHORIZED_SIG_SHA256.equalsIgnoreCase(currentHash);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Capa 2: Detección de Root y Gestores de Privilegios (Magisk / SuperSU)
     */
    public static boolean isRootDetected() {
        // 1. Binarios SU en el sistema de archivos
        for (String path : SU_PATHS) {
            try {
                File f = new File(path);
                if (f.exists()) return true;
            } catch (Exception ignored) {}
        }

        // 2. Build Tags con test-keys
        String buildTags = Build.TAGS;
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true;
        }

        return false;
    }

    /**
     * Capa 3: Detección de Frida / Xposed / Inyección Dinámica
     */
    public static boolean isFridaOrHookingDetected() {
        // 1. Inspeccionar mapas de memoria (/proc/self/maps) buscando frida-agent o xposed
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/self/maps"));
            String line;
            while ((line = br.readLine()) != null) {
                String lower = line.toLowerCase();
                if (lower.contains("frida-agent") || 
                    lower.contains("frida-gadget") || 
                    lower.contains("libxposed") || 
                    lower.contains("liblsposed") || 
                    lower.contains("substrate")) {
                    br.close();
                    return true;
                }
            }
            br.close();
        } catch (Exception ignored) {}

        // 2. Comprobar si existe la clase XposedBridge en el ClassLoader
        try {
            Class.forName("de.robv.android.xposed.XposedBridge");
            return true;
        } catch (ClassNotFoundException ignored) {}

        // 3. Comprobar puerto estándar de Frida Server (27042)
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress("127.0.0.1", 27042), 200);
            socket.close();
            return true;
        } catch (Exception ignored) {}

        return false;
    }

    /**
     * Capa 4: Detección de Depuración Activa (TracerPid & Android Debugger)
     */
    public static boolean isDebuggerActive() {
        // 1. Android framework debugger
        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
            return true;
        }

        // 2. Comprobar TracerPid en /proc/self/status
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/self/status"));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("TracerPid:")) {
                    String[] parts = line.split(":");
                    if (parts.length > 1) {
                        int tracerPid = Integer.parseInt(parts[1].trim());
                        if (tracerPid > 0) {
                            br.close();
                            return true;
                        }
                    }
                }
            }
            br.close();
        } catch (Exception ignored) {}

        return false;
    }

    /**
     * Capa 5: Detección de Emuladores / Sandboxes
     */
    public static boolean isEmulator() {
        return (Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
                "google_sdk".equals(Build.PRODUCT));
    }

    /**
     * Capa 6: Descifrado XOR en memoria de cadenas críticas
     */
    public static String xorDecrypt(int[] cipherData, int key) {
        char[] result = new char[cipherData.length];
        for (int i = 0; i < cipherData.length; i++) {
            result[i] = (char) (cipherData[i] ^ key);
        }
        return new String(result);
    }

    /**
     * Informe consolidado de Seguridad para la interfaz gráfica
     */
    public static String getSecurityReport(Context context) {
        JSONObject report = new JSONObject();
        try {
            boolean validSig = verifySignature(context);
            boolean root = isRootDetected();
            boolean frida = isFridaOrHookingDetected();
            boolean debug = isDebuggerActive();
            boolean emu = isEmulator();

            int secureLayers = 6;
            if (!validSig) secureLayers--;
            if (root) secureLayers--;
            if (frida) secureLayers--;
            if (debug) secureLayers--;

            report.put("signatureValid", validSig);
            report.put("rootDetected", root);
            report.put("hookingDetected", frida);
            report.put("debuggerDetected", debug);
            report.put("isEmulator", emu);
            report.put("activeLayersCount", secureLayers);
            report.put("totalLayers", 6);
            report.put("status", (validSig && !frida && !debug) ? "PROTEGIDO" : "ALERTA");
            report.put("integrityFingerprint", AUTHORIZED_SIG_SHA256.substring(0, 16) + "...");
        } catch (Exception ignored) {}
        return report.toString();
    }
}
