package com.snaptik.app;

import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
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

            // Motor 1 (Principal): TikWM Direct HD API (JSON completo con avatar, cover, descripción, 720p y 1080p)
            JSONObject tikwmResult = resolveViaTikWM(cleanUrl);
            if (tikwmResult != null && tikwmResult.optBoolean("success", false)) {
                return tikwmResult.toString();
            }

            // Motor 2 (Respaldo): SSSTik Protocol (Actualizado a selectores .ssr-ava, .ssr-caption, ssrPreview)
            JSONObject sssResult = resolveViaSSSTik(cleanUrl);
            if (sssResult != null && sssResult.optBoolean("success", false)) {
                return sssResult.toString();
            }

            res.put("success", false);
            String err = tikwmResult != null ? tikwmResult.optString("error", "Fallo de extracción") : "El video es privado o no está disponible.";
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

    private static JSONObject resolveViaTikWM(String cleanUrl) {
        try {
            JSONObject res = new JSONObject();
            String apiUrl = "https://www.tikwm.com/api/?url=" + URLEncoder.encode(cleanUrl, "UTF-8") + "&hd=1";
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
            if (playUrl.isEmpty() || "null".equals(playUrl)) playUrl = hdPlayUrl;
            if (hdPlayUrl.isEmpty() || "null".equals(hdPlayUrl)) hdPlayUrl = playUrl;

            JSONArray imagesArr = d.optJSONArray("images");

            if (playUrl.isEmpty() && (imagesArr == null || imagesArr.length() == 0)) {
                res.put("success", false);
                res.put("error", "No se encontró enlace directo de reproducción");
                return res;
            }

            String rawCover = d.optString("cover", "");
            if (rawCover.isEmpty() || "null".equals(rawCover)) {
                rawCover = d.optString("origin_cover", "");
            }

            String title = d.optString("title", "").trim();

            JSONObject authorObj = d.optJSONObject("author");
            String nickname = "Creador";
            String uniqueId = "tiktok_user";
            String rawAvatar = "";

            if (authorObj != null) {
                nickname = authorObj.optString("nickname", "Creador");
                uniqueId = authorObj.optString("unique_id", "tiktok_user");
                rawAvatar = authorObj.optString("avatar", "");
            }

            if (title.isEmpty()) {
                title = "Vídeo de " + nickname + " (@" + uniqueId + ")";
            }

            // Convertir avatar y cover a Base64 para visualización inmediata y persistencia permanente en Biblioteca
            String avatarFinal = fetchImageAsDataUri(rawAvatar);
            if (avatarFinal == null || avatarFinal.isEmpty()) {
                avatarFinal = rawAvatar;
            }

            String coverFinal = fetchImageAsDataUri(rawCover);
            if (coverFinal == null || coverFinal.isEmpty()) {
                coverFinal = rawCover.isEmpty() ? avatarFinal : rawCover;
            }

            JSONObject data = new JSONObject();
            data.put("id", d.optString("id", "video_" + System.currentTimeMillis()));
            data.put("source_url", cleanUrl);
            data.put("title", title);
            data.put("preview_url", playUrl);
            data.put("video_url", playUrl);
            data.put("hd_url", hdPlayUrl);
            data.put("audio_url", d.optString("music", ""));
            data.put("cover_url", coverFinal);
            if (imagesArr != null && imagesArr.length() > 0) {
                data.put("images", imagesArr);
            }

            JSONObject author = new JSONObject();
            author.put("nickname", nickname);
            author.put("unique_id", uniqueId);
            author.put("avatar", avatarFinal);
            data.put("author", author);

            res.put("success", true);
            res.put("data", data);
            return res;
        } catch (Exception e) {
            return null;
        }
    }

    private static JSONObject resolveViaSSSTik(String cleanUrl) {
        try {
            JSONObject res = new JSONObject();
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

            // Enlace de previsualización en streaming (ssrPreview)
            String previewUrl = "";
            Matcher prevMatcher = Pattern.compile("ssrPreview\\(['\"](https?://tikcdn\\.io/ssstik/v/[^'\"]+)['\"]\\)").matcher(postHtml);
            if (prevMatcher.find()) {
                previewUrl = prevMatcher.group(1);
            }

            // Enlace de descarga directa MP4
            String videoUrl = "";
            Matcher vMatcher = Pattern.compile("href=[\"'](https?://tikcdn\\.io/ssstik/(?!m/)[^\"']+)[\"']").matcher(postHtml);
            if (vMatcher.find()) {
                videoUrl = vMatcher.group(1);
            }
            if (videoUrl.isEmpty() && !previewUrl.isEmpty()) {
                videoUrl = previewUrl;
            }
            if (previewUrl.isEmpty()) {
                previewUrl = videoUrl;
            }

            String audioUrl = "";
            Matcher aMatcher = Pattern.compile("href=[\"'](https?://tikcdn\\.io/ssstik/m/[^\"']+)[\"']").matcher(postHtml);
            if (aMatcher.find()) {
                audioUrl = aMatcher.group(1);
            }

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

            // Soportar tanto el nuevo selector .ssr-ava como .result_author
            String rawAvatar = "";
            Matcher avatarMatcher = Pattern.compile("<img[^>]*class=[\"'][^\"']*(?:ssr-ava|result_author)[^\"']*[\"'][^>]*src=[\"']([^\"']+)[\"']").matcher(postHtml);
            if (avatarMatcher.find()) {
                rawAvatar = avatarMatcher.group(1);
            }

            // Poster de portada (.ssr-poster background-image)
            String rawCover = "";
            Matcher coverMatcher = Pattern.compile("url\\(['\"]?(https?://tikcdn\\.io/ssstik/p/[^'\"\\)]+)['\"]?\\)").matcher(postHtml);
            if (coverMatcher.find()) {
                rawCover = coverMatcher.group(1);
            }

            // Soportar tanto el nuevo selector .ssr-caption como .maintext
            String desc = "";
            Matcher descMatcher = Pattern.compile("<p[^>]*class=[\"'][^\"']*(?:ssr-caption|maintext)[^\"']*[\"'][^>]*>(.*?)</p>", Pattern.DOTALL).matcher(postHtml);
            if (descMatcher.find()) {
                desc = descMatcher.group(1).replaceAll("<[^>]+>", "").trim();
            }
            if (desc.isEmpty()) {
                desc = "Vídeo de " + authorName;
            }

            String videoId = "video_" + System.currentTimeMillis();
            Matcher idMatcher = Pattern.compile("/(?:video|photo)/(\\d+)").matcher(cleanUrl);
            if (idMatcher.find()) {
                videoId = idMatcher.group(1);
            } else {
                Matcher sssIdMatcher = Pattern.compile("/ssstik/(?:v/)?(\\d+)").matcher(videoUrl);
                if (sssIdMatcher.find()) {
                    videoId = sssIdMatcher.group(1);
                }
            }

            if (videoUrl.isEmpty() && imagesArr.length() == 0) {
                res.put("success", false);
                res.put("error", "No se encontró enlace multimedia en la respuesta.");
                return res;
            }

            String avatarFinal = fetchImageAsDataUri(rawAvatar);
            if (avatarFinal == null || avatarFinal.isEmpty()) {
                avatarFinal = rawAvatar;
            }

            String coverFinal = fetchImageAsDataUri(rawCover);
            if (coverFinal == null || coverFinal.isEmpty()) {
                coverFinal = rawCover.isEmpty() ? avatarFinal : rawCover;
            }

            String uniqueId = authorName.toLowerCase().replaceAll("[^a-z0-9_.]", "");
            if (uniqueId.isEmpty()) uniqueId = "tiktok_creator";

            JSONObject data = new JSONObject();
            data.put("id", videoId);
            data.put("source_url", cleanUrl);
            data.put("title", desc);
            data.put("preview_url", previewUrl);
            data.put("video_url", videoUrl);
            data.put("hd_url", videoUrl);
            data.put("audio_url", audioUrl);
            data.put("cover_url", coverFinal);
            if (imagesArr.length() > 0) {
                data.put("images", imagesArr);
            }

            JSONObject author = new JSONObject();
            author.put("nickname", authorName);
            author.put("unique_id", uniqueId);
            author.put("avatar", avatarFinal);
            data.put("author", author);

            res.put("success", true);
            res.put("data", data);
            return res;

        } catch (Exception e) {
            return null;
        }
    }

    private static String fetchImageAsDataUri(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty() || !imageUrl.startsWith("http")) {
            return null;
        }
        HttpURLConnection conn = null;
        try {
            URL url = new URL(imageUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Referer", "https://www.tiktok.com/");

            int status = conn.getResponseCode();
            if (status == 200) {
                String contentType = conn.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    contentType = "image/jpeg";
                }
                InputStream is = conn.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                int total = 0;
                while ((n = is.read(buf)) != -1) {
                    total += n;
                    if (total > 350000) {
                        // Si supera 350KB, usar URL directa para no sobrecargar JSON
                        is.close();
                        return imageUrl;
                    }
                    baos.write(buf, 0, n);
                }
                is.close();
                byte[] bytes = baos.toByteArray();
                if (bytes.length > 0) {
                    String b64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
                    return "data:" + contentType + ";base64," + b64;
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return imageUrl;
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
        conn.setInstanceFollowRedirects(true);

        if (headers != null) {
            for (int i = 0; i < headers.length - 1; i += 2) {
                conn.setRequestProperty(headers[i], headers[i + 1]);
            }
        }

        if ("POST".equalsIgnoreCase(method) && body != null) {
            conn.setDoOutput(true);
            byte[] input = body.getBytes("UTF-8");
            conn.setFixedLengthStreamingMode(input.length);
            OutputStream os = conn.getOutputStream();
            os.write(input);
            os.flush();
            os.close();
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) return "";

        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        br.close();
        conn.disconnect();
        return sb.toString();
    }
}
