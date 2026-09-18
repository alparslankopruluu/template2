import SwiftUI

struct ExportView: View {
    let theme: ThemeType
    @Environment(\.dismiss) private var dismiss
    @State private var status = "Ready to export."
    @State private var busy = false
    @State private var lastURL: URL?

    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                Image(systemName: busy ? "hourglass" : "film")
                    .font(.system(size: 48))
                    .foregroundStyle(Color(red: 0.42, green: 0.55, blue: 1))
                Text(status)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.secondary)
                    .padding(.horizontal)

                if let lastURL {
                    Text(lastURL.lastPathComponent)
                        .font(.caption.monospaced())
                        .foregroundStyle(.secondary)
                }

                Button {
                    Task { await runExport() }
                } label: {
                    Text(busy ? "Exporting…" : "Generate & save to Photos")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                }
                .buttonStyle(.borderedProminent)
                .tint(Color(red: 0.42, green: 0.55, blue: 1))
                .disabled(busy)

                Text("AVAssetWriter writes ~3s of clearer scene-specific frames (Space / Aquarium / Vehicle / Nature). Use the tutorial sheet to set it as wallpaper.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                Spacer()
            }
            .padding(24)
            .onAppear { status = "Ready to export a ~3s MP4 of \(theme.rawValue)." }
            .navigationTitle("Export")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Close") { dismiss() }
                }
            }
        }
        .preferredColorScheme(.dark)
    }

    private func runExport() async {
        busy = true
        status = "Rendering frames…"
        do {
            let url = try await VideoExporter.exportShortClip(theme: theme)
            lastURL = url
            status = "Saved to Photos!\n\(url.path)"
        } catch {
            status = "Export failed: \(error.localizedDescription)"
        }
        busy = false
    }
}
