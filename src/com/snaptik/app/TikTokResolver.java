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
    public static String PRIVATE_BACKEND_URL = AppSecurityGuard.getProtectedBackendUrl();

    public static void setCustomBackendUrl(String url) {
        if (url != null && !url.trim().isEmpty()) {
            PRIVATE_BACKEND_URL = url.trim();
        }
    }

    public static String resolve(String tiktokUrl) {
        JSONObject res = new JSONObject();
        try {
            String cleanUrl = normalizeUrl(tiktokUrl);
            if (cleanUrl == null) {
                res.put("success", false);
                res.put("error", "URL de TikTok inválida. Debe ser un enlace de tiktok.com.");
                return res.toString();
            }

            // Motor 0 (Prioridad 1): Nuestro Servidor Backend Privado
            if (PRIVATE_BACKEND_URL != null && !PRIVATE_BACKEND_URL.isEmpty()) {
                JSONObject backendResult = resolveViaPrivateBackend(cleanUrl);
                if (backendResult != null && backendResult.optBoolean("success", false)) {
                    return backendResult.toString();
                }
            }

            // Motor 1 (Principal): SSSTik Protocol (CDN limpia tikcdn.io que no bloquea en Android)
            JSONObject sssResult = resolveViaSSSTik(cleanUrl);
            if (sssResult != null && sssResult.optBoolean("success", false)) {
                return sssResult.toString();
            }

            // Motor 2 (Respaldo): TikWM Direct HD API
            JSONObject tikwmResult = resolveViaTikWM(cleanUrl);
            if (tikwmResult != null && tikwmResult.optBoolean("success", false)) {
                return tikwmResult.toString();
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

    private static JSONObject resolveViaPrivateBackend(String cleanUrl) {
        try {
            JSONObject body = new JSONObject();
            body.put("url", cleanUrl);
            String ts = String.valueOf(System.currentTimeMillis() / 1000L);
            String sig = AppSecurityGuard.computeRequestSignature(ts, cleanUrl);
            String[] headers = new String[]{
                "Content-Type", "application/json; charset=UTF-8",
                "Accept", "application/json",
                "X-App-Client", "com.snaptik.app",
                "X-App-Timestamp", ts,
                "X-App-Signature", sig
            };
            String resp = fetchUrl(PRIVATE_BACKEND_URL, "POST", body.toString(), headers);
            if (resp != null && !resp.trim().isEmpty()) {
                JSONObject root = new JSONObject(resp);
                if (root.optBoolean("success", false)) {
                    JSONObject d = root.optJSONObject("data");
                    if (d != null) {
                        return root;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String ensureAbsoluteTikWmUrl(String u) {
        if (u == null || u.isEmpty() || "null".equalsIgnoreCase(u)) return "";
        if (u.startsWith("//")) return "https:" + u;
        if (u.startsWith("/")) return "https://www.tikwm.com" + u;
        return u;
    }

    private static String formatMegabytes(long bytes) {
        if (bytes <= 0) return "";
        double mb = bytes / (1024.0 * 1024.0);
        return String.format(java.util.Locale.US, "%.1f MB", mb);
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
            String playUrl = ensureAbsoluteTikWmUrl(d.optString("play", ""));
            String hdPlayUrl = ensureAbsoluteTikWmUrl(d.optString("hdplay", ""));
            String wmPlayUrl = ensureAbsoluteTikWmUrl(d.optString("wmplay", ""));
            if (playUrl.isEmpty()) playUrl = hdPlayUrl;
            if (hdPlayUrl.isEmpty()) hdPlayUrl = playUrl;

            JSONArray rawImagesArr = d.optJSONArray("images");
            JSONArray cleanImagesArr = new JSONArray();
            JSONArray previewImagesArr = new JSONArray();
            if (rawImagesArr != null && rawImagesArr.length() > 0) {
                int maxB64 = Math.min(rawImagesArr.length(), 10);
                for (int i = 0; i < rawImagesArr.length(); i++) {
                    String imgU = ensureAbsoluteTikWmUrl(rawImagesArr.optString(i, ""));
                    if (!imgU.isEmpty()) {
                        cleanImagesArr.put(imgU);
                        if (i < maxB64) {
                            String b64Img = fetchImageAsDataUri(imgU);
                            previewImagesArr.put((b64Img != null && !b64Img.isEmpty()) ? b64Img : imgU);
                        } else {
                            previewImagesArr.put(imgU);
                        }
                    }
                }
            }

            boolean isPhotoPost = cleanImagesArr.length() > 0;

            if (playUrl.isEmpty() && !isPhotoPost) {
                res.put("success", false);
                res.put("error", "No se encontró enlace directo de reproducción");
                return res;
            }

            String audioUrl = ensureAbsoluteTikWmUrl(d.optString("music", ""));
            if (isPhotoPost && (playUrl.contains("audio_mpeg") || playUrl.contains("mime_type=audio"))) {
                if (audioUrl.isEmpty()) {
                    audioUrl = playUrl;
                }
                playUrl = "";
                hdPlayUrl = "";
            }
            String musicTitle = "Audio Original";
            JSONObject musicInfo = d.optJSONObject("music_info");
            if (musicInfo != null) {
                if (audioUrl.isEmpty()) {
                    audioUrl = ensureAbsoluteTikWmUrl(musicInfo.optString("play", ""));
                }
                String mTitle = musicInfo.optString("title", "").trim();
                String mAuthor = musicInfo.optString("author", "").trim();
                if (!mTitle.isEmpty()) {
                    musicTitle = !mAuthor.isEmpty() ? (mTitle + " • " + mAuthor) : mTitle;
                }
            }
            if (audioUrl.isEmpty() && isPhotoPost && !playUrl.isEmpty()) {
                audioUrl = playUrl;
            }

            String rawCover = ensureAbsoluteTikWmUrl(d.optString("origin_cover", ""));
            if (rawCover.isEmpty()) {
                rawCover = ensureAbsoluteTikWmUrl(d.optString("cover", ""));
            }
            if (rawCover.isEmpty() && isPhotoPost) {
                rawCover = cleanImagesArr.optString(0, "");
            }

            String title = d.optString("title", "").trim();

            JSONObject authorObj = d.optJSONObject("author");
            String nickname = "Creador";
            String uniqueId = "tiktok_user";
            String rawAvatar = "";

            if (authorObj != null) {
                nickname = authorObj.optString("nickname", "Creador");
                uniqueId = authorObj.optString("unique_id", "tiktok_user");
                rawAvatar = ensureAbsoluteTikWmUrl(authorObj.optString("avatar", ""));
            }

            if (title.isEmpty()) {
                title = (isPhotoPost ? "Carrusel de " : "Vídeo de ") + nickname + " (@" + uniqueId + ")";
            }

            String avatarFinal = fetchImageAsDataUri(rawAvatar);
            if (avatarFinal == null || avatarFinal.isEmpty()) {
                avatarFinal = rawAvatar;
            }

            String coverFinal = (isPhotoPost && previewImagesArr.length() > 0)
                    ? previewImagesArr.optString(0, "")
                    : fetchImageAsDataUri(rawCover);
            if (coverFinal == null || coverFinal.isEmpty()) {
                coverFinal = rawCover.isEmpty() ? avatarFinal : rawCover;
            }

            long sizeBytes = d.optLong("size", 0);
            long hdSizeBytes = d.optLong("hd_size", 0);
            if (hdSizeBytes <= 0) hdSizeBytes = sizeBytes;
            int duration = d.optInt("duration", 0);
            int width = d.optInt("width", 0);
            int height = d.optInt("height", 0);
            int maxDim = Math.max(width, height);
            int minDim = Math.min(width, height);

            boolean is4k = !isPhotoPost && (
                    maxDim >= 2560 || minDim >= 1440 ||
                    (hdSizeBytes > 0 && duration > 0 && (hdSizeBytes / Math.max(1, duration)) > 550000) ||
                    (hdSizeBytes > 18 * 1024 * 1024)
            );
            String detectedQuality = isPhotoPost
                    ? ("CARRUSEL (" + cleanImagesArr.length() + " FOTOS + MÚSICA)")
                    : (is4k ? "4K UHD DETECTADO" : ((hdSizeBytes > sizeBytes || maxDim >= 1920) ? "1080p FULL HD" : "720p HD"));

            JSONObject data = new JSONObject();
            data.put("id", d.optString("id", "video_" + System.currentTimeMillis()));
            data.put("source_url", cleanUrl);
            data.put("title", title);
            data.put("preview_url", playUrl);
            data.put("video_url", playUrl);
            data.put("hd_url", !hdPlayUrl.isEmpty() ? hdPlayUrl : playUrl);
            data.put("uhd_4k_url", !hdPlayUrl.isEmpty() ? hdPlayUrl : playUrl);
            data.put("sd_url", playUrl);
            data.put("wm_url", wmPlayUrl);
            data.put("audio_url", audioUrl);
            data.put("music_title", musicTitle);
            data.put("cover_url", coverFinal);
            data.put("raw_cover_url", rawCover);
            data.put("size_mb", formatMegabytes(sizeBytes));
            data.put("hd_size_mb", formatMegabytes(hdSizeBytes));
            data.put("duration", duration);
            data.put("width", width);
            data.put("height", height);
            data.put("is_4k", is4k);
            data.put("detected_quality", detectedQuality);
            if (isPhotoPost) {
                data.put("images", cleanImagesArr);
                data.put("images_preview", previewImagesArr);
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
            JSONArray previewImagesArr = new JSONArray();

            // 1. Extraer diapositivas desde input hidden slides_data (formato base64 JSON nuevo de SSSTik)
            Matcher slidesMatcher = Pattern.compile("name=[\"']?slides_data[\"']?[^>]*value=[\"'](.*?)[\"']").matcher(postHtml);
            if (slidesMatcher.find()) {
                try {
                    String rawB64 = slidesMatcher.group(1);
                    byte[] decoded = android.util.Base64.decode(rawB64, android.util.Base64.DEFAULT);
                    String jsonStr = new String(decoded, "UTF-8");
                    JSONObject slidesJson = new JSONObject(jsonStr);
                    java.util.Iterator<String> keys = slidesJson.keys();
                    java.util.List<Integer> intKeys = new java.util.ArrayList<>();
                    while (keys.hasNext()) {
                        String k = keys.next();
                        try {
                            intKeys.add(Integer.parseInt(k));
                        } catch (Exception ignored) {}
                    }
                    java.util.Collections.sort(intKeys);
                    for (Integer idx : intKeys) {
                        JSONObject slideObj = slidesJson.optJSONObject(String.valueOf(idx));
                        if (slideObj != null) {
                            String slideUrl = slideObj.optString("url", "");
                            if (!slideUrl.isEmpty()) {
                                if (!slideUrl.startsWith("https://tikcdn.io/")) {
                                    String b64Img = Base64.encodeToString(slideUrl.getBytes("UTF-8"), Base64.NO_WRAP);
                                    slideUrl = "https://tikcdn.io/ssstik/" + b64Img;
                                }
                                imagesArr.put(slideUrl);
                                if (previewImagesArr.length() < 10) {
                                    String b64 = fetchImageAsDataUri(slideUrl);
                                    previewImagesArr.put((b64 != null && !b64.isEmpty()) ? b64 : slideUrl);
                                } else {
                                    previewImagesArr.put(slideUrl);
                                }
                            }
                        }
                    }
                    if (audioUrl.isEmpty()) {
                        String rawMusic = slidesJson.optString("music", "");
                        if (!rawMusic.isEmpty()) {
                            if (!rawMusic.startsWith("https://tikcdn.io/")) {
                                String b64Music = Base64.encodeToString(rawMusic.getBytes("UTF-8"), Base64.NO_WRAP);
                                audioUrl = "https://tikcdn.io/ssstik/m/" + b64Music;
                            } else {
                                audioUrl = rawMusic;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

            // 2. Extraer desde data-splide-lazy (fallback formato clásico)
            Matcher imgMatcher = Pattern.compile("data-splide-lazy=[\"'](https?://[^\"']+)[\"']").matcher(postHtml);
            int idxImg = 0;
            while (imgMatcher.find()) {
                String imgU = imgMatcher.group(1);
                imagesArr.put(imgU);
                if (idxImg < 10) {
                    String b64 = fetchImageAsDataUri(imgU);
                    previewImagesArr.put((b64 != null && !b64.isEmpty()) ? b64 : imgU);
                } else {
                    previewImagesArr.put(imgU);
                }
                idxImg++;
            }

            // 3. Revisar si videoUrl es en realidad una imagen individual de diapositiva
            boolean isVideoActuallyImg = false;
            if (!videoUrl.isEmpty()) {
                String lowerV = videoUrl.toLowerCase();
                if (lowerV.contains("photomode") || lowerV.endsWith(".webp") || lowerV.endsWith(".jpg") || lowerV.endsWith(".png") || lowerV.endsWith(".jpeg")) {
                    isVideoActuallyImg = true;
                } else if (lowerV.contains("/ssstik/ahr0") || lowerV.contains("/ssstik/a/")) {
                    try {
                        String b64Part = videoUrl.substring(videoUrl.indexOf("/ssstik/") + 8);
                        if (b64Part.contains("?")) b64Part = b64Part.substring(0, b64Part.indexOf("?"));
                        if (b64Part.startsWith("a/")) b64Part = b64Part.substring(2);
                        byte[] dBytes = android.util.Base64.decode(b64Part, android.util.Base64.DEFAULT);
                        String dec = new String(dBytes, "UTF-8").toLowerCase();
                        if (dec.contains("photomode") || dec.contains(".webp") || dec.contains(".jpg") || dec.contains(".png") || dec.contains(".jpeg")) {
                            isVideoActuallyImg = true;
                        }
                    } catch (Exception ignored) {}
                }
            }

            if (isVideoActuallyImg) {
                if (imagesArr.length() == 0) {
                    imagesArr.put(videoUrl);
                    previewImagesArr.put(videoUrl);
                }
                videoUrl = "";
                previewUrl = "";
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

            boolean isPhoto = imagesArr.length() > 0;
            JSONObject data = new JSONObject();
            data.put("id", videoId);
            data.put("source_url", cleanUrl);
            data.put("title", desc);
            data.put("preview_url", isPhoto ? "" : previewUrl);
            data.put("video_url", isPhoto ? "" : videoUrl);
            data.put("hd_url", isPhoto ? "" : videoUrl);
            data.put("uhd_4k_url", isPhoto ? "" : videoUrl);
            data.put("sd_url", isPhoto ? "" : videoUrl);
            data.put("audio_url", audioUrl);
            data.put("music_title", "Audio Original");
            data.put("cover_url", coverFinal);
            data.put("raw_cover_url", rawCover);
            data.put("detected_quality", isPhoto ? ("CARRUSEL (" + imagesArr.length() + " FOTOS + MÚSICA)") : "1080p FULL HD");
            if (isPhoto) {
                data.put("images", imagesArr);
                data.put("images_preview", previewImagesArr);
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
        Matcher m = Pattern.compile("(https?://(?:[a-zA-Z0-9-]+\\.)?(?:tiktok|douyin)\\.com/[^\\s\"']+)").matcher(input);
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
