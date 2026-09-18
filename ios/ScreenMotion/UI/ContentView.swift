import SwiftUI

struct ContentView: View {
    @EnvironmentObject var settings: AppSettings
    @StateObject private var motion = MotionManager()
    @State private var touch = TouchState()
    @State private var showOnboarding = false
    @State private var showTutorial = false
    @State private var showExport = false

    var body: some View {
        ZStack {
            Color(red: 0.03, green: 0.03, blue: 0.06).ignoresSafeArea()

            VStack(alignment: .leading, spacing: 16) {
                header
                previewCard
                themePicker
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
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("ScreenMotion")
                .font(.system(size: 28, weight: .bold, design: .rounded))
                .foregroundStyle(Color(red: 0.95, green: 0.96, blue: 1))
            Text("Interactive preview · export for wallpaper")
                .font(.subheadline)
                .foregroundStyle(Color(red: 0.6, green: 0.64, blue: 0.78))
        }
    }

    private var previewCard: some View {
        SceneCanvasView(theme: settings.selectedTheme, motion: motion, touch: $touch)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(Color.white.opacity(0.08), lineWidth: 1)
            )
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var themePicker: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Choose a theme")
                .font(.caption)
                .foregroundStyle(Color(red: 0.6, green: 0.64, blue: 0.78))
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(ThemeType.allCases) { theme in
                        ThemeCard(theme: theme, selected: settings.selectedTheme == theme) {
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.7)) {
                                settings.selectedTheme = theme
                                touch = TouchState()
                            }
                        }
                    }
                }
            }
        }
    }

    private var actionButtons: some View {
        VStack(spacing: 10) {
            Button {
                showExport = true
            } label: {
                Label("Export short video", systemImage: "square.and.arrow.up")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
            }
            .buttonStyle(.borderedProminent)
            .tint(Color(red: 0.42, green: 0.55, blue: 1))

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
            VStack(spacing: 6) {
                Text(theme.emoji).font(.largeTitle)
                Text(theme.rawValue)
                    .font(.caption.weight(.bold))
                    .foregroundStyle(.white)
            }
            .frame(width: 100, height: 88)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Color(red: 0.09, green: 0.09, blue: 0.13))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(selected ? Color(red: 0.42, green: 0.55, blue: 1) : Color.clear, lineWidth: 3)
            )
            .opacity(selected ? 1 : 0.72)
            .scaleEffect(selected ? 1.04 : 1)
        }
        .buttonStyle(.plain)
    }
}
