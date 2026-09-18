# ScreenMotion

Interactive live wallpaper — tilt, touch, particles, expressive mascots. Apple-inspired graphite UI (system blue #0A84FF).

**Themes:** Space · Aquarium · Vehicle (4 variants) · Nature

Target repo: `https://github.com/alparslankopruluu/template2`

---

## English

### What’s new (v5)
- **Mock monetization (demo only)** — SharedPreferences / UserDefaults `is_pro` flag. **No Play Billing, no AdMob, no StoreKit.** Paywall bottom sheet with yearly (399,99 TL, “En iyi değer” / Best value) + monthly (79,99 TL). Mock purchase sets Pro + toast.
- **Free rules** — all 4 themes free; Sports vehicle free; Truck / Bike / Heli locked behind Pro (lock icon → paywall).
- **Theme-embedded billboards** — subtle Sponsored / sample brand panels in Canvas scenes when !Pro; omitted when Pro (Vehicle roadside, Space soft panel, Aquarium floating sign, Nature road sign).
- **Debug** — Android: long-press title **or** “Mock Pro aç/kapat” toggle. iOS: long-press **Pro** button.

### What’s new (v4)
- **Sound effects** — lightweight procedural SFX (theme select, touch splash, boost, meteor, bubble pop, bird/wind, apply success). Mute toggle persists. Fails silently if audio unavailable.
- **Vehicle variants** — sports car, truck, motorcycle, helicopter; chip picker in Vehicle theme; distinct silhouettes, trail colors, boost behavior.
- **Store Showcase** — in-app marketing screen (hero EN+TR, feature highlights, theme gallery). Listing copy under `store/` (Play + App Store, EN+TR). Optional mock frames in `assets/store_mock/`.

### Requirements
- **Android:** Android Studio Hedgehog+ (AGP 8.2 / Gradle 8.2), JDK 17, SDK 34, minSdk 26
- **iOS:** Xcode 15+, iOS 16+ deployment target, physical device recommended for CoreMotion

### Android — open, run, try v5
1. Open `android/` in Android Studio → sync → Run (API 26+).
2. **Sounds:** tap themes / preview / Set wallpaper; mute with the speaker icon (top-right). Preference persists.
3. **Vehicle picker:** choose **Vehicle** theme → chip row (Sports free; Truck / Bike / Heli show 🔒). Locked tap opens mock paywall.
4. **Mock Pro:** tap **Pro** for paywall; long-press title or **Mock Pro aç/kapat** to toggle without purchase. Billboards hide when Pro.
5. **Showcase:** tap **Showcase** for store-style hero + features + gallery.
6. **Live wallpaper + lock:** **Set wallpaper** opens live chooser (Home) and applies FLAG_LOCK static frame; Huawei guide sheet follows.
7. **Huawei guide:** tap **Huawei / Honor lock guide** anytime.

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

### Bu sürümde neler var (v5)
- **Mock monetikasyon (yalnızca demo)** — `is_pro` SharedPreferences / UserDefaults. **Gerçek Play Billing / AdMob / StoreKit yok.** Yıllık 399,99 TL (En iyi değer) + aylık 79,99 TL paywall; mock satın alma Pro açar.
- **Ücretsiz kurallar** — 4 tema ücretsiz; yalnızca Sports araç ücretsiz; Truck / Bike / Heli → Pro.
- **Tema panoları** — !Pro iken sahnelerde Sponsorlu / Örnek Marka panoları; Pro’da gizlenir.
- **Debug** — başlığa uzun bas veya “Mock Pro aç/kapat”.

### Bu sürümde neler var (v4)
- **Ses efektleri** — hafif prosedürel SFX; sessize alma kaydedilir; ses yoksa sessizce geçilir.
- **Araç varyantları** — spor araba, kamyon, motor, helikopter; Vehicle temasında çip seçici.
- **Mağaza Vitrini** — uygulama içi hero (TR+EN), özellikler, tema galerisi. Metinler `store/` altında.

### Gereksinimler
- **Android:** Android Studio Hedgehog+, JDK 17, SDK 34, minSdk 26
- **iOS:** Xcode 15+, iOS 16+, CoreMotion için gerçek cihaz önerilir

### Android — v5’i deneme
1. `android/` klasörünü Studio’da açıp çalıştırın.
2. **Ses:** tema / önizleme / duvar kağıdı; sağ üstteki hoparlör ile sessiz.
3. **Araç seçici:** Vehicle → Sports ücretsiz; kilitli araçlara dokunun → mock paywall.
4. **Mock Pro:** **Pro** düğmesi / başlığa uzun bas / **Mock Pro aç/kapat**.
5. **Vitrin:** **Showcase / Vitrin** düğmesi.
6. **Canlı + kilit:** **Duvar kağıdı yap** ana ekran canlı seçiciyi açar ve kilit için statik kare atar; ardından Huawei rehberi gelir.
7. **Huawei rehberi:** **Huawei / Honor kilit rehberi** düğmesi.

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
- **Mock Pro:** `ConfigRepository.isPro` (`is_pro` prefs), `PaywallSheet`, `AdBillboardPainter`, locked vehicle chips. No billing SDK.
- iOS: `SoundEffects` (procedural WAV via AVAudioPlayer) + `AppSettings`; `VehicleType` + `VehicleScene`; `ShowcaseView`; light `PaywallView` + `isPro`.
- SharedPreferences / UserDefaults: theme, vehicle, onboarding, soundMuted, **is_pro**.
- Procedural drawing + procedural audio (no heavy binary SFX packs).

## Limitations
- On many Huawei/Honor devices live wallpaper cannot drive the lock screen or AOD “Tam ekran”; static FLAG_LOCK is required.
- Android Gradle wrapper JAR may need Studio to generate.
- iOS cannot host realtime interactive system wallpapers; preview + export/tutorial remain the path.
- ToneGenerator / generated tones are intentionally simple, not studio-quality samples.
- **Monetization is mock only** — no Google Play Billing, AdMob, or StoreKit. Do not ship as real IAP without replacing the mock layer.
- No GitHub push from this workspace — parent agent pushes to `template2`.
