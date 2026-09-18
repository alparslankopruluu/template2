import SwiftUI

struct WallpaperTutorialSheet: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Group {
                        Text("EN — iOS cannot run realtime gyro/touch live wallpapers on Lock/Home like Android. Use export + Photos instead.")
                        Text("TR — iOS’ta Android’deki gibi anlık jiroskop/dokunma canlı duvar kağıdı yok. Dışa aktarıp Photos’tan ayarlayın.")
                    }
                    .font(.subheadline)
                    .foregroundStyle(.secondary)

                    step(1,
                         titleEN: "Export a short clip",
                         titleTR: "Kısa klip dışa aktar",
                         detailEN: "Tap Export in ScreenMotion. Allow Photos access when asked. Frames now match your chosen scene (Space / Aquarium / Vehicle / Nature).",
                         detailTR: "Export’a dokunun. Photos izni verin. Kareler seçili sahneye göre üretilir.")
                    step(2,
                         titleEN: "Open Photos",
                         titleTR: "Photos’u aç",
                         detailEN: "Find the saved MP4 in your library.",
                         detailTR: "Kayıtlı MP4’ü kitaplığınızda bulun.")
                    step(3,
                         titleEN: "Set as wallpaper",
                         titleTR: "Duvar kağıdı yap",
                         detailEN: "Share → Use as Wallpaper (or Settings → Wallpaper → Add New). Adjust and set for Lock / Home.",
                         detailTR: "Paylaş → Duvar Kağıdı Olarak Kullan (veya Ayarlar → Duvar Kağıdı). Kilit / Ana ekran seçin.")
                    step(4,
                         titleEN: "Optional Live Photo",
                         titleTR: "İsteğe bağlı Canlı Fotoğraf",
                         detailEN: "Convert the clip to a Live Photo with a shortcut or third-party tool for short motion.",
                         detailTR: "Kısa hareket için klipi Canlı Fotoğrafa dönüştürebilirsiniz.")

                    Text("Tip EN: Keep the interactive preview open — full Space / Aquarium / Vehicle / Nature play lives here.\nİpucu TR: Tam etkileşim uygulamada — dört tema burada canlı.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .padding(.top, 8)
                }
                .padding(24)
            }
            .background(SMColor.bg)
            .navigationTitle("Set wallpaper")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
        .preferredColorScheme(.dark)
    }

    private func step(_ n: Int, titleEN: String, titleTR: String, detailEN: String, detailTR: String) -> some View {
        HStack(alignment: .top, spacing: 14) {
            Text("\(n)")
                .font(.headline.monospacedDigit())
                .frame(width: 32, height: 32)
                .background(Circle().fill(SMColor.accent))
            VStack(alignment: .leading, spacing: 4) {
                Text(titleEN).font(.headline)
                Text(titleTR).font(.subheadline.weight(.semibold)).foregroundStyle(SMColor.textSecondary)
                Text(detailEN).font(.subheadline).foregroundStyle(.secondary)
                Text(detailTR).font(.caption).foregroundStyle(SMColor.textSecondary)
            }
        }
    }
}
