# ScreenMotion

Interactive live wallpaper — tilt, touch, particles, expressive mascots. Dark cinematic UI.

**Themes:** Space · Aquarium · Vehicle · **Nature**

Target repo: `https://github.com/alparslankopruluu/template2`

---

## English

### What’s new (polish pass)
- **Nature theme** — drifting clouds, birds that follow your drag, wind particles, soft parallax trees/hills (3 depths), cute leaf mascot flutter.
- **Stronger mascots** — Aquarium fish with eyes/fins/cheeks that react; friendlier spaceship + planet accents; sportier neon car with headlights & trail.
- **Richer motion** — smoother easing, more particles, layered parallax, subtle idle animations when you’re not touching.
- **UI** — darker premium theme cards with gradient previews; 3-tip onboarding (EN+TR cues); light haptic on Android theme taps.
- **iOS export** — clearer scene-specific frames (20fps / ~3s) for the chosen theme, including Nature.

### Requirements
- **Android:** Android Studio Hedgehog+ (AGP 8.2 / Gradle 8.2), JDK 17, SDK 34, minSdk 26
- **iOS:** Xcode 15+, iOS 16+ deployment target, physical device recommended for CoreMotion

### Android — open, run, set wallpaper
1. Open `android/` in Android Studio (File → Open → select the `android` folder).
2. Let Gradle sync. If the wrapper JAR is missing, use **File → Settings → Build → Gradle** or let Studio regenerate the wrapper; `gradle/wrapper/gradle-wrapper.properties` points at Gradle 8.2.
3. Select a device/emulator (API 26+) → **Run** the `app` configuration.
4. In-app: pick a theme, play in the live preview (tilt + touch).
5. Tap **Set as live wallpaper** → confirm ScreenMotion in the system picker.
6. Or: long-press Home → Wallpapers → Live wallpapers → **ScreenMotion**.

**Interactions**
- **Space:** tilt parallax stars & planets; swipe for meteors; long-press to pilot the friendly ship (idle bob when idle).
- **Aquarium:** fish school and approach your finger (eyes/fins react); tilt shifts current; bubbles rise.
- **Vehicle:** tilt to change lane; drag to steer; fast swipe for boost; neon trail + headlight beams.
- **Nature:** birds follow drag; wind particles from tilt; parallax hills/trees/clouds; leaf mascot flutters near finger.

### iOS — open, run, export
1. Open `ios/ScreenMotion.xcodeproj` in Xcode.
2. Set your **Team** under Signing & Capabilities (bundle id `com.screenmotion.app`).
3. Run on a simulator or device. **Tilt needs a real device** (simulator shows idle scene + touch).
4. Pick a theme, interact in the preview.
5. **Export short video** → allow Photos → ~3s MP4 with clearer frames of the chosen scene via `AVAssetWriter`.
6. **How to set wallpaper on iOS** sheet explains Photos → Use as Wallpaper (EN + TR). iOS has no realtime gyro live wallpaper API like Android.

### Project layout
```
screenmotion/
  android/          Kotlin WallpaperService + Canvas scenes
  ios/              SwiftUI + CoreMotion preview + export
  assets/           Shared placeholder icon
  README.md
```

---

## Türkçe

### Bu geçişte neler yenilendi
- **Nature teması** — süzülen bulutlar, sürüklemeyi takip eden kuşlar, rüzgar parçacıkları, yumuşak paralaks tepe/ağaç (3 derinlik), sevimli yaprak maskot.
- **Daha güçlü maskotlar** — akvaryum balıkları (göz/yüzgeç tepkisi); dost uzay gemisi + gezegenler; neon izli spor araba.
- **Daha zengin hareket** — yumuşak easing, daha fazla parçacık, katmanlı paralaks, dokunulmadığında idle animasyonlar.
- **Arayüz** — koyu premium tema kartları; 3 ipuçlu onboarding (TR+EN); Android’de hafif haptic.
- **iOS export** — seçilen sahneye özel daha net kareler (Nature dahil).

### Gereksinimler
- **Android:** Android Studio Hedgehog+ (AGP 8.2 / Gradle 8.2), JDK 17, SDK 34, minSdk 26
- **iOS:** Xcode 15+, iOS 16+, CoreMotion için gerçek cihaz önerilir

### Android — açma, çalıştırma, duvar kağıdı
1. Android Studio’da `android/` klasörünü açın.
2. Gradle senkronizasyonunu bekleyin. Wrapper JAR yoksa Studio yeniden üretebilir; `gradle-wrapper.properties` Gradle 8.2’yi işaret eder.
3. API 26+ cihaz/emülatör seçip **Run** ile çalıştırın.
4. Uygulamada tema seçin; önizlemede eğme + dokunma ile oynayın.
5. **Set as live wallpaper** ile sistem seçiciden ScreenMotion’ı onaylayın.
6. Alternatif: Ana ekrana uzun bas → Duvar kağıtları → Canlı duvar kağıtları → **ScreenMotion**.

**Etkileşimler**
- **Space:** eğince yıldız/gezegen paralaksı; kaydırınca meteor; uzun basınca dost gemi.
- **Aquarium:** balıklar sürü halinde parmağa yaklaşır; eğince akıntı; kabarcıklar.
- **Vehicle:** eğince şerit; sürükleyerek sürüş; hızlı kaydırınca boost; neon iz + far.
- **Nature:** kuşlar sürüklemeyi izler; rüzgar parçacıkları; tepeler/ağaçlar/bulutlar; yaprak maskot.

### iOS — açma, çalıştırma, dışa aktarma
1. Xcode’da `ios/ScreenMotion.xcodeproj` dosyasını açın.
2. Signing’de **Team** seçin (bundle id: `com.screenmotion.app`).
3. Simülatör veya cihazda çalıştırın. **Eğme için gerçek cihaz** gerekir.
4. Tema seçip önizlemede etkileşime geçin.
5. **Export short video** → Photos izni → seçili sahnenin daha net kareleriyle ~3 sn MP4.
6. **How to set wallpaper** sayfası TR+EN: Photos → Duvar Kağıdı Olarak Kullan.

### Klasör yapısı
```
screenmotion/
  android/          Kotlin WallpaperService + Canvas sahneler
  ios/              SwiftUI + CoreMotion önizleme + export
  assets/           Ortak ikon
  README.md
```

---

## Architecture notes
- Android Canvas renderers implement `SceneRenderer` — ready to swap for OpenGL ES later. `SceneFactory` + `ThemeType` include **Nature**.
- iOS uses SwiftUI `Canvas` + `TimelineView` — ready for Metal/SpriteKit later. `NatureScene` wired in `SceneCanvasView` and Xcode project.
- SharedPreferences / UserDefaults store selected theme and onboarding flag.
- Procedural drawing only (no heavy binary asset pipelines).

## Limitations
- Android Gradle wrapper JAR is not vendored; open in Android Studio to sync/generate.
- iOS export produces a short styled MP4 (not a full Live Photo pipeline); frames are clearer scene previews, not a full Metal capture of the live Canvas.
- iOS Lock/Home screen cannot host realtime interactive wallpapers; preview + export/tutorial is the supported path.
- No GitHub push from this workspace — parent agent pushes to `template2`.
