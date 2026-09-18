import SwiftUI

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
            Color(red: 0.03, green: 0.03, blue: 0.06).ignoresSafeArea()

            VStack(alignment: .leading, spacing: 14) {
                header
                previewCard
                themePicker
                if settings.selectedTheme == .vehicle {
                    vehiclePicker
                }
                actionButtons
            }
            .padding(20)

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
                    .font(.system(size: 28, weight: .bold, design: .rounded))
                    .foregroundStyle(Color(red: 0.95, green: 0.96, blue: 1))
                Text("Interactive preview · export for wallpaper")
                    .font(.subheadline)
                    .foregroundStyle(Color(red: 0.6, green: 0.64, blue: 0.78))
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
                    .foregroundStyle(Color(red: 0.95, green: 0.96, blue: 1))
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
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(Color.white.opacity(0.08), lineWidth: 1)
        )
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var themePicker: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Choose a theme · Tema seç")
                .font(.caption)
                .foregroundStyle(Color(red: 0.6, green: 0.64, blue: 0.78))
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(ThemeType.allCases) { theme in
                        ThemeCard(theme: theme, selected: settings.selectedTheme == theme) {
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.68)) {
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
                .foregroundStyle(Color(red: 0.6, green: 0.64, blue: 0.78))
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(VehicleType.allCases) { v in
                        Button {
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                                settings.selectedVehicle = v
                            }
                            SoundEffects.shared.play(.themeSelect)
                        } label: {
                            HStack(spacing: 6) {
                                Text(v.emoji)
                                Text(v.rawValue)
                                    .font(.caption.weight(.bold))
                                    .foregroundStyle(.white)
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(
                                Capsule()
                                    .fill(Color(red: 0.08, green: 0.08, blue: 0.12))
                                    .overlay(
                                        Capsule()
                                            .stroke(
                                                settings.selectedVehicle == v
                                                    ? Color(red: 1, green: 0.42, blue: 0.54)
                                                    : Color.white.opacity(0.08),
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
                        .padding(.vertical, 12)
                }
                .buttonStyle(.bordered)
                .tint(Color(red: 0.42, green: 0.55, blue: 1))

                Button {
                    showExport = true
                    SoundEffects.shared.play(.applySuccess)
                } label: {
                    Label("Export", systemImage: "square.and.arrow.up")
                        .font(.subheadline.weight(.semibold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                }
                .buttonStyle(.borderedProminent)
                .tint(Color(red: 0.42, green: 0.55, blue: 1))
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
            .tint(Color(red: 1, green: 0.42, blue: 0.54))
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
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(
                            LinearGradient(colors: theme.gradient, startPoint: .topLeading, endPoint: .bottomTrailing)
                        )
                        .frame(height: 48)
                    Text(theme.emoji).font(.title)
                }
                Text(theme.rawValue)
                    .font(.caption.weight(.bold))
                    .foregroundStyle(.white)
                Text(theme.subtitle)
                    .font(.system(size: 9))
                    .foregroundStyle(Color(red: 0.65, green: 0.68, blue: 0.8))
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(10)
            .frame(width: 128, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(Color(red: 0.08, green: 0.08, blue: 0.12))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(selected ? Color(red: 0.42, green: 0.55, blue: 1) : Color.white.opacity(0.06), lineWidth: selected ? 2.5 : 1)
            )
            .opacity(selected ? 1 : 0.78)
            .scaleEffect(selected ? 1.04 : 1)
            .shadow(color: selected ? Color(red: 0.42, green: 0.55, blue: 1).opacity(0.35) : .clear, radius: 8, y: 2)
        }
        .buttonStyle(.plain)
    }
}
