import SwiftUI

struct WallpaperTutorialSheet: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text("iOS cannot run realtime gyro/touch live wallpapers on the Lock/Home screen like Android. Use export + Photos instead.")
                        .foregroundStyle(.secondary)

                    step(1, title: "Export a short clip", detail: "Tap Export in ScreenMotion. Allow Photos access when asked.")
                    step(2, title: "Open Photos", detail: "Find the saved video (or still frames) in your library.")
                    step(3, title: "Set as wallpaper", detail: "Share → Use as Wallpaper (or Settings → Wallpaper → Add New). Adjust and set for Lock Screen / Home Screen.")
                    step(4, title: "Optional Live Photo", detail: "If you convert the clip to a Live Photo with a shortcut or third-party tool, you get a short motion wallpaper.")

                    Text("Tip: Keep the interactive preview open anytime — full Space / Aquarium / Vehicle play lives here.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .padding(.top, 8)
                }
                .padding(24)
            }
            .background(Color(red: 0.03, green: 0.03, blue: 0.06))
            .navigationTitle("Set wallpaper")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
        .preferredColorScheme(.dark)
    }

    private func step(_ n: Int, title: String, detail: String) -> some View {
        HStack(alignment: .top, spacing: 14) {
            Text("\(n)")
                .font(.headline.monospacedDigit())
                .frame(width: 32, height: 32)
                .background(Circle().fill(Color(red: 0.42, green: 0.55, blue: 1)))
            VStack(alignment: .leading, spacing: 4) {
                Text(title).font(.headline)
                Text(detail).font(.subheadline).foregroundStyle(.secondary)
            }
        }
    }
}
