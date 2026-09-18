# ScreenMotion — MVP brief

Fun, interactive live wallpaper mobile app. Dark, premium, cinematic visual language. Animations, particles, parallax, gyro + touch. Make it delightful and polished.

## Product
Users pick themes (vehicles, aquarium, space, nature) and get interactive lock/home wallpaper experiences.
- Phone tilt moves the scene (gyro/parallax).
- Touch, drag, swipe interactions.
- Particles: stars, bubbles, dust, etc.

## Platforms
### Android (primary interactive wallpaper)
- Kotlin
- WallpaperService + SurfaceHolder + render loop ~60fps
- SensorManager / TYPE_GAME_ROTATION_VECTOR (or equivalent)
- First render: Android Canvas; structure ready for OpenGL ES later
- Real "Set as live wallpaper" apply button
- Package layout: data, motion, render, wallpaper, ui, util
- Suggested types: ThemeConfig, ThemeType, ConfigRepository, MotionController, SceneRenderer, AquariumSceneRenderer, SpaceSceneRenderer, VehicleSceneRenderer, NatureSceneRenderer, InteractiveWallpaperService
- SharedPreferences for MVP settings

### iOS
- Swift + SwiftUI
- CoreMotion / CMMotionManager
- Interactive preview inside the app (system wallpaper cannot do realtime gyro/touch)
- First render: SwiftUI Canvas + TimelineView (structure ready for Metal/SpriteKit later)
- Export path: short video (~3s) and/or Live Photo placeholder; Photos permission; tutorial to set as wallpaper
- UIImageRenderer / AVAssetWriter / Photos API as appropriate for MVP

## MVP themes (must ship)
1. **Space** — gyro parallax stars + planets; swipe meteor; long-press friendly ship
2. **Aquarium** — expressive fish school toward finger; tilt current; bubbles
3. **Vehicle** — neon sportscar; tilt lane; finger drive; swipe boost
4. **Nature** — clouds, birds follow drag, wind particles, parallax trees/hills, leaf mascot

## UX
- Theme picker
- Interactive preview that feels alive
- Step-by-step onboarding that demonstrates tilt + touch on the device
- Android: apply live wallpaper
- iOS: export + how-to-set wallpaper tutorial
- Polish: smooth motion, nice easing, cinematic dark UI, playful micro-interactions
- Include placeholder mascot-like visual elements (fish, car, stars, ship) as drawn/procedural or simple assets — fun and interactive, not empty geometry only

## Repo layout
Monorepo named ScreenMotion:
- `/android` — Android Studio project
- `/ios` — Xcode / SwiftUI project
- `/README.md` — how to open, run, set wallpaper on both platforms
- Assets folder for any PNG placeholders

## Success criteria
1. Both projects open/build in their IDEs (document any required SDK versions).
2. Android: live wallpaper service renders chosen theme with gyro + touch; can be applied as wallpaper.
3. iOS: SwiftUI app with theme picker + interactive preview (gyro + touch) + export or clear placeholder for video/Live Photo.
4. At least space, aquarium, vehicle scenes feel distinct and fun.
5. README in Turkish and English with run instructions.
6. Commit everything to the new Origin repo main branch.

Do not invent a separate product name; use ScreenMotion.
