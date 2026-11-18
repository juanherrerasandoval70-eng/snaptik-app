document.addEventListener('DOMContentLoaded', () => {
    const extractForm = document.getElementById('extractForm');
    const urlInput = document.getElementById('urlInput');
    const pasteBtn = document.getElementById('pasteBtn');
    const clearBtn = document.getElementById('clearBtn');
    const submitBtn = document.getElementById('submitBtn');
    const launchText = submitBtn.querySelector('.launch-text');
    const launchLoader = submitBtn.querySelector('.launch-loader');
    const errorBox = document.getElementById('errorBox');
    const errorMessage = document.getElementById('errorMessage');

    const workstationSection = document.getElementById('workstationSection');
    const engineUsedBadge = document.getElementById('engineUsedBadge');
    const displayVideoId = document.getElementById('displayVideoId');
    const studioVideo = document.getElementById('studioVideo');
    const waveformVisual = document.getElementById('waveformVisual');
    const timecodeDisplay = document.getElementById('timecodeDisplay');
    const speedButtons = document.querySelectorAll('.speed-btn');

    const creatorAvatar = document.getElementById('creatorAvatar');
    const creatorNickname = document.getElementById('creatorNickname');
    const creatorHandle = document.getElementById('creatorHandle');
    const captionText = document.getElementById('captionText');
    const copyCaptionBtn = document.getElementById('copyCaptionBtn');

    const metricLikes = document.getElementById('metricLikes');
    const metricComments = document.getElementById('metricComments');
    const metricShares = document.getElementById('metricShares');
    const metricPlays = document.getElementById('metricPlays');

    const trackStrip = document.getElementById('trackStrip');
    const trackTitle = document.getElementById('trackTitle');
    const trackArtist = document.getElementById('trackArtist');

    const btnDownloadHD = document.getElementById('btnDownloadHD');
    const btnDownloadAudio = document.getElementById('btnDownloadAudio');
    const btnDownloadCover = document.getElementById('btnDownloadCover');
    const copyShareLinkBtn = document.getElementById('copyShareLinkBtn');
    const newExtractBtn = document.getElementById('newExtractBtn');

    const reelSection = document.getElementById('reelSection');
    const reelGrid = document.getElementById('reelGrid');
    const clearReelBtn = document.getElementById('clearReelBtn');

    let currentItem = null;

    function toggleInputState() {
        if (urlInput.value.trim().length > 0) {
            pasteBtn.style.display = 'none';
            clearBtn.style.display = 'flex';
        } else {
            pasteBtn.style.display = 'flex';
            clearBtn.style.display = 'none';
        }
    }
    urlInput.addEventListener('input', toggleInputState);

    pasteBtn.addEventListener('click', async () => {
        try {
            const text = await navigator.clipboard.readText();
            if (text) {
                urlInput.value = text.trim();
                toggleInputState();
            }
        } catch (err) {
            urlInput.focus();
        }
    });

    clearBtn.addEventListener('click', () => {
        urlInput.value = '';
        toggleInputState();
        hideError();
    });

    extractForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const url = urlInput.value.trim();
        if (!url) return;

        if (!url.includes('tiktok.com')) {
            showError('Introduce un enlace válido de TikTok.');
            return;
        }

        setLoading(true);
        hideError();

        if (window.AndroidApp) {
            window.AndroidApp.extract(url);
        } else {
            fetch('/api/extract', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ url })
            })
            .then(r => r.json())
            .then(res => {
                if (res.success && res.data) renderWorkstation(res.data);
                else showError(res.detail || 'Error procesando enlace');
            })
            .catch(err => showError('Error de red'))
            .finally(() => setLoading(false));
        }
    });

    window.onExtractionComplete = function(jsonString) {
        setLoading(false);
        try {
            const res = JSON.parse(jsonString);
            if (res.success && res.data) {
                renderWorkstation(res.data);
                saveToReel(res.data);
            } else {
                showError(res.error || 'No fue posible resolver el video.');
            }
        } catch (e) {
            showError('Error interno de análisis: ' + e.message);
        }
    };

    window.onSharedLinkReceived = function(sharedText) {
        if (sharedText && sharedText.includes('tiktok.com')) {
            urlInput.value = sharedText.trim();
            toggleInputState();
            submitBtn.click();
        }
    };

    function renderWorkstation(item) {
        currentItem = item;
        engineUsedBadge.textContent = 'Android Native Engine';
        displayVideoId.textContent = `ID: ${item.id}`;

        creatorAvatar.src = item.author.avatar || '';
        creatorNickname.textContent = item.author.nickname || 'Creador de TikTok';
        creatorHandle.textContent = '@tiktok_user';
        captionText.textContent = item.title || 'Sin descripción';

        metricLikes.textContent = 'CLEAN';
        metricComments.textContent = 'ORIGINAL';
        metricShares.textContent = 'DIRECT';
        metricPlays.textContent = 'MP4';

        studioVideo.src = item.video_url;
        studioVideo.load();

        btnDownloadHD.onclick = (e) => {
            e.preventDefault();
            if (window.AndroidApp) {
                window.AndroidApp.download(item.video_url, `vortex_${item.id}_CLEAN.mp4`);
            }
        };

        if (item.audio_url) {
            btnDownloadAudio.style.display = 'flex';
            btnDownloadAudio.onclick = (e) => {
                e.preventDefault();
                if (window.AndroidApp) {
                    window.AndroidApp.download(item.audio_url, `vortex_${item.id}_AUDIO.mp3`);
                }
            };
        } else {
            btnDownloadAudio.style.display = 'none';
        }

        btnDownloadCover.style.display = 'none';

        workstationSection.style.display = 'block';
        workstationSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    speedButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            speedButtons.forEach(b => b.classList.remove('speed-active'));
            btn.classList.add('speed-active');
            studioVideo.playbackRate = parseFloat(btn.dataset.speed);
        });
    });

    studioVideo.addEventListener('timeupdate', () => {
        const cur = formatTime(studioVideo.currentTime);
        const dur = formatTime(studioVideo.duration || 0);
        timecodeDisplay.textContent = `${cur} / ${dur}`;
    });

    studioVideo.addEventListener('play', () => {
        waveformVisual.classList.add('playing');
    });

    studioVideo.addEventListener('pause', () => {
        waveformVisual.classList.remove('playing');
    });

    copyCaptionBtn.addEventListener('click', async () => {
        if (!currentItem || !currentItem.title) return;
        try {
            await navigator.clipboard.writeText(currentItem.title);
            if (window.AndroidApp) window.AndroidApp.showToast('Descripción copiada');
        } catch (e) {}
    });

    copyShareLinkBtn.addEventListener('click', async () => {
        if (!currentItem || !currentItem.video_url) return;
        try {
            await navigator.clipboard.writeText(currentItem.video_url);
            if (window.AndroidApp) window.AndroidApp.showToast('Enlace de video copiado');
        } catch (e) {}
    });

    newExtractBtn.addEventListener('click', () => {
        workstationSection.style.display = 'none';
        studioVideo.pause();
        studioVideo.src = '';
        urlInput.value = '';
        toggleInputState();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    });

    function setLoading(isLoading) {
        submitBtn.disabled = isLoading;
        if (isLoading) {
            launchText.style.display = 'none';
            launchLoader.style.display = 'flex';
        } else {
            launchText.style.display = 'flex';
            launchLoader.style.display = 'none';
        }
    }

    function showError(msg) {
        errorMessage.textContent = msg;
        errorBox.style.display = 'flex';
    }

    function hideError() {
        errorBox.style.display = 'none';
        errorMessage.textContent = '';
    }

    function formatTime(seconds) {
        if (isNaN(seconds)) return '00:00';
        const mins = Math.floor(seconds / 60);
        const secs = Math.floor(seconds % 60);
        return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }

    function loadReel() {
        const stored = localStorage.getItem('vortex_reel');
        if (!stored) {
            reelSection.style.display = 'none';
            return;
        }
        try {
            const list = JSON.parse(stored);
            if (list.length === 0) {
                reelSection.style.display = 'none';
                return;
            }
            reelGrid.innerHTML = '';
            list.forEach(item => {
                const card = document.createElement('div');
                card.className = 'reel-card';
                card.innerHTML = `
                    <img class="reel-thumb" src="${item.author.avatar || ''}" alt="">
                    <div class="reel-details">
                        <div class="reel-author">${item.author.nickname || 'Creador'}</div>
                        <div class="reel-caption">${item.title || 'Video de TikTok'}</div>
                    </div>
                `;
                card.addEventListener('click', () => renderWorkstation(item));
                reelGrid.appendChild(card);
            });
            reelSection.style.display = 'block';
        } catch (e) {}
    }

    function saveToReel(item) {
        let list = [];
        try {
            const stored = localStorage.getItem('vortex_reel');
            if (stored) list = JSON.parse(stored);
        } catch (e) {}
        list = list.filter(x => x.id !== item.id);
        list.unshift(item);
        if (list.length > 8) list.pop();
        localStorage.setItem('vortex_reel', JSON.stringify(list));
        loadReel();
    }

    clearReelBtn.addEventListener('click', () => {
        localStorage.removeItem('vortex_reel');
        loadReel();
    });

    loadReel();
});
