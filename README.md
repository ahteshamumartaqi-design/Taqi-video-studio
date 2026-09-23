# TAQI VIDEO STUDIO (personal-use scaffold)

Kotlin + Jetpack Compose + Media3 Transformer. Min SDK 29 (Android 10+).

## How to open it
1. Install **Android Studio** (free) — Koala or newer.
2. `File → Open` → select this `VideoEditorApp` folder.
3. Let Gradle sync (Android Studio auto-generates the Gradle wrapper the first time — no manual setup needed). If it doesn't offer to, use `File → Sync Project with Gradle Files`.
4. Run on a device/emulator running Android 10+.

The app requests media permissions on first launch (Android 13+ granular photo/video/audio permissions, older devices fall back to `READ_EXTERNAL_STORAGE`).

## What actually works out of the box
| Feature | Status |
|---|---|
| Import video, preview with ExoPlayer | ✅ |
| Trim (start/end) | ✅ |
| Mute original audio | ✅ |
| Add background music (loops to fill video length, adjustable volume) | ✅ |
| Speed change (0.25x–4x) | ✅ |
| Color effects (grayscale, sepia, warm, cool, high-contrast) | ✅ |
| Text overlay (timed, positioned) | ✅ basic version |
| Image-to-video slideshow (static images → clip) | ✅ |
| Export to `Movies/VideoEditor` via MediaStore | ✅ |
| Animated intro (logo/wordmark motion on cold start) | ✅ |
| AI text-to-video via your own Wan 2.1 server | ✅ client + UI, needs your server running |

## AI generation — Wan 2.1, self-hosted
[Wan 2.1](https://github.com/Wan-Video/Wan2.1) is open-weight and unrestricted, which is why
it's used here instead of a paid API — but it's a real diffusion model, not something a
phone can run. Even the smallest text-to-video variant (1.3B params) needs a discrete GPU
with several GB of VRAM. So "your own AI platform" means:

1. Run Wan 2.1 somewhere with a GPU — a cloud GPU instance (RunPod, Lambda, your own
   rig), or ComfyUI with the community Wan nodes.
2. Put a thin HTTP wrapper in front of it that exposes:
   - `POST /generate` → `{ "job_id": "..." }` (accepts `prompt`, `duration_seconds`,
     and optionally `init_image_b64` for image-to-video)
   - `GET /status/{job_id}` → `{ "status": "queued|running|done|error", "progress": 0.0-1.0,
     "video_url": "..." }` when done
3. In the app, go to **AI generation settings** and enter that server's URL (and an API
   key if you added one).

`app/src/main/java/com/example/videoeditor/ai/WanVideoClient.kt` is the HTTP client —
if your server's contract differs from the shape above (e.g. you're calling ComfyUI's
native `/prompt` + `/history` endpoints directly instead of wrapping them), adjust
`submitJob()` / `pollUntilDone()` to match; the rest of the flow (polling, download,
save-to-gallery) stays the same.

## Intro animation
`ui/screens/IntroScreen.kt` runs once on cold start: the logo mark scales/fades in with a
spring overshoot, a gradient ring spins behind it, then the wordmark and tagline settle in
(~1.8s total) before handing off to Home. It sits on top of the OS-level splash screen
(`androidx.core.splashscreen`, wired up in `MainActivity`) that covers the brief gap before
Compose itself is ready.

## What's stubbed / needs more work
- **Reverse video** — Media3 Transformer's pipeline decodes forward-only, so there's no
  built-in reverse export. `ReverseVideoProcessor.kt` documents the two real paths
  (FFmpeg, or manual MediaCodec frame-buffer reversal) and where to hook one in.
- **Ken Burns pan/zoom on images** — currently images are static for their duration.
  An animated zoom needs a custom `GlShaderProgram` that interpolates scale over time;
  `ImageToVideoBuilder.kt` has a comment on where to add it.
- **Overlay with background removed** (cut a subject out of a photo) — needs an
  on-device segmentation model. `OverlayEffects.kt` notes the ML Kit Selfie Segmentation
  dependency to add and where the alpha-mask step goes.
- **Text-to-video (AI)** — there is no free/unlimited way to generate video from a text
  prompt; every capable model (Veo, Runway, Luma, Pika) runs server-side and is billed
  per generation. `TextToVideoScreen.kt` has the UI and a placeholder for wiring in
  whichever provider's API you're willing to pay for.
- **Vignette effect** — currently falls back to a mild contrast bump; a true radial
  vignette needs a small custom shader (see comment in `EffectsLibrary.kt`).

## Why Media3 instead of FFmpeg
You picked Media3/Transformer for a lighter APK and because it's Google's officially
maintained pipeline. The trade-off is the handful of items above that FFmpeg would give
you for free (reverse, more exotic filters) but that Media3 doesn't do natively. If any
of those become a priority, the cleanest fix is adding FFmpegKit (or a maintained fork —
the original `arthenica/ffmpeg-kit` stopped publishing new releases in 2025, so check
current status before depending on it) just for those specific operations, and keeping
Media3 for everything else.

## Building from just a phone (no PC, no Termux SDK setup)
`.github/workflows/build.yml` in this project is already set up to build a debug APK on
GitHub's free build servers every time you push — you only need Termux for `git`, not for
compiling anything. Steps:

1. **Install Termux** from F-Droid (not the Play Store version — it's outdated), then in it:
   ```
   pkg update -y && pkg install git -y
   termux-setup-storage
   ```
2. **Get this project onto your phone** and into Termux's reach — move/extract the zip into
   your Downloads folder, then in Termux:
   ```
   cd ~/storage/downloads
   unzip TaqiVideoStudio.zip
   cd VideoEditorApp
   ```
3. **Create a free GitHub account** (github.com, in your browser) and an empty new
   repository — don't add a README/license from GitHub's side, keep it empty.
4. **Create a Personal Access Token** (GitHub → Settings → Developer settings → Personal
   access tokens → Tokens (classic) → Generate new token, tick "repo" scope). Copy it
   somewhere safe — GitHub only shows it once. You'll use this as your password when
   pushing (GitHub no longer accepts your account password for git operations).
5. **Push the project** from Termux:
   ```
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/<your-username>/<your-repo>.git
   git push -u origin main
   ```
   When it asks for a username/password, use your GitHub username and the token from
   step 4 as the password.
6. **Watch it build**: open your repo on github.com in your phone's browser → **Actions**
   tab → the "Build debug APK" run should already be in progress (takes ~5–10 minutes the
   first time). Green check = success.
7. **Download the APK**: on that same Actions run page, scroll to **Artifacts** →
   download `TaqiVideoStudio-debug-apk` (it downloads as a zip containing the `.apk`).
8. **Install it**: open the zip with your phone's Files app, extract the `.apk`, tap it.
   Android will ask to allow installs from that app (Files/Chrome) the first time — allow
   it, then confirm the install.

Every time you `git add . && git commit -m "..." && git push` after editing the code, a
new APK builds automatically — no local compiling ever required.


```
app/src/main/java/com/example/videoeditor/
  editor/          EditorUiState, VideoEditorViewModel — all edit parameters live here
  processing/       VideoProcessor (orchestrator), EffectsLibrary, OverlayEffects,
                     AudioMixer, ImageToVideoBuilder, SlideshowExporter, ReverseVideoProcessor
  ui/screens/       HomeScreen, EditorScreen, ImageToVideoScreen, TextToVideoScreen
  ui/theme/         Compose color/theme setup
  util/             MediaStoreUtil (save exports to gallery)
```

`VideoProcessor.export()` is the single place that reads an `EditorUiState` and turns it
into a Media3 `Composition` — that's the function to extend when adding a new edit type.
