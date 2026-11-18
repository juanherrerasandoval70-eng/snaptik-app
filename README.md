# SnapTik PRO - Android Video Downloader

Aplicación nativa Android ultra-optimizada para descarga de vídeos en Full HD 1080p sin marca de agua, con sistema de actualización automática integrado (OTA) y monetización Adsterra Smartlinks.

## Estructura del Proyecto
- `src/`: Código fuente nativo Java.
- `assets/`: Interfaz de usuario OLED Glassmorphism con WebAppInterface.
- `res/`: Recursos Android (strings, layouts, drawables).
- `version.json`: Control remoto de versiones para actualizaciones automáticas.
- `publish_version.ps1`: Automatización de compilación y publicación de nuevas versiones.
- `build_apk.ps1`: Script de compilación nativa directa (AAPT2, Javac, D8, Zipalign, Apksigner).
