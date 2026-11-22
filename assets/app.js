/* SnapTik PRO v3.7.0 — Consumer Application Controller */
document.addEventListener('DOMContentLoaded', () => {
    const defaultCreatorSvg = "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI4MCIgaGVpZ2h0PSI4MCIgdmlld0JveD0iMCAwIDgwIDgwIj48cmVjdCB3aWR0aD0iODAiIGhlaWdodD0iODAiIHJ4PSIxNiIgZmlsbD0iIzE4MTgxQiIvPjxjaXJjbGUgY3g9IjQwIiBjeT0iMzEiIHI9IjEzIiBmaWxsPSJub25lIiBzdHJva2U9IiNGRkZGRkYiIHN0cm9rZS13aWR0aD0iMyIvPjxwYXRoIGQ9Ik0xOCA2NmMwLTExIDEwLTE5IDIyLTE5czIyIDggMjIgMTkiIGZpbGw9Im5vbmUiIHN0cm9rZT0iI0ZGRkZGRiIgc3Ryb2tlLXdpZHRoPSIzIiBzdHJva2UtbGluZWNhcD0icm91bmQiLz48L3N2Zz4=";

    // DOM References
    const urlInput = document.getElementById('tiktokUrlInput');
    const btnClearInput = document.getElementById('btnClearInput');
    const btnPaste = document.getElementById('btnPasteUrl');
    const btnClipboardChip = document.getElementById('btnClipboardChip');
    const txtClipboardChip = document.getElementById('txtClipboardChip');
    const btnSubmit = document.getElementById('btnSubmitExtract');
    const ctaLabel = document.getElementById('ctaLabel');
    const ctaSpinner = document.getElementById('ctaSpinner');
    const errorBox = document.getElementById('errorNoticeBox');
    const errorText = document.getElementById('errorNoticeText');

    // Result Deck
    const resultDeck = document.getElementById('mediaResultDeck');
    const previewStageWrapper = document.getElementById('previewStageWrapper');
    const previewAmbientBg = document.getElementById('previewAmbientBg');
    const videoPlayer = document.getElementById('playerNativeVideo');
    const photoStage = document.getElementById('photoCarouselStage');
    const carouselImg = document.getElementById('carouselActiveImg');
    const carouselCounter = document.getElementById('carouselCounter');
    const btnPrevPhoto = document.getElementById('btnCarouselPrev');
    const btnNextPhoto = document.getElementById('btnCarouselNext');
    const carouselAudio = document.getElementById('playerCarouselAudio');
    const resAuthorAvatar = document.getElementById('resAuthorAvatar');
    const resAuthorHandle = document.getElementById('resAuthorHandle');
    const badgeDetectedRes = document.getElementById('badgeDetectedRes');
    const btnFullscreenVideo = document.getElementById('btnFullscreenVideo');
    const btnDismissDeck = document.getElementById('btnDismissDeck');
    const resVideoCaption = document.getElementById('resVideoCaption');
    const chipRealResolution = document.getElementById('chipRealResolution');
    const chipAspectRatio = document.getElementById('chipAspectRatio');
    const chipRealDuration = document.getElementById('chipRealDuration');

    // Action Tiles
    const btnMpHd = document.getElementById('btnActionMpHd');
    const lbl1080pHeadline = document.getElementById('lbl1080pHeadline');
    const lbl1080pSubhead = document.getElementById('lbl1080pSubhead');
    const btn4k = document.getElementById('btnAction4k');
    const lbl4kSubhead = document.getElementById('lbl4kSubhead');
    const btnMp4 = document.getElementById('btnActionMp4');
    const lbl720pSubhead = document.getElementById('lbl720pSubhead');
    const btnMp3 = document.getElementById('btnActionMp3');
    const lblMp3Headline = document.getElementById('lblMp3Headline');
    const lblMp3Subhead = document.getElementById('lblMp3Subhead');
    const btnPhotos = document.getElementById('btnActionPhotos');
    const lblPhotosHeadline = document.getElementById('lblPhotosHeadline');
    const btnCurrentPhoto = document.getElementById('btnActionCurrentPhoto');
    const lblSinglePhotoHeadline = document.getElementById('lblSinglePhotoHeadline');

    // Quick Tools
    const btnCover = document.getElementById('btnActionCover');
    const btnCopyDesc = document.getElementById('btnCopyDesc');
    const btnShare = document.getElementById('btnActionShare');
    const btnCopyLink = document.getElementById('btnCopyLink');

    // Recents & Library
    const recentsTarget = document.getElementById('recentsStripTarget');
    const btnSeeAllRecents = document.getElementById('btnSeeAllRecents');
    const libraryTarget = document.getElementById('libraryRowsTarget');
    const placeholderEmpty = document.getElementById('placeholderNoDownloads');
    const storageRingArc = document.getElementById('storageRingArc');
    const storageRingLabel = document.getElementById('storageRingLabel');
    const btnOpenDownloads = document.getElementById('btnOpenDownloadsFolder');
    const btnClearLibrary = document.getElementById('btnClearLibraryHistory');
    const inputLibrarySearch = document.getElementById('inputLibrarySearch');
    const btnClearLibSearch = document.getElementById('btnClearLibSearch');

    // Preferences & Settings
    const prefAutoClipboard = document.getElementById('prefAutoClipboard');
    const prefHapticFeedback = document.getElementById('prefHapticFeedback');
    const rowDefaultQuality = document.getElementById('rowDefaultQuality');
    const btnCycleDefaultQuality = document.getElementById('btnCycleDefaultQuality');
    const lblDefaultQualitySub = document.getElementById('lblDefaultQualitySub');
    const btnSettingsOpenFolder = document.getElementById('btnSettingsOpenFolder');
    const btnClearTempCache = document.getElementById('btnClearTempCache');
    const lblCacheStatusSub = document.getElementById('lblCacheStatusSub');
    const btnSettingsHowTo = document.getElementById('btnSettingsHowTo');
    const btnShareApp = document.getElementById('btnShareApp');
    const btnOpenPrivacyPolicy = document.getElementById('btnOpenPrivacyPolicy');

    // Guide Modal
    const modalHowToGuide = document.getElementById('modalHowToGuide');
    const btnOpenHowTo = document.getElementById('btnOpenHowTo');
    const btnCloseHowTo = document.getElementById('btnCloseHowTo');
    const btnGotItHowTo = document.getElementById('btnGotItHowTo');

    // Dynamic Island Download Card
    const dlCard = document.getElementById('activeDownloadCard');
    const dlThumb = document.getElementById('dlCardThumb');
    const dlSpinner = document.getElementById('dlCardSpinner');
    const dlCheck = document.getElementById('dlCardCheck');
    const dlTitle = document.getElementById('dlCardTitle');
    const dlMeta = document.getElementById('dlCardMeta');
    const dlFill = document.getElementById('dlCardFill');
    const dlPct = document.getElementById('dlCardPct');
    const btnDlPlay = document.getElementById('btnDlCardPlay');
    const btnDlClose = document.getElementById('btnDismissDlCard');

    // State
    let selectedMode = localStorage.getItem('snaptik_selected_mode') || '1080p';
    let activeResolvedData = null;
    let activeCarouselIdx = 0;
    let activeLibraryFilter = 'all';
    let activeLibrarySearch = '';
    let detectedClipboardUrl = '';

    // Download Card State
    let activeDlFilename = '';
    let activeDlQualityTag = '1080p FHD';
    let activeDlAutoClose = null;
    let activeDlSimTimer = null;
    let batchTotal = 0;
    let batchDone = 0;

    // Utilities
    function triggerHaptics(ms = 20) {
        if (!prefHapticFeedback || !prefHapticFeedback.checked) return;
        if (window.AndroidApp && typeof window.AndroidApp.vibrate === 'function') {
            window.AndroidApp.vibrate(ms);
        } else if (navigator.vibrate) {
            navigator.vibrate(ms);
        }
    }

    let toastTimer = null;
    function showHudToast(msg) {
        const toast = document.getElementById('monoToastHud');
        const text = document.getElementById('monoToastText');
        if (!toast || !text) return;
        text.textContent = msg;
        toast.classList.add('show');
        if (toastTimer) clearTimeout(toastTimer);
        toastTimer = setTimeout(() => toast.classList.remove('show'), 2500);
    }

    function formatDuration(sec) {
        const s = parseInt(sec, 10);
        if (!s || s <= 0) return 'HD';
        const m = Math.floor(s / 60);
        const rem = s % 60;
        return `${m}:${rem < 10 ? '0' : ''}${rem}`;
    }

    function makeFileName(data, suffix, ext) {
        const creator = (data && data.author && data.author.unique_id)
            ? data.author.unique_id.replace(/[^a-zA-Z0-9_-]/g, '') : 'tiktok';
        const id = (data && data.id) ? data.id : Date.now();
        return `SnapTik_@${creator}_${id}_${suffix}.${ext}`;
    }

    function extractTikTokUrl(raw) {
        if (!raw) return null;
        const m = raw.match(/https?:\/\/(?:[a-zA-Z0-9-]+\.)?(?:tiktok|douyin)\.com\/[^\s"'<>]+/i);
        return m ? m[0] : null;
    }

    // Quick Guide Modal
    function openHowToModal() {
        triggerHaptics(18);
        if (modalHowToGuide) modalHowToGuide.style.display = 'flex';
    }
    function closeHowToModal() {
        triggerHaptics(15);
        if (modalHowToGuide) modalHowToGuide.style.display = 'none';
    }
    if (btnOpenHowTo) btnOpenHowTo.addEventListener('click', openHowToModal);
    if (btnSettingsHowTo) btnSettingsHowTo.addEventListener('click', openHowToModal);
    if (btnCloseHowTo) btnCloseHowTo.addEventListener('click', closeHowToModal);
    if (btnGotItHowTo) btnGotItHowTo.addEventListener('click', closeHowToModal);

    // Navigation Dock
    const navButtons = document.querySelectorAll('.dock-nav-item');
    const sections = document.querySelectorAll('.section-view');

    function switchView(targetId) {
        triggerHaptics(18);
        navButtons.forEach(b => b.classList.toggle('active', b.dataset.view === targetId));
        sections.forEach(s => s.classList.toggle('active', s.id === targetId));
        if (targetId === 'sectionLibrary') renderLibraryList();
        if (targetId === 'sectionSettings') pingServerStatus();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    navButtons.forEach(btn => {
        btn.addEventListener('click', () => switchView(btn.dataset.view));
    });

    if (btnSeeAllRecents) {
        btnSeeAllRecents.addEventListener('click', () => switchView('sectionLibrary'));
    }

    // Quality Capsule & Default Quality Preference Sync
    const qualityBtns = document.querySelectorAll('.quality-seg-btn');
    const qualityMetaMap = {
        '1080p': { pill: '1080p HD', desc: '1080p Full HD • Sin marca de agua' },
        '4k': { pill: '4K UHD', desc: '4K Ultra HD • Máxima calidad original' },
        '720p': { pill: '720p', desc: '720p HD • Descarga rápida y ligera' },
        'audio': { pill: 'Audio MP3', desc: 'Audio MP3 • 320 kbps original' }
    };
    const qualityOrder = ['1080p', '4k', '720p', 'audio'];

    function highlightActiveBentoTile(mode) {
        const map = { '1080p': btnMpHd, '4k': btn4k, '720p': btnMp4, 'audio': btnMp3 };
        [btnMpHd, btn4k, btnMp4, btnMp3].forEach(tile => {
            if (!tile) return;
            tile.classList.toggle('hero-pearl', tile === map[mode]);
        });
    }

    function syncSelectedModeUI(mode) {
        if (!qualityMetaMap[mode]) mode = '1080p';
        selectedMode = mode;
        localStorage.setItem('snaptik_selected_mode', mode);
        qualityBtns.forEach(b => b.classList.toggle('active', b.dataset.mode === mode));
        if (btnCycleDefaultQuality) btnCycleDefaultQuality.textContent = qualityMetaMap[mode].pill;
        if (lblDefaultQualitySub) lblDefaultQualitySub.textContent = qualityMetaMap[mode].desc;
        highlightActiveBentoTile(mode);
        if (activeResolvedData) {
            activeResolvedData.chosen_mode = mode;
            const isPhoto = Array.isArray(activeResolvedData.images) && activeResolvedData.images.length > 0;
            if (!isPhoto) {
                const modeLabels = { '4k': '4K Ultra HD', '1080p': '1080p Full HD', '720p': '720p HD', 'audio': 'Audio MP3 320k' };
                const t = modeLabels[mode] || '1080p Full HD';
                if (badgeDetectedRes) badgeDetectedRes.textContent = t;
                if (chipRealResolution) chipRealResolution.textContent = t;
            }
            saveItemToLocalLibrary(activeResolvedData);
            renderRecentsStrip();
        }
    }

    qualityBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            triggerHaptics(18);
            syncSelectedModeUI(btn.dataset.mode);
        });
    });

    function cycleDefaultQuality() {
        triggerHaptics(20);
        const idx = qualityOrder.indexOf(selectedMode);
        const nextMode = qualityOrder[(idx + 1) % qualityOrder.length];
        syncSelectedModeUI(nextMode);
        showHudToast(`Calidad predeterminada: ${qualityMetaMap[nextMode].pill}`);
    }

    if (rowDefaultQuality) rowDefaultQuality.addEventListener('click', cycleDefaultQuality);
    syncSelectedModeUI(selectedMode);

    // Preferences Persistence
    if (prefAutoClipboard) {
        const savedAutoClip = localStorage.getItem('snaptik_pref_autoclip');
        if (savedAutoClip !== null) prefAutoClipboard.checked = savedAutoClip === 'true';
        prefAutoClipboard.addEventListener('change', () => {
            triggerHaptics(18);
            localStorage.setItem('snaptik_pref_autoclip', String(prefAutoClipboard.checked));
            showHudToast(prefAutoClipboard.checked ? 'Pegado automático activado' : 'Pegado automático desactivado');
        });
    }

    if (prefHapticFeedback) {
        const savedHaptic = localStorage.getItem('snaptik_pref_haptic');
        if (savedHaptic !== null) prefHapticFeedback.checked = savedHaptic === 'true';
        prefHapticFeedback.addEventListener('change', () => {
            localStorage.setItem('snaptik_pref_haptic', String(prefHapticFeedback.checked));
            if (prefHapticFeedback.checked) triggerHaptics(25);
            showHudToast(prefHapticFeedback.checked ? 'Vibración táctil activada' : 'Vibración táctil desactivada');
        });
    }

    // Input & Clipboard Logic
    function updateClearBtn() {
        btnClearInput.style.display = urlInput.value.trim().length > 0 ? 'flex' : 'none';
    }
    urlInput.addEventListener('input', () => {
        updateClearBtn();
        hideError();
    });
    btnClearInput.addEventListener('click', () => {
        triggerHaptics(15);
        urlInput.value = '';
        updateClearBtn();
        hideError();
    });

    async function readClipboardAndFill(autoExtract) {
        triggerHaptics(22);
        let text = '';
        try {
            if (window.AndroidApp && typeof window.AndroidApp.getClipboard === 'function') {
                text = window.AndroidApp.getClipboard() || '';
            } else if (navigator.clipboard && navigator.clipboard.readText) {
                text = await navigator.clipboard.readText();
            }
        } catch (e) {}

        const found = extractTikTokUrl(text);
        if (found) {
            urlInput.value = found;
            updateClearBtn();
            hideError();
            if (autoExtract) {
                startExtraction(found);
            } else {
                showHudToast('Enlace pegado');
            }
        } else if (text && text.trim().length > 0) {
            urlInput.value = text.trim();
            updateClearBtn();
            showHudToast('Texto pegado');
        } else {
            showHudToast('Copia primero un enlace de TikTok');
        }
    }

    btnPaste.addEventListener('click', () => readClipboardAndFill(false));
    if (btnClipboardChip) {
        btnClipboardChip.addEventListener('click', () => {
            if (detectedClipboardUrl) {
                urlInput.value = detectedClipboardUrl;
                updateClearBtn();
                startExtraction(detectedClipboardUrl);
                detectedClipboardUrl = '';
            } else {
                readClipboardAndFill(true);
            }
        });
    }

    // Extraction Flow
    function setLoading(loading) {
        btnSubmit.disabled = loading;
        ctaLabel.textContent = loading ? 'Obteniendo vídeo...' : 'Obtener Vídeo Ahora';
        ctaSpinner.style.display = loading ? 'inline-block' : 'none';
    }

    function showError(msg) {
        errorText.textContent = msg;
        errorBox.style.display = 'flex';
        triggerHaptics(45);
    }

    function hideError() {
        errorBox.style.display = 'none';
        errorText.textContent = '';
    }

    function startExtraction(rawUrl) {
        const clean = extractTikTokUrl(rawUrl || urlInput.value.trim());
        if (!clean) {
            showError('Pega un enlace válido de TikTok (tiktok.com).');
            return;
        }
        hideError();
        setLoading(true);
        triggerHaptics(30);

        if (window.AndroidApp && typeof window.AndroidApp.extract === 'function') {
            window.AndroidApp.extract(clean);
        } else {
            fetch('https://snaptik-backend.onrender.com/api/extract', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ url: clean })
            })
            .then(r => r.json())
            .then(json => handleExtractionPayload(json))
            .catch(() => {
                setLoading(false);
                showError('No se pudo conectar con el servicio. Verifica tu conexión.');
            });
        }
    }

    btnSubmit.addEventListener('click', () => startExtraction(urlInput.value));

    window.onExtractionCompleteBase64 = function(b64) {
        try {
            const binary = atob(b64);
            const bytes = new Uint8Array(binary.length);
            for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
            const decoded = new TextDecoder('utf-8').decode(bytes);
            handleExtractionPayload(JSON.parse(decoded));
        } catch (e) {
            setLoading(false);
            showError('Error al procesar el vídeo.');
        }
    };

    window.onExtractionComplete = function(jsonStr) {
        try {
            const parsed = typeof jsonStr === 'string' ? JSON.parse(jsonStr) : jsonStr;
            handleExtractionPayload(parsed);
        } catch (e) {
            setLoading(false);
            showError('Error procesando el vídeo.');
        }
    };

    window.onSharedLinkReceived = function(sharedText) {
        const found = extractTikTokUrl(sharedText);
        if (found) {
            switchView('sectionDownload');
            urlInput.value = found;
            updateClearBtn();
            startExtraction(found);
        }
    };

    window.onAutoPasteDetected = function(url) {
        if (!prefAutoClipboard || !prefAutoClipboard.checked) return;
        const found = extractTikTokUrl(url);
        if (!found) return;
        detectedClipboardUrl = found;
        if (!urlInput.value.trim()) {
            urlInput.value = found;
            updateClearBtn();
            showHudToast('Enlace de TikTok detectado');
            triggerHaptics(20);
        }
    };

    function handleExtractionPayload(payload) {
        setLoading(false);
        if (!payload || !payload.success || !payload.data) {
            showError((payload && (payload.error || payload.detail)) || 'No se pudo obtener este vídeo.');
            return;
        }
        renderResultDeck(payload.data);
    }

    // Render Result Deck (Preview + 2x2 Bento Grid)
    function renderResultDeck(data) {
        activeResolvedData = data;
        const isPhotoAlbum = Array.isArray(data.images) && data.images.length > 0;

        // Creator Overlay
        const avatar = (data.author && data.author.avatar && data.author.avatar.length > 4)
            ? data.author.avatar : defaultCreatorSvg;
        const handle = (data.author && data.author.unique_id)
            ? '@' + data.author.unique_id : (data.author && data.author.nickname ? data.author.nickname : '@creador');
        resAuthorAvatar.src = avatar;
        resAuthorAvatar.onerror = () => { resAuthorAvatar.src = defaultCreatorSvg; };
        resAuthorHandle.textContent = handle + ' ✓';

        // Ambient Blurred Backdrop
        const ambientCover = data.cover_url || data.raw_cover_url || (isPhotoAlbum && data.images[0]) || '';
        if (previewAmbientBg) {
            previewAmbientBg.style.backgroundImage = ambientCover ? `url("${ambientCover}")` : 'none';
        }

        // Caption & Initial Metadata Chips
        if (resVideoCaption) {
            resVideoCaption.textContent = (data.title && data.title.trim().length > 0)
                ? data.title : `Vídeo sin marca de agua de ${handle}`;
        }

        const modeLabels = { '4k': '4K Ultra HD', '1080p': '1080p Full HD', '720p': '720p HD', 'audio': 'Audio MP3 320k' };
        const initResLabel = isPhotoAlbum
            ? `${data.images.length} Fotos • HD`
            : (modeLabels[selectedMode] || (data.is_4k ? '4K Ultra HD' : '1080p Full HD'));
        if (badgeDetectedRes) badgeDetectedRes.textContent = initResLabel;
        if (chipRealResolution) chipRealResolution.textContent = isPhotoAlbum ? `Álbum • ${data.images.length} fotos` : initResLabel;
        if (chipAspectRatio) chipAspectRatio.textContent = isPhotoAlbum ? 'Carrusel HD' : 'Formato original';
        if (chipRealDuration) {
            if (!isPhotoAlbum && data.duration > 0) {
                chipRealDuration.textContent = formatDuration(data.duration);
                chipRealDuration.style.display = 'inline-flex';
            } else {
                chipRealDuration.style.display = 'none';
            }
        }

        // Media Preview Stage
        if (isPhotoAlbum) {
            videoPlayer.pause();
            videoPlayer.src = '';
            videoPlayer.style.display = 'none';
            if (btnFullscreenVideo) btnFullscreenVideo.style.display = 'none';
            photoStage.style.display = 'flex';
            if (previewStageWrapper) previewStageWrapper.style.aspectRatio = 'auto';
            activeCarouselIdx = 0;
            renderCarouselPhoto();
            if (data.audio_url) {
                carouselAudio.src = data.audio_url;
            }
        } else {
            photoStage.style.display = 'none';
            carouselAudio.pause();
            videoPlayer.style.display = 'block';
            if (btnFullscreenVideo) btnFullscreenVideo.style.display = 'flex';
            const primaryStream = data.preview_url || data.video_url || data.hd_url;
            const backupStream = data.video_url || data.hd_url;
            videoPlayer.poster = ambientCover;

            videoPlayer.onloadedmetadata = () => {
                const vw = videoPlayer.videoWidth || data.width || 0;
                const vh = videoPlayer.videoHeight || data.height || 0;
                const realDur = (videoPlayer.duration && isFinite(videoPlayer.duration))
                    ? Math.round(videoPlayer.duration) : (data.duration || 0);

                if (vw > 0 && vh > 0) {
                    const ratio = vw / vh;
                    const maxDim = Math.max(vw, vh);
                    const minDim = Math.min(vw, vh);

                    if (previewStageWrapper) {
                        previewStageWrapper.style.aspectRatio = 'auto';
                        if (ratio < 0.85) {
                            videoPlayer.style.maxHeight = '62vh';
                        } else if (ratio > 1.15) {
                            videoPlayer.style.maxHeight = '48vh';
                        } else {
                            videoPlayer.style.maxHeight = '54vh';
                        }
                    }

                    let tier = '1080p Full HD';
                    if (selectedMode === '4k') tier = '4K Ultra HD';
                    else if (selectedMode === '1080p') tier = '1080p Full HD';
                    else if (selectedMode === '720p') tier = '720p HD';
                    else if (selectedMode === 'audio') tier = 'Audio MP3 320k';
                    else if (maxDim >= 2560 || minDim >= 1440) tier = '4K Ultra HD';
                    else if (maxDim >= 1800 || minDim >= 1000) tier = '1080p Full HD';
                    else if (maxDim >= 1200 || minDim >= 700) tier = '720p HD';

                    const orientLabel = ratio < 0.85 ? 'Vertical 9:16' : (ratio > 1.15 ? 'Horizontal 16:9' : 'Cuadrado 1:1');
                    const durStr = realDur > 0 ? formatDuration(realDur) : '';

                    if (badgeDetectedRes) badgeDetectedRes.textContent = tier;
                    if (chipRealResolution) chipRealResolution.textContent = tier;
                    if (chipAspectRatio) chipAspectRatio.textContent = orientLabel;
                    if (chipRealDuration && durStr) {
                        chipRealDuration.textContent = durStr;
                        chipRealDuration.style.display = 'inline-flex';
                    }

                    // Auto-upgrade cover if current thumbnail is a low-res avatar
                    autoUpgradeHistoryCovers();
                }
            };

            videoPlayer.onerror = () => {
                if (backupStream && videoPlayer.src !== backupStream) {
                    videoPlayer.src = backupStream;
                } else {
                    videoPlayer.onerror = null;
                }
            };
            videoPlayer.src = primaryStream;
        }

        // Configure 2x2 Bento Tiles
        const hdUrl = data.hd_url || data.video_url;
        const uhdUrl = data.uhd_4k_url || data.hd_url || data.video_url;
        const stdUrl = data.video_url || data.sd_url;
        const audioUrl = (data.audio_url && data.audio_url.length > 5) ? data.audio_url : (!isPhotoAlbum ? data.video_url : '');

        if (isPhotoAlbum) {
            btnMpHd.style.display = 'none';
            btn4k.style.display = 'none';
            btnMp4.style.display = 'none';

            btnPhotos.style.display = 'flex';
            lblPhotosHeadline.textContent = `Guardar Álbum (${data.images.length} Fotos)`;
            btnPhotos.onclick = () => {
                triggerHaptics(35);
                startAlbumBatchDownload(data);
            };

            btnCurrentPhoto.style.display = 'flex';
            btnCurrentPhoto.onclick = () => {
                triggerHaptics(30);
                const b64List = data.images_preview || [];
                const idx = activeCarouselIdx || 0;
                const target = (b64List[idx] && b64List[idx].startsWith('data:')) ? b64List[idx] : data.images[idx];
                const fname = makeFileName(data, `foto_${idx + 1}`, 'jpg');
                triggerDownloadWithCard(target, fname, `Foto #${idx + 1}`, data);
            };
        } else {
            btnPhotos.style.display = 'none';
            btnCurrentPhoto.style.display = 'none';

            if (hdUrl) {
                btnMpHd.style.display = 'flex';
                lbl1080pHeadline.textContent = 'Full HD 1080p';
                lbl1080pSubhead.textContent = (data.hd_size_mb ? data.hd_size_mb + ' • ' : '') + 'Sin marca';
                btnMpHd.onclick = () => {
                    triggerHaptics(35);
                    syncSelectedModeUI('1080p');
                    data.chosen_mode = '1080p';
                    saveItemToLocalLibrary(data);
                    renderRecentsStrip();
                    const fname = makeFileName(data, '1080p_FHD', 'mp4');
                    triggerRewardedDownload(hdUrl, fname, '1080p FHD', data);
                };
            } else {
                btnMpHd.style.display = 'none';
            }

            if (uhdUrl) {
                btn4k.style.display = 'flex';
                lbl4kSubhead.textContent = data.hd_size_mb ? `${data.hd_size_mb} • Original` : 'Calidad original';
                btn4k.onclick = () => {
                    triggerHaptics(35);
                    syncSelectedModeUI('4k');
                    data.chosen_mode = '4k';
                    saveItemToLocalLibrary(data);
                    renderRecentsStrip();
                    const fname = makeFileName(data, '4K_UHD', 'mp4');
                    triggerRewardedDownload(uhdUrl, fname, '4K UHD', data);
                };
            } else {
                btn4k.style.display = 'none';
            }

            if (stdUrl) {
                btnMp4.style.display = 'flex';
                lbl720pSubhead.textContent = (data.size_mb ? data.size_mb + ' • ' : '') + 'Descarga rápida';
                btnMp4.onclick = () => {
                    triggerHaptics(35);
                    syncSelectedModeUI('720p');
                    data.chosen_mode = '720p';
                    saveItemToLocalLibrary(data);
                    renderRecentsStrip();
                    const fname = makeFileName(data, '720p_HD', 'mp4');
                    triggerDownloadWithCard(stdUrl, fname, '720p HD', data);
                };
            } else {
                btnMp4.style.display = 'none';
            }
        }

        if (audioUrl) {
            btnMp3.style.display = 'flex';
            lblMp3Headline.textContent = 'Audio MP3';
            lblMp3Subhead.textContent = '320 kbps original';
            btnMp3.onclick = () => {
                triggerHaptics(35);
                if (!isPhotoAlbum) {
                    syncSelectedModeUI('audio');
                    data.chosen_mode = 'audio';
                    saveItemToLocalLibrary(data);
                    renderRecentsStrip();
                }
                const fname = makeFileName(data, 'AUDIO_320K', 'mp3');
                triggerDownloadWithCard(audioUrl, fname, 'MP3 320k', data);
            };
        } else {
            btnMp3.style.display = 'none';
        }

        data.chosen_mode = selectedMode;
        highlightActiveBentoTile(selectedMode);
        saveItemToLocalLibrary(data);
        renderRecentsStrip();
        autoUpgradeHistoryCovers();
        resultDeck.style.display = 'block';
        resultDeck.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    function renderCarouselPhoto() {
        if (!activeResolvedData || !activeResolvedData.images) return;
        const list = activeResolvedData.images_preview || activeResolvedData.images;
        const total = activeResolvedData.images.length;
        carouselImg.src = list[activeCarouselIdx] || activeResolvedData.images[activeCarouselIdx];
        carouselCounter.textContent = `${activeCarouselIdx + 1} / ${total}`;
        lblSinglePhotoHeadline.textContent = `Foto #${activeCarouselIdx + 1}`;
    }

    btnPrevPhoto.addEventListener('click', () => {
        if (!activeResolvedData || !activeResolvedData.images) return;
        triggerHaptics(15);
        activeCarouselIdx = (activeCarouselIdx - 1 + activeResolvedData.images.length) % activeResolvedData.images.length;
        renderCarouselPhoto();
    });

    btnNextPhoto.addEventListener('click', () => {
        if (!activeResolvedData || !activeResolvedData.images) return;
        triggerHaptics(15);
        activeCarouselIdx = (activeCarouselIdx + 1) % activeResolvedData.images.length;
        renderCarouselPhoto();
    });

    if (btnFullscreenVideo) {
        btnFullscreenVideo.addEventListener('click', () => {
            triggerHaptics(20);
            try {
                if (videoPlayer.requestFullscreen) {
                    videoPlayer.requestFullscreen();
                } else if (videoPlayer.webkitEnterFullscreen) {
                    videoPlayer.webkitEnterFullscreen();
                } else if (previewStageWrapper && previewStageWrapper.requestFullscreen) {
                    previewStageWrapper.requestFullscreen();
                }
            } catch (e) {}
        });
    }

    btnDismissDeck.addEventListener('click', () => {
        triggerHaptics(18);
        resultDeck.style.display = 'none';
        videoPlayer.pause();
        videoPlayer.src = '';
        carouselAudio.pause();
        urlInput.value = '';
        updateClearBtn();
    });

    // Quick Tools
    btnCover.addEventListener('click', () => {
        if (!activeResolvedData) return;
        const coverSrc = activeResolvedData.cover_url || activeResolvedData.raw_cover_url || (activeResolvedData.images && activeResolvedData.images[0]);
        if (!coverSrc) return;
        const fname = makeFileName(activeResolvedData, 'PORTADA_HD', 'jpg');
        triggerDownloadWithCard(coverSrc, fname, 'Portada HD', activeResolvedData);
    });

    btnCopyDesc.addEventListener('click', async () => {
        if (!activeResolvedData || !activeResolvedData.title) return;
        triggerHaptics(18);
        try {
            await navigator.clipboard.writeText(activeResolvedData.title);
            showHudToast('Descripción copiada');
        } catch (e) { showHudToast('Texto copiado'); }
    });

    btnShare.addEventListener('click', () => {
        if (!activeResolvedData) return;
        triggerHaptics(20);
        const shareUrl = activeResolvedData.video_url || (activeResolvedData.images && activeResolvedData.images[0]) || activeResolvedData.source_url;
        if (window.AndroidApp && window.AndroidApp.shareText) {
            window.AndroidApp.shareText(shareUrl, 'Compartir vídeo');
        } else if (navigator.share) {
            navigator.share({ title: activeResolvedData.title, url: shareUrl });
        }
    });

    btnCopyLink.addEventListener('click', async () => {
        if (!activeResolvedData) return;
        const link = activeResolvedData.hd_url || activeResolvedData.video_url || activeResolvedData.source_url;
        if (!link) return;
        triggerHaptics(18);
        try {
            await navigator.clipboard.writeText(link);
            showHudToast('Enlace directo copiado');
        } catch (e) {}
    });

    // =========================================================
    // DYNAMIC ISLAND DOWNLOAD CARD CONTROLLER
    // =========================================================
    function showDynamicCard(fname, qualityTag, itemData, isBatch, totalCount) {
        if (activeDlAutoClose) clearTimeout(activeDlAutoClose);
        if (activeDlSimTimer) clearInterval(activeDlSimTimer);

        activeDlFilename = fname;
        activeDlQualityTag = qualityTag || '1080p FHD';
        batchTotal = isBatch ? totalCount : 0;
        batchDone = 0;

        const cover = (itemData && (itemData.cover_url || itemData.raw_cover_url || (itemData.images && itemData.images[0]) || (itemData.author && itemData.author.avatar))) || defaultCreatorSvg;
        dlThumb.src = cover;
        dlThumb.onerror = () => { dlThumb.src = defaultCreatorSvg; };

        dlTitle.textContent = isBatch ? `Guardando álbum (0/${totalCount})...` : 'Guardando en tu galería...';
        dlMeta.textContent = `${activeDlQualityTag} • Conectando...`;
        dlFill.classList.remove('done');
        dlPct.classList.remove('done');
        dlFill.style.width = '8%';
        dlPct.textContent = '8%';
        dlSpinner.style.display = 'block';
        dlCheck.style.display = 'none';
        btnDlPlay.style.display = 'none';

        dlCard.classList.add('show');

        if (!isBatch) {
            let sim = 8;
            activeDlSimTimer = setInterval(() => {
                if (sim < 88) {
                    sim += Math.floor(Math.random() * 10) + 6;
                    if (sim > 88) sim = 88;
                    dlFill.style.width = sim + '%';
                    dlPct.textContent = sim + '%';
                }
            }, 220);
        }
    }

    function triggerDownloadWithCard(url, fname, qualityTag, itemData) {
        if (itemData) saveItemToLocalLibrary(itemData);
        showDynamicCard(fname, qualityTag, itemData, false, 0);

        if (window.AndroidApp && typeof window.AndroidApp.download === 'function') {
            window.AndroidApp.download(url, fname);
        } else {
            window.open(url, '_blank');
            setTimeout(() => onDownloadDoneUI(fname, '16.4 MB/s'), 1400);
        }
    }

    function startAlbumBatchDownload(data) {
        saveItemToLocalLibrary(data);
        const total = data.images.length;
        const b64List = data.images_preview || [];
        const firstSrc = (b64List[0] && b64List[0].startsWith('data:')) ? b64List[0] : data.images[0];
        const firstFname = makeFileName(data, 'foto_1', 'jpg');

        showDynamicCard(firstFname, `${total} Fotos`, data, true, total);

        if (window.AndroidApp && typeof window.AndroidApp.download === 'function') {
            window.AndroidApp.download(firstSrc, firstFname);
            data.images.slice(1).forEach((imgUrl, idx) => {
                setTimeout(() => {
                    const f = makeFileName(data, `foto_${idx + 2}`, 'jpg');
                    const src = (b64List[idx + 1] && b64List[idx + 1].startsWith('data:')) ? b64List[idx + 1] : imgUrl;
                    if (typeof window.AndroidApp.downloadQuiet === 'function') {
                        window.AndroidApp.downloadQuiet(src, f);
                    } else {
                        window.AndroidApp.download(src, f);
                    }
                }, (idx + 1) * 180);
            });
        }
    }

    function onDownloadDoneUI(fname, speedLabel) {
        if (activeDlSimTimer) {
            clearInterval(activeDlSimTimer);
            activeDlSimTimer = null;
        }
        dlTitle.textContent = 'Guardado en Galería';
        dlMeta.textContent = `${activeDlQualityTag}${speedLabel ? ' • ' + speedLabel : ''}`;
        dlFill.style.width = '100%';
        dlFill.classList.add('done');
        dlPct.textContent = '100%';
        dlPct.classList.add('done');
        dlSpinner.style.display = 'none';
        dlCheck.style.display = 'flex';
        btnDlPlay.style.display = 'inline-flex';
        triggerHaptics(35);

        if (activeDlAutoClose) clearTimeout(activeDlAutoClose);
        activeDlAutoClose = setTimeout(() => {
            dlCard.classList.remove('show');
        }, 12000);
    }

    window.onFastDownloadProgress = function(fname, pct, downloaded, total, speedLabel) {
        if (!dlCard.classList.contains('show') || batchTotal > 0) return;
        if (activeDlSimTimer) {
            clearInterval(activeDlSimTimer);
            activeDlSimTimer = null;
        }
        const p = Math.min(100, Math.max(0, pct));
        dlFill.style.width = p + '%';
        dlPct.textContent = p + '%';
        dlMeta.textContent = `${activeDlQualityTag}${speedLabel ? ' • ' + speedLabel : ''}`;
    };

    window.onFastDownloadComplete = function(fname, fpath, isVideo, speedLabel) {
        if (batchTotal > 0) {
            batchDone++;
            const pct = Math.min(100, Math.round((batchDone / batchTotal) * 100));
            dlFill.style.width = pct + '%';
            dlPct.textContent = pct + '%';
            dlTitle.textContent = `Guardando álbum (${batchDone}/${batchTotal})`;
            if (batchDone >= batchTotal) {
                batchTotal = 0;
                onDownloadDoneUI(fname, speedLabel || 'Álbum completo');
            }
            return;
        }
        onDownloadDoneUI(fname, speedLabel);
    };

    window.onFastDownloadFallback = function(fname) {
        if (activeDlSimTimer) {
            clearInterval(activeDlSimTimer);
            activeDlSimTimer = null;
        }
        dlTitle.textContent = 'Guardando en Descargas';
        dlMeta.textContent = `${activeDlQualityTag} • En segundo plano`;
        dlFill.style.width = '100%';
        dlPct.textContent = '✓';
        dlSpinner.style.display = 'none';
        dlCheck.style.display = 'flex';
        if (activeDlAutoClose) clearTimeout(activeDlAutoClose);
        activeDlAutoClose = setTimeout(() => dlCard.classList.remove('show'), 5000);
    };

    btnDlPlay.addEventListener('click', () => {
        triggerHaptics(25);
        if (window.AndroidApp && typeof window.AndroidApp.openDownloadedFile === 'function') {
            window.AndroidApp.openDownloadedFile(activeDlFilename);
        } else if (window.AndroidApp && typeof window.AndroidApp.openDownloadsFolder === 'function') {
            window.AndroidApp.openDownloadsFolder();
        }
    });

    btnDlClose.addEventListener('click', () => {
        triggerHaptics(15);
        dlCard.classList.remove('show');
        if (activeDlAutoClose) clearTimeout(activeDlAutoClose);
    });

    // =========================================================
    // LIBRARY & RECENTS MANAGEMENT
    // =========================================================
    function getHistory() {
        try {
            const raw = localStorage.getItem('snaptik_pro_history');
            return raw ? JSON.parse(raw) : [];
        } catch (e) { return []; }
    }

    function saveItemToLocalLibrary(item) {
        if (!item) return;
        let list = getHistory();
        const urlKey = item.source_url || '';
        const authorId = (item.author && item.author.unique_id) ? item.author.unique_id : '';
        list = list.filter(x => {
            if (x.id && item.id && String(x.id) === String(item.id)) return false;
            if (urlKey && x.source_url && x.source_url === urlKey) return false;
            if (authorId && item.title && x.author && x.author.unique_id === authorId && x.title === item.title) return false;
            return true;
        });
        list.unshift(item);
        if (list.length > 30) list.pop();
        try { localStorage.setItem('snaptik_pro_history', JSON.stringify(list)); } catch (e) {}
    }

    function getBestCoverUrl(item) {
        if (!item) return defaultCreatorSvg;
        if (item.cover_url && item.cover_url.startsWith('data:')) return item.cover_url;
        if (item.raw_cover_url && item.raw_cover_url.length > 6) return item.raw_cover_url;
        if (item.cover_url && item.cover_url.length > 6) return item.cover_url;
        if (item.images_preview && item.images_preview[0]) return item.images_preview[0];
        if (item.images && item.images[0]) return item.images[0];
        if (item.author && item.author.avatar) return item.author.avatar;
        return defaultCreatorSvg;
    }

    function isLowResAvatarCover(item, thumb) {
        if (!thumb || thumb === defaultCreatorSvg) return true;
        if (thumb.startsWith('data:') && item.raw_cover_url && !item.raw_cover_url.includes('100x100') && !item.raw_cover_url.includes('avt-')) {
            return false;
        }
        const avatar = (item.author && item.author.avatar) ? item.author.avatar : '';
        if (avatar && thumb === avatar && !(item.raw_cover_url && item.raw_cover_url !== avatar)) return true;
        if (thumb.includes('100x100') || thumb.includes('avt-')) return true;
        return false;
    }

    const upgradingCoverIds = new Set();
    function autoUpgradeHistoryCovers() {
        const list = getHistory();
        let changed = false;
        list.forEach(item => {
            const thumb = getBestCoverUrl(item);
            if (!isLowResAvatarCover(item, thumb)) return;
            const srcUrl = item.source_url || (item.id && /^\d{15,}$/.test(String(item.id)) ? `https://www.tiktok.com/@tiktok/video/${item.id}` : '');
            if (!srcUrl || upgradingCoverIds.has(srcUrl)) return;
            upgradingCoverIds.add(srcUrl);
            fetch(`https://www.tikwm.com/api/?url=${encodeURIComponent(srcUrl)}`)
                .then(r => r.json())
                .then(js => {
                    if (js && js.code === 0 && js.data) {
                        let hd = js.data.origin_cover || js.data.ai_dynamic_cover || js.data.cover || '';
                        if (hd.startsWith('//')) hd = 'https:' + hd;
                        else if (hd.startsWith('/')) hd = 'https://www.tikwm.com' + hd;
                        if (hd && !hd.includes('100x100')) {
                            const currentList = getHistory();
                            let updated = false;
                            currentList.forEach(x => {
                                if ((x.source_url && x.source_url === item.source_url) || (x.id && x.id === item.id)) {
                                    x.raw_cover_url = hd;
                                    x.cover_url = hd;
                                    if (js.data.duration && !x.duration) x.duration = js.data.duration;
                                    updated = true;
                                }
                            });
                            if (updated) {
                                try { localStorage.setItem('snaptik_pro_history', JSON.stringify(currentList)); } catch (e) {}
                                renderRecentsStrip();
                                renderLibraryList();
                            }
                        }
                    }
                })
                .catch(() => {});
        });
    }

    function getRealQualityTag(item, isPhoto) {
        if (isPhoto) return 'ÁLBUM';
        if (item.chosen_mode === '4k') return '4K';
        if (item.chosen_mode === '1080p') return '1080p';
        if (item.chosen_mode === '720p') return '720p';
        if (item.chosen_mode === 'audio') return 'MP3';
        if (item.is_4k) return '4K';
        const w = parseInt(item.width, 10) || 0;
        const h = parseInt(item.height, 10) || 0;
        const maxD = Math.max(w, h);
        const minD = Math.min(w, h);
        if (maxD >= 2560 || minD >= 1440) return '4K';
        if (maxD >= 1800 || minD >= 1000) return '1080p';
        if (maxD >= 1200 || minD >= 700) return '720p';
        if (minD > 0) return `${minD}p`;
        return item.hd_size_mb ? item.hd_size_mb : 'HD';
    }

    function getLeftMetaPill(item, isPhoto) {
        if (isPhoto) return `${item.images.length} fotos`;
        const s = parseInt(item.duration, 10);
        if (s && s > 0) return formatDuration(s);
        if (item.hd_size_mb) return item.hd_size_mb;
        if (item.size_mb) return item.size_mb;
        return 'MP4';
    }

    function openHistoryItem(item) {
        triggerHaptics(20);
        switchView('sectionDownload');
        const needsRefresh = (!item.video_url && (!item.images || item.images.length === 0));
        const refreshUrl = item.source_url || (item.id ? `https://www.tiktok.com/@tiktok/video/${item.id}` : null);
        if (needsRefresh && refreshUrl && window.AndroidApp) {
            setLoading(true);
            window.AndroidApp.extract(refreshUrl);
        } else {
            renderResultDeck(item);
        }
    }

    function renderRecentsStrip() {
        const list = getHistory().slice(0, 3);
        recentsTarget.innerHTML = '';
        if (list.length === 0) {
            recentsTarget.innerHTML = `<div class="recent-empty-card">Tus vídeos y álbumes descargados recientemente aparecerán aquí para acceso rápido.</div>`;
            return;
        }
        list.forEach(item => {
            const isPhoto = Array.isArray(item.images) && item.images.length > 0;
            const card = document.createElement('div');
            card.className = 'recent-thumb-card';
            const thumb = getBestCoverUrl(item);
            const lowRes = isLowResAvatarCover(item, thumb);
            const author = (item.author && item.author.nickname) ? item.author.nickname : 'TikTok';
            const qualTag = getRealQualityTag(item, isPhoto);
            const durPill = getLeftMetaPill(item, isPhoto);
            card.innerHTML = `
                <img class="recent-thumb-img ${lowRes ? 'lowres-blur' : ''}" src="${thumb}" alt="" onerror="this.onerror=null;this.src='${defaultCreatorSvg}'">
                ${lowRes ? `<img class="lib-avatar-center" style="width:42px;height:42px;" src="${thumb}" alt="" onerror="this.style.display='none'">` : ''}
                <div class="recent-play-circle">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor"><polygon points="6 4 20 12 6 20 6 4"/></svg>
                </div>
                <div class="recent-thumb-scrim">
                    <span class="recent-thumb-author">${author}</span>
                    <div class="recent-meta-row">
                        <span class="recent-badge-pill">${qualTag}</span>
                        <span class="recent-dur-text">${durPill}</span>
                    </div>
                </div>
            `;
            card.addEventListener('click', () => openHistoryItem(item));
            recentsTarget.appendChild(card);
        });
    }

    function renderLibraryList() {
        const all = getHistory();
        let estMb = 0;
        all.forEach(it => {
            const m = String(it.hd_size_mb || it.size_mb || '').match(/([\d.]+)/);
            estMb += m ? parseFloat(m[1]) : 4.2;
        });
        const totalMbStr = estMb >= 10 ? Math.round(estMb) + ' MB' : estMb.toFixed(1) + ' MB';
        if (storageRingLabel) storageRingLabel.textContent = totalMbStr;
        if (storageRingArc) {
            const pct = Math.min(1, all.length / 25);
            storageRingArc.style.strokeDashoffset = String(125.6 - (125.6 * pct));
        }

        const q = activeLibrarySearch.trim().toLowerCase();
        const filtered = all.filter(item => {
            const isPhoto = Array.isArray(item.images) && item.images.length > 0;
            if (activeLibraryFilter === 'photo' && !isPhoto) return false;
            if (activeLibraryFilter === 'video' && isPhoto) return false;
            if (activeLibraryFilter === 'audio' && !Boolean(item.audio_url)) return false;
            if (q.length > 0) {
                const nick = (item.author && item.author.nickname ? item.author.nickname : '').toLowerCase();
                const uid = (item.author && item.author.unique_id ? item.author.unique_id : '').toLowerCase();
                const title = (item.title ? item.title : '').toLowerCase();
                return nick.includes(q) || uid.includes(q) || title.includes(q);
            }
            return true;
        });

        libraryTarget.innerHTML = '';
        if (filtered.length === 0) {
            placeholderEmpty.style.display = 'flex';
            return;
        }
        placeholderEmpty.style.display = 'none';

        filtered.forEach(item => {
            const isPhoto = Array.isArray(item.images) && item.images.length > 0;
            const thumb = getBestCoverUrl(item);
            const lowRes = isLowResAvatarCover(item, thumb);
            const creator = (item.author && item.author.nickname) ? item.author.nickname : 'Creador';
            const leftPill = getLeftMetaPill(item, isPhoto);
            const qualTag = getRealQualityTag(item, isPhoto);

            const card = document.createElement('div');
            card.className = 'lib-media-card';
            card.innerHTML = `
                <img class="lib-media-thumb ${lowRes ? 'lowres-blur' : ''}" src="${thumb}" alt="" onerror="this.onerror=null;this.src='${defaultCreatorSvg}'">
                ${lowRes ? `<img class="lib-avatar-center" src="${thumb}" alt="" onerror="this.style.display='none'">` : ''}
                <div class="lib-media-overlay">
                    <div class="lib-card-top-row">
                        <div class="lib-media-share-dot" data-action="share" title="Compartir">
                            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/></svg>
                        </div>
                        <div class="lib-media-play-dot">
                            <svg width="11" height="11" viewBox="0 0 24 24" fill="currentColor"><polygon points="6 4 20 12 6 20 6 4"/></svg>
                        </div>
                    </div>
                    <div class="lib-media-bottom">
                        <div class="lib-media-creator">${creator}</div>
                        <div class="lib-media-badges">
                            <span class="lib-micro-pill">${leftPill}</span>
                            ${qualTag !== leftPill ? `<span class="lib-micro-pill">${qualTag}</span>` : ''}
                        </div>
                    </div>
                </div>
            `;
            card.addEventListener('click', (ev) => {
                const shareBtn = ev.target.closest('[data-action="share"]');
                if (shareBtn) {
                    ev.stopPropagation();
                    triggerHaptics(20);
                    const sUrl = item.source_url || item.video_url || '';
                    if (window.AndroidApp && window.AndroidApp.shareText) {
                        window.AndroidApp.shareText(sUrl, 'Compartir vídeo');
                    }
                    return;
                }
                openHistoryItem(item);
            });
            libraryTarget.appendChild(card);
        });
    }

    if (inputLibrarySearch) {
        inputLibrarySearch.addEventListener('input', () => {
            activeLibrarySearch = inputLibrarySearch.value;
            if (btnClearLibSearch) {
                btnClearLibSearch.style.display = activeLibrarySearch.trim().length > 0 ? 'flex' : 'none';
            }
            renderLibraryList();
        });
    }

    if (btnClearLibSearch) {
        btnClearLibSearch.addEventListener('click', () => {
            triggerHaptics(15);
            inputLibrarySearch.value = '';
            activeLibrarySearch = '';
            btnClearLibSearch.style.display = 'none';
            renderLibraryList();
        });
    }

    document.querySelectorAll('.filter-pill-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            triggerHaptics(15);
            activeLibraryFilter = btn.dataset.filter;
            document.querySelectorAll('.filter-pill-btn').forEach(b => b.classList.toggle('active', b === btn));
            renderLibraryList();
        });
    });

    function openDeviceGalleryFolder() {
        triggerHaptics(20);
        if (window.AndroidApp && window.AndroidApp.openDownloadsFolder) {
            window.AndroidApp.openDownloadsFolder();
        } else {
            showHudToast('Archivos guardados en la carpeta Descargas');
        }
    }

    if (btnOpenDownloads) btnOpenDownloads.addEventListener('click', openDeviceGalleryFolder);
    if (btnSettingsOpenFolder) btnSettingsOpenFolder.addEventListener('click', openDeviceGalleryFolder);

    btnClearLibrary.addEventListener('click', () => {
        triggerHaptics(30);
        localStorage.removeItem('snaptik_pro_history');
        renderLibraryList();
        renderRecentsStrip();
        showHudToast('Biblioteca vaciada');
    });

    // Consumer Settings Actions
    if (btnClearTempCache) {
        btnClearTempCache.addEventListener('click', () => {
            triggerHaptics(25);
            if (window.AndroidApp && typeof window.AndroidApp.clearTempCache === 'function') {
                window.AndroidApp.clearTempCache();
            }
            if (lblCacheStatusSub) lblCacheStatusSub.textContent = '0 MB • Caché limpia';
            showHudToast('Caché temporal liberada');
        });
    }

    if (btnShareApp) {
        btnShareApp.addEventListener('click', () => {
            triggerHaptics(20);
            const msg = 'Descarga vídeos de TikTok en 4K y 1080p sin marca de agua con SnapTik PRO: https://github.com/juanherrerasandoval70-eng/snaptik-app/releases/latest';
            if (window.AndroidApp && typeof window.AndroidApp.shareText === 'function') {
                window.AndroidApp.shareText(msg, 'Compartir SnapTik PRO');
            } else if (navigator.share) {
                navigator.share({ title: 'SnapTik PRO', text: msg });
            } else {
                showHudToast('Enlace de la app copiado');
            }
        });
    }

    if (btnOpenPrivacyPolicy) {
        btnOpenPrivacyPolicy.addEventListener('click', () => {
            triggerHaptics(20);
            const url = 'https://snaptik-backend.onrender.com/privacy';
            if (window.AndroidApp && typeof window.AndroidApp.openExternalUrl === 'function') {
                window.AndroidApp.openExternalUrl(url);
            } else {
                window.open(url, '_blank');
            }
        });
    }

    // =========================================================
    // MONETIZATION & NATIVE AD CONTROLLER
    // =========================================================
    let pendingRewardAction = null;
    function triggerRewardedDownload(url, fname, qualityTag, itemData) {
        if (window.AndroidApp && typeof window.AndroidApp.requestRewardedVideo === 'function') {
            pendingRewardAction = () => triggerDownloadWithCard(url, fname, qualityTag, itemData);
            window.AndroidApp.requestRewardedVideo();
        } else {
            triggerDownloadWithCard(url, fname, qualityTag, itemData);
        }
    }

    window.onAdsterraRewardUnlocked = function() {
        if (pendingRewardAction) {
            pendingRewardAction();
            pendingRewardAction = null;
        }
    };
    window.onUnityRewardUnlocked = window.onAdsterraRewardUnlocked;

    const modalMonoAd = document.getElementById('modalMonoAd');
    document.getElementById('btnCloseAdModal').addEventListener('click', () => { modalMonoAd.style.display = 'none'; });
    document.getElementById('btnOpenAdOffer').addEventListener('click', () => {
        modalMonoAd.style.display = 'none';
        if (window.AndroidApp && window.AndroidApp.openSponsorOffer) window.AndroidApp.openSponsorOffer('');
    });
    window.showMonochromeInterstitial = function() {
        if (modalMonoAd) modalMonoAd.style.display = 'flex';
    };

    const startIoNativeCard = document.getElementById('startIoNativeCard');
    window.onStartIoNativeAdLoaded = function(ad) {
        if (!startIoNativeCard || !ad) return;
        const iconEl = document.getElementById('nativeAdIcon');
        const titleEl = document.getElementById('nativeAdTitle');
        const descEl = document.getElementById('nativeAdDesc');
        const ctaEl = document.getElementById('nativeAdCta');
        if (iconEl) iconEl.src = ad.iconUrl || ad.imageUrl || defaultCreatorSvg;
        if (titleEl) titleEl.textContent = ad.title || 'Recomendado';
        if (descEl) descEl.textContent = ad.description || 'Contenido patrocinado';
        if (ctaEl) ctaEl.textContent = ad.cta || 'Abrir';
        startIoNativeCard.style.display = 'flex';
    };
    if (startIoNativeCard) {
        startIoNativeCard.addEventListener('click', () => {
            triggerHaptics(20);
            if (window.AndroidApp && typeof window.AndroidApp.clickNativeAd === 'function') {
                window.AndroidApp.clickNativeAd();
            }
        });
    }

    // =========================================================
    // CONNECTION STATUS & OTA UPDATE MANAGER
    // =========================================================
    function pingServerStatus() {
        const dot = document.getElementById('headerStatusDot');
        const headerTxt = document.getElementById('headerStatusText');
        fetch('https://snaptik-backend.onrender.com/api/health')
            .then(r => {
                if (r.ok) {
                    if (dot) dot.classList.remove('offline');
                    if (headerTxt) headerTxt.textContent = 'Listo';
                } else {
                    throw new Error('offline');
                }
            })
            .catch(() => {
                if (dot) dot.classList.remove('offline');
                if (headerTxt) headerTxt.textContent = 'Listo';
            });
    }

    const modalUpdate = document.getElementById('modalAppUpdate');
    const badgeNewVersion = document.getElementById('badgeNewVersion');
    const txtUpdateChangelog = document.getElementById('txtUpdateChangelog');
    const boxUpdateProgress = document.getElementById('boxUpdateProgress');
    const barUpdateProgress = document.getElementById('barUpdateProgress');
    const txtUpdatePercent = document.getElementById('txtUpdatePercent');
    const btnLaterUpdate = document.getElementById('btnLaterUpdate');
    const btnNowUpdate = document.getElementById('btnNowUpdate');
    let pendingUpdateData = null;

    const btnCheckUpdatesManual = document.getElementById('btnCheckUpdatesManual');
    if (btnCheckUpdatesManual) {
        btnCheckUpdatesManual.addEventListener('click', () => {
            triggerHaptics(25);
            showHudToast('Buscando actualizaciones...');
            if (window.AndroidApp && window.AndroidApp.checkForUpdates) {
                window.AndroidApp.checkForUpdates(true);
            } else {
                setTimeout(() => showHudToast('✓ Tienes la última versión instalada (v3.7.0)'), 700);
            }
        });
    }

    window.onUpdateAvailable = function(data) {
        pendingUpdateData = data;
        badgeNewVersion.textContent = data.newVersion || 'Nueva';
        txtUpdateChangelog.textContent = data.changelog || 'Mejoras de velocidad y estabilidad.';
        boxUpdateProgress.style.display = 'none';
        btnLaterUpdate.style.display = data.forceUpdate ? 'none' : 'block';
        modalUpdate.style.display = 'flex';
    };

    window.onUpdateCheckResult = function(data) {
        showHudToast(`✓ Tienes la última versión (${data.currentVersion || '3.7.0'})`);
    };

    window.onUpdateError = function(msg) {
        showHudToast(msg || 'Tienes la última versión disponible');
    };

    window.onUpdateDownloadProgress = function(pct) {
        boxUpdateProgress.style.display = 'block';
        barUpdateProgress.style.width = pct + '%';
        txtUpdatePercent.textContent = pct + '%';
    };

    window.onUpdateDownloadError = function() {
        boxUpdateProgress.style.display = 'none';
        showHudToast('Error al descargar la actualización');
    };

    btnLaterUpdate.addEventListener('click', () => { modalUpdate.style.display = 'none'; });
    btnNowUpdate.addEventListener('click', () => {
        if (!pendingUpdateData) return;
        boxUpdateProgress.style.display = 'block';
        if (window.AndroidApp && window.AndroidApp.installUpdate) {
            window.AndroidApp.installUpdate(pendingUpdateData.apkUrl, pendingUpdateData.newVersion);
        }
    });

    // Init
    renderRecentsStrip();
    autoUpgradeHistoryCovers();
    pingServerStatus();
});
