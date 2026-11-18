package com.snaptik.app;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TikTokResolver {

    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    public static String resolve(String tiktokUrl) {
        JSONObject res = new JSONObject();
        try {
            String cleanUrl = normalizeUrl(tiktokUrl);
            if (cleanUrl == null) {
                res.put("success", false);
                res.put("error", "URL de TikTok inválida. Debe ser un enlace de tiktok.com.");
                return res.toString();
            }

            // Intento 1: SSSTik Protocol
            JSONObject sssResult = resolveViaSSSTik(cleanUrl);
            if (sssResult != null && sssResult.optBoolean("success", false)) {
                return sssResult.toString();
            }

            // Intento 2: TikWM Direct API Fallback (admite también carruseles de fotos)
            JSONObject tikwmResult = resolveViaTikWM(cleanUrl);
            if (tikwmResult != null && tikwmResult.optBoolean("success", false)) {
                return tikwmResult.toString();
            }

            // Si ambos fallaron
            res.put("success", false);
            String err = sssResult != null ? sssResult.optString("error", "Fallo de extracción") : "El video es privado o no está disponible.";
            res.put("error", err);
            return res.toString();

        } catch (Exception e) {
            try {
                res.put("success", false);
                res.put("error", "Error interno: " + e.getMessage());
            } catch (Exception ignored) {}
            return res.toString();
        }
    }

    private static JSONObject resolveViaSSSTik(String cleanUrl) {
        try {
            JSONObject res = new JSONObject();
            // 1. Obtener s_tt fresco
            String homeHtml = fetchUrl("https://ssstik.io/es", "GET", null, null);
            Matcher ttMatcher = Pattern.compile("s_tt\\s*=\\s*'([^']+)'").matcher(homeHtml);
            Matcher furlMatcher = Pattern.compile("s_furl\\s*=\\s*'([^']+)'").matcher(homeHtml);

            if (!ttMatcher.find()) {
                res.put("success", false);
                res.put("error", "No se pudo obtener token de verificación SSSTik.");
                return res;
            }

            String sTt = ttMatcher.group(1);
            String sFurl = furlMatcher.find() ? furlMatcher.group(1) : "abc";

            // 2. Enviar POST HTMX
            String postUrl = "https://ssstik.io/" + sFurl + "?url=dl";
            String postBody = "id=" + URLEncoder.encode(cleanUrl, "UTF-8")
                    + "&locale=es&tt=" + URLEncoder.encode(sTt, "UTF-8");

            String[] extraHeaders = new String[]{
                    "HX-Request", "true",
                    "HX-Target", "target",
                    "HX-Current-URL", "https://ssstik.io/es",
                    "Origin", "https://ssstik.io",
                    "Referer", "https://ssstik.io/es",
                    "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8"
            };

            String postHtml = fetchUrl(postUrl, "POST", postBody, extraHeaders);

            if (postHtml.contains("video is currently unavailable") || postHtml.contains("Error code: 3111")) {
                res.put("success", false);
                res.put("error", "El video es privado o fue eliminado de TikTok.");
                return res;
            }

            // 3. Extraer enlaces de video o fotos
            String videoUrl = "";
            Matcher vMatcher = Pattern.compile("href=[\"'](https?://tikcdn\\.io/ssstik/[^\"']+)[\"']").matcher(postHtml);
            if (vMatcher.find()) {
                videoUrl = vMatcher.group(1);
            }

            String audioUrl = "";
            Matcher aMatcher = Pattern.compile("href=[\"'](https?://tikcdn\\.io/ssstik/m/[^\"']+)[\"']").matcher(postHtml);
            if (aMatcher.find()) {
                audioUrl = aMatcher.group(1);
            }

            // Detectar imágenes de carrusel en SSSTik
            JSONArray imagesArr = new JSONArray();
            Matcher imgMatcher = Pattern.compile("data-splide-lazy=[\"'](https?://[^\"']+)[\"']").matcher(postHtml);
            while (imgMatcher.find()) {
                imagesArr.put(imgMatcher.group(1));
            }

            String authorName = "Creador";
            Matcher nameMatcher = Pattern.compile("<h2>(.*?)</h2>").matcher(postHtml);
            if (nameMatcher.find()) {
                authorName = nameMatcher.group(1).replaceAll("<[^>]+>", "").trim();
            }

            String authorAvatar = "";
            Matcher avatarMatcher = Pattern.compile("<img[^>]*class=[\"']result_author[\"'][^>]*src=[\"']([^\"']+)[\"']").matcher(postHtml);
            if (avatarMatcher.find()) {
                authorAvatar = avatarMatcher.group(1);
            }

            String desc = "";
            Matcher descMatcher = Pattern.compile("<p[^>]*class=[\"']maintext[\"'][^>]*>(.*?)</p>").matcher(postHtml);
            if (descMatcher.find()) {
                desc = descMatcher.group(1).replaceAll("<[^>]+>", "").trim();
            }

            String videoId = "video_" + System.currentTimeMillis();
            Matcher idMatcher = Pattern.compile("/(?:video|photo)/(\\d+)").matcher(cleanUrl);
            if (idMatcher.find()) {
                videoId = idMatcher.group(1);
            }

            if (videoUrl.isEmpty() && imagesArr.length() == 0) {
                res.put("success", false);
                res.put("error", "No se encontró enlace multimedia en la respuesta.");
                return res;
            }

            JSONObject data = new JSONObject();
            data.put("id", videoId);
            data.put("title", desc);
            data.put("video_url", videoUrl);
            data.put("hd_url", videoUrl);
            data.put("audio_url", audioUrl);
            data.put("cover_url", authorAvatar);
            if (imagesArr.length() > 0) {
                data.put("images", imagesArr);
            }

            JSONObject author = new JSONObject();
            author.put("nickname", authorName);
            author.put("unique_id", authorName.toLowerCase().replaceAll("[^a-z0-9_.]", ""));
            author.put("avatar", authorAvatar);
            data.put("author", author);

            res.put("success", true);
            res.put("data", data);
            return res;

        } catch (Exception e) {
            return null;
        }
    }

    private static JSONObject resolveViaTikWM(String cleanUrl) {
        try {
            JSONObject res = new JSONObject();
            String apiUrl = "https://www.tikwm.com/api/?url=" + URLEncoder.encode(cleanUrl, "UTF-8");
            String jsonResp = fetchUrl(apiUrl, "GET", null, null);

            JSONObject root = new JSONObject(jsonResp);
            int code = root.optInt("code", -1);
            if (code != 0) {
                res.put("success", false);
                res.put("error", root.optString("msg", "Error en TikWM"));
                return res;
            }

            JSONObject d = root.getJSONObject("data");
            String playUrl = d.optString("play", "");
            String hdPlayUrl = d.optString("hdplay", "");
            if (playUrl.isEmpty()) playUrl = hdPlayUrl;
            if (hdPlayUrl.isEmpty()) hdPlayUrl = playUrl;

            JSONArray imagesArr = d.optJSONArray("images");

            if (playUrl.isEmpty() && (imagesArr == null || imagesArr.length() == 0)) {
                res.put("success", false);
                res.put("error", "No se encontró enlace directo de reproducción");
                return res;
            }

            JSONObject data = new JSONObject();
            data.put("id", d.optString("id", "video"));
            data.put("title", d.optString("title", "Video de TikTok"));
            data.put("video_url", playUrl);
            data.put("hd_url", hdPlayUrl);
            data.put("audio_url", d.optString("music", ""));
            data.put("cover_url", d.optString("cover", ""));
            if (imagesArr != null && imagesArr.length() > 0) {
                data.put("images", imagesArr);
            }

            JSONObject author = new JSONObject();
            JSONObject authorObj = d.optJSONObject("author");
            if (authorObj != null) {
                author.put("nickname", authorObj.optString("nickname", "Creador"));
                author.put("unique_id", authorObj.optString("unique_id", "tiktok_user"));
                author.put("avatar", authorObj.optString("avatar", ""));
            } else {
                author.put("nickname", "Creador");
                author.put("unique_id", "tiktok_user");
                author.put("avatar", "");
            }
            data.put("author", author);

            res.put("success", true);
            res.put("data", data);
            return res;
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalizeUrl(String input) {
        if (input == null) return null;
        Matcher m = Pattern.compile("(https?://(?:vm\\.|vt\\.|www\\.|m\\.)?tiktok\\.com/[^\\s\"']+)").matcher(input);
        if (m.find()) {
            String raw = m.group(1);
            int qIdx = raw.indexOf('?');
            return qIdx > 0 ? raw.substring(0, qIdx) : raw;
        }
        return null;
    }

    private static String fetchUrl(String targetUrl, String method, String body, String[] headers) throws Exception {
        URL url = new URL(targetUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", UA);
        conn.setRequestProperty("Accept", "*/*");
        conn.setRequestProperty("Accept-Language", "es-ES,es;q=0.9,en;q=0.8");

        if (headers != null) {
            for (int i = 0; i < headers.length; i += 2) {
                conn.setRequestProperty(headers[i], headers[i + 1]);
            }
        }

        if ("POST".equalsIgnoreCase(method) && body != null) {
            conn.setDoOutput(true);
            byte[] bytes = body.getBytes("UTF-8");
            conn.setFixedLengthStreamingMode(bytes.length);
            OutputStream os = conn.getOutputStream();
            os.write(bytes);
            os.flush();
            os.close();
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) return "";

        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        conn.disconnect();
        return sb.toString();
    }
}
