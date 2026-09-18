# ScreenMotion

Interactive live wallpaper — tilt, touch, particles, expressive mascots. Apple-inspired graphite UI (system blue #0A84FF).

**Themes:** Space · Aquarium · Vehicle (4 variants) · Nature

Target repo: `https://github.com/alparslankopruluu/template2`

---

## English

### What’s new (v4)
- **Sound effects** — lightweight procedural SFX (theme select, touch splash, boost, meteor, bubble pop, bird/wind, apply success). Mute toggle persists. Fails silently if audio unavailable.
- **Vehicle variants** — sports car, truck, motorcycle, helicopter; chip picker in Vehicle theme; distinct silhouettes, trail colors, boost behavior.
- **Store Showcase** — in-app marketing screen (hero EN+TR, feature highlights, theme gallery). Listing copy under `store/` (Play + App Store, EN+TR). Optional mock frames in `assets/store_mock/`.

### Requirements
- **Android:** Android Studio Hedgehog+ (AGP 8.2 / Gradle 8.2), JDK 17, SDK 34, minSdk 26
- **iOS:** Xcode 15+, iOS 16+ deployment target, physical device recommended for CoreMotion

### Android — open, run, try v3
1. Open `android/` in Android Studio → sync → Run (API 26+).
2. **Sounds:** tap themes / preview / Set wallpaper; mute with the speaker icon (top-right). Preference persists.
3. **Vehicle picker:** choose **Vehicle** theme → chip row appears (Sports / Truck / Bike / Heli). Swipe for boost; each type looks/feels different.
4. **Showcase:** tap **Showcase** for store-style hero + features + gallery.
5. **Live wallpaper + lock:** **Set wallpaper** opens live chooser (Home) and applies FLAG_LOCK static frame; Huawei guide sheet follows.
6. **Huawei guide:** tap **Huawei / Honor lock guide** anytime.

### iOS — open, run, try v3
1. Open `ios/ScreenMotion.xcodeproj` → set Team → Run (device for tilt).
2. **Sounds:** speaker icon toggles mute; interactions play procedural tones.
3. **Vehicle picker:** select Vehicle → chip carousel under themes.
4. **Showcase:** tap **Showcase** sheet.
5. **Export** still writes ~3s MP4; tutorial sheet for wallpaper steps.

### Project layout
```
screenmotion/
  android/          Kotlin WallpaperService + Canvas scenes + SFX + Showcase
  ios/              SwiftUI + CoreMotion preview + SFX + Showcase + export
  store/            Play / App Store listing copy (EN+TR)
  assets/           Icon + store_mock PNG frames
  README.md
```

---

## Türkçe

### Bu sürümde neler var (v4)
- **Ses efektleri** — hafif prosedürel SFX; sessize alma kaydedilir; ses yoksa sessizce geçilir.
- **Araç varyantları** — spor araba, kamyon, motor, helikopter; Vehicle temasında çip seçici.
- **Mağaza Vitrini** — uygulama içi hero (TR+EN), özellikler, tema galerisi. Metinler `store/` altında.

### Gereksinimler
- **Android:** Android Studio Hedgehog+, JDK 17, SDK 34, minSdk 26
- **iOS:** Xcode 15+, iOS 16+, CoreMotion için gerçek cihaz önerilir

### Android — v3’ü deneme
1. `android/` klasörünü Studio’da açıp çalıştırın.
2. **Ses:** tema / önizleme / duvar kağıdı; sağ üstteki hoparlör ile sessiz.
3. **Araç seçici:** Vehicle teması → Sports / Truck / Bike / Heli çipleri; kaydırınca boost.
4. **Vitrin:** **Showcase / Vitrin** düğmesi.
5. **Canlı + kilit:** **Duvar kağıdı yap** ana ekran canlı seçiciyi açar ve kilit için statik kare atar; ardından Huawei rehberi gelir.
6. **Huawei rehberi:** **Huawei / Honor kilit rehberi** düğmesi.

### iOS — v3’ü deneme
1. `ios/ScreenMotion.xcodeproj` → Team → Run.
2. Hoparlör ikonu ile sessiz; Vehicle çipleri; **Showcase** sayfası; Export + tutorial aynı.

### Klasör yapısı
```
screenmotion/
  android/          Kotlin + SFX + Showcase
  ios/              SwiftUI + SFX + Showcase + export
  store/            Mağaza metinleri (TR+EN)
  assets/           İkon + mock PNG
  README.md
```

---

## Architecture notes
- Android: `LockWallpaperApplier` (render/static → FLAG_LOCK + picker fallback) + `HuaweiLockGuideSheet`; `VehiclePainter` (3D Canvas + PNG sprites in `assets/themes/vehicles/`); vehicle chip thumbs; `SfxPlayer` + `ConfigRepository`; `ShowcaseActivity`.
- iOS: `SoundEffects` (procedural WAV via AVAudioPlayer) + `AppSettings`; `VehicleType` + `VehicleScene`; `ShowcaseView`.
- SharedPreferences / UserDefaults: theme, vehicle, onboarding, soundMuted.
- Procedural drawing + procedural audio (no heavy binary SFX packs).

## Limitations
- On many Huawei/Honor devices live wallpaper cannot drive the lock screen or AOD “Tam ekran”; static FLAG_LOCK is required.
- Android Gradle wrapper JAR may need Studio to generate.
- iOS cannot host realtime interactive system wallpapers; preview + export/tutorial remain the path.
- ToneGenerator / generated tones are intentionally simple, not studio-quality samples.
- No GitHub push from this workspace — parent agent pushes to `template2`.
