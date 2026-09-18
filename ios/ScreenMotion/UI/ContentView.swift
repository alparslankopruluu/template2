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
                        }
                    }
                }
                .padding(.vertical, 4)
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
