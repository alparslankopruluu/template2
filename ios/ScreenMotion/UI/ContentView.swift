import SwiftUI

/// Apple-inspired graphite palette (system blue accent).
enum SMColor {
    static let bg = Color.black
    static let card = Color(red: 0.17, green: 0.17, blue: 0.18) // #2C2C2E
    static let elevated = Color(red: 0.11, green: 0.11, blue: 0.12) // #1C1C1E
    static let accent = Color(red: 0.039, green: 0.518, blue: 1.0) // #0A84FF
    static let textPrimary = Color.white
    static let textSecondary = Color(red: 0.557, green: 0.557, blue: 0.576) // #8E8E93
    static let stroke = Color(red: 0.227, green: 0.227, blue: 0.235) // #3A3A3C
}

struct ContentView: View {
    @EnvironmentObject var settings: AppSettings
    @StateObject private var motion = MotionManager()
    @State private var touch = TouchState()
    @State private var showOnboarding = false
    @State private var showTutorial = false
    @State private var showExport = false
    @State private var showShowcase = false

    var body: some View {
        ZStack {
            SMColor.bg.ignoresSafeArea()

            VStack(alignment: .leading, spacing: 16) {
                header
                previewCard
                themePicker
                if settings.selectedTheme == .vehicle {
                    vehiclePicker
                }
                actionButtons
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)

            if showOnboarding {
                OnboardingOverlay {
                    settings.onboardingDone = true
                    withAnimation { showOnboarding = false }
                }
                .transition(.opacity)
            }
        }
        .onAppear {
            motion.start()
            SoundEffects.shared.isMuted = settings.soundMuted
            if !settings.onboardingDone {
                showOnboarding = true
            }
        }
        .onDisappear { motion.stop() }
        .sheet(isPresented: $showTutorial) {
            WallpaperTutorialSheet()
        }
        .sheet(isPresented: $showExport) {
            ExportView(theme: settings.selectedTheme)
        }
        .sheet(isPresented: $showShowcase) {
            ShowcaseView()
        }
    }

    private var header: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 4) {
                Text("ScreenMotion")
                    .font(.system(size: 34, weight: .bold, design: .default))
                    .foregroundStyle(SMColor.textPrimary)
                Text("Interactive preview · export for wallpaper")
                    .font(.subheadline)
                    .foregroundStyle(SMColor.textSecondary)
            }
            Spacer()
            Button {
                settings.soundMuted.toggle()
                if !settings.soundMuted {
                    SoundEffects.shared.play(.themeSelect)
                }
            } label: {
                Image(systemName: settings.soundMuted ? "speaker.slash.fill" : "speaker.wave.2.fill")
                    .font(.title3)
                    .foregroundStyle(SMColor.textPrimary)
                    .opacity(settings.soundMuted ? 0.55 : 1)
                    .frame(width: 40, height: 40)
            }
            .accessibilityLabel("Mute sound effects")
        }
    }

    private var previewCard: some View {
        SceneCanvasView(
            theme: settings.selectedTheme,
            vehicle: settings.selectedVehicle,
            motion: motion,
            touch: $touch
        )
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(SMColor.stroke, lineWidth: 1)
        )
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var themePicker: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Choose a theme · Tema seç")
                .font(.caption)
                .foregroundStyle(SMColor.textSecondary)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(ThemeType.allCases) { theme in
                        ThemeCard(theme: theme, selected: settings.selectedTheme == theme) {
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.72)) {
                                settings.selectedTheme = theme
                                touch = TouchState()
                            }
                            SoundEffects.shared.play(.themeSelect)
                        }
                    }
                }
                .padding(.vertical, 4)
            }
        }
    }

    private var vehiclePicker: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Vehicle type · Araç tipi")
                .font(.caption)
                .foregroundStyle(SMColor.textSecondary)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(VehicleType.allCases) { v in
                        Button {
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.75)) {
                                settings.selectedVehicle = v
                            }
                            SoundEffects.shared.play(.themeSelect)
                        } label: {
                            HStack(spacing: 6) {
                                Text(v.emoji)
                                Text(v.rawValue)
                                    .font(.caption.weight(.semibold))
                                    .foregroundStyle(SMColor.textPrimary)
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(
                                Capsule()
                                    .fill(SMColor.card)
                                    .overlay(
                                        Capsule()
                                            .stroke(
                                                settings.selectedVehicle == v
                                                    ? SMColor.accent
                                                    : SMColor.stroke,
                                                lineWidth: settings.selectedVehicle == v ? 2 : 1
                                            )
                                    )
                            )
                            .opacity(settings.selectedVehicle == v ? 1 : 0.75)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    private var actionButtons: some View {
        VStack(spacing: 10) {
            HStack(spacing: 10) {
                Button {
                    showShowcase = true
                    SoundEffects.shared.play(.themeSelect)
                } label: {
                    Label("Showcase", systemImage: "sparkles.rectangle.stack")
                        .font(.subheadline.weight(.semibold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                }
                .buttonStyle(.bordered)
                .tint(SMColor.accent)

                Button {
                    showExport = true
                    SoundEffects.shared.play(.applySuccess)
                } label: {
                    Label("Export", systemImage: "square.and.arrow.up")
                        .font(.subheadline.weight(.semibold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                }
                .buttonStyle(.borderedProminent)
                .tint(SMColor.accent)
            }

            Button {
                showTutorial = true
            } label: {
                Label("How to set wallpaper on iOS", systemImage: "questionmark.circle")
                    .font(.subheadline.weight(.semibold))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
            }
            .buttonStyle(.bordered)
            .tint(SMColor.accent)
        }
    }
}

struct ThemeCard: View {
    let theme: ThemeType
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 6) {
                ZStack {
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(
                            LinearGradient(colors: theme.gradient, startPoint: .topLeading, endPoint: .bottomTrailing)
                        )
                        .frame(height: 48)
                    Text(theme.emoji).font(.title)
                }
                Text(theme.rawValue)
                    .font(.caption.weight(.bold))
                    .foregroundStyle(SMColor.textPrimary)
                Text(theme.subtitle)
                    .font(.system(size: 9))
                    .foregroundStyle(SMColor.textSecondary)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(10)
            .frame(width: 128, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(SMColor.card)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(selected ? SMColor.accent : SMColor.stroke, lineWidth: selected ? 2 : 1)
            )
            .opacity(selected ? 1 : 0.82)
            .scaleEffect(selected ? 1.03 : 1)
        }
        .buttonStyle(.plain)
    }
}
