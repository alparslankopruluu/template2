# ScreenMotion

Interactive live wallpaper MVP — tilt, touch, particles. Dark cinematic UI.

**Themes:** Space · Aquarium · Vehicle

Target repo: `https://github.com/alparslankopruluu/template2`

---

## English

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
- **Space:** tilt parallax stars; swipe for meteors; long-press for spaceship.
- **Aquarium:** fish swim toward your finger; tilt shifts water current; bubbles rise.
- **Vehicle:** tilt to change lane; drag to steer; fast swipe for boost.

### iOS — open, run, export
1. Open `ios/ScreenMotion.xcodeproj` in Xcode.
2. Set your **Team** under Signing & Capabilities (bundle id `com.screenmotion.app`).
3. Run on a simulator or device. **Tilt needs a real device** (simulator shows idle scene + touch).
4. Pick a theme, interact in the preview.
5. **Export short video** → allow Photos → ~3s MP4 saved via `AVAssetWriter`.
6. **How to set wallpaper on iOS** sheet explains Photos → Use as Wallpaper (iOS has no realtime gyro live wallpaper API like Android).

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
- **Space:** eğince yıldız paralaksı; kaydırınca meteor; uzun basınca uzay gemisi.
- **Aquarium:** balıklar parmağa yaklaşır; eğince akıntı; kabarcıklar.
- **Vehicle:** eğince şerit; sürükleyerek sürüş; hızlı kaydırınca boost.

### iOS — açma, çalıştırma, dışa aktarma
1. Xcode’da `ios/ScreenMotion.xcodeproj` dosyasını açın.
2. Signing’de **Team** seçin (bundle id: `com.screenmotion.app`).
3. Simülatör veya cihazda çalıştırın. **Eğme için gerçek cihaz** gerekir.
4. Tema seçip önizlemede etkileşime geçin.
5. **Export short video** → Photos izni → `AVAssetWriter` ile ~3 sn MP4.
6. **How to set wallpaper** sayfası: Photos → Duvar Kağıdı Olarak Kullan (iOS’ta Android’deki gibi anlık jiroskop canlı duvar kağıdı API’si yok).

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
- Android Canvas renderers implement `SceneRenderer` — ready to swap for OpenGL ES later.
- iOS uses SwiftUI `Canvas` + `TimelineView` — ready for Metal/SpriteKit later.
- SharedPreferences / UserDefaults store selected theme and onboarding flag.

## Limitations
- Android Gradle wrapper JAR is not vendored; open in Android Studio to sync/generate.
- iOS export produces a short styled MP4 placeholder (not a full Live Photo pipeline).
- iOS Lock/Home screen cannot host realtime interactive wallpapers; preview + export/tutorial is the supported path.
- Nature theme from the product vision is not in this MVP (Space / Aquarium / Vehicle only).
- No GitHub push from this workspace — parent agent pushes to `template2`.
