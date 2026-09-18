import SwiftUI

struct OnboardingOverlay: View {
    let onDismiss: () -> Void

    private let tips: [(icon: String, title: String, detail: String)] = [
        ("gyroscope", "1 · Tilt / Eğ", "Parallax layers, wind, and water current follow your phone. · Katmanlar telefonunu takip eder."),
        ("hand.draw", "2 · Drag / Sürükle", "Fish, birds, and the leaf mascot rush toward your finger. · Balık, kuş ve yaprak parmağına gelir."),
        ("hand.tap", "3 · Swipe & hold / Kaydır & bas", "Swipe for meteors & boost · long-press for the friendly ship. · Meteor, boost ve dost gemi.")
    ]

    var body: some View {
        ZStack {
            Color.black.opacity(0.9).ignoresSafeArea()
            VStack(alignment: .leading, spacing: 14) {
                Text("Make it move")
                    .font(.title.bold())
                    .foregroundStyle(.white)
                Text("Canlı duvar kağıdı · kısa ipuçları")
                    .font(.caption)
                    .foregroundStyle(Color(white: 0.5))

                ForEach(Array(tips.enumerated()), id: \.offset) { _, tip in
                    HStack(alignment: .top, spacing: 12) {
                        Image(systemName: tip.icon)
                            .font(.title3)
                            .foregroundStyle(Color(red: 0.42, green: 0.55, blue: 1))
                            .frame(width: 28)
                        VStack(alignment: .leading, spacing: 3) {
                            Text(tip.title)
                                .font(.subheadline.weight(.bold))
                                .foregroundStyle(Color(red: 0.75, green: 0.8, blue: 1))
                            Text(tip.detail)
                                .font(.footnote)
                                .foregroundStyle(Color(white: 0.7))
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }
                    .padding(.vertical, 4)
                }

                Text("On iOS, interactive wallpapers live in-app — export a clip or follow the tutorial. · iOS’ta etkileşim uygulamada; dışa aktar veya öğreticiyi izle.")
                    .font(.caption2)
                    .foregroundStyle(Color(white: 0.45))
                    .padding(.top, 4)

                Button("Got it — let's play · Tamam, oynayalım", action: onDismiss)
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(Color(red: 1, green: 0.42, blue: 0.54), in: RoundedRectangle(cornerRadius: 14))
                    .foregroundStyle(.white)
                    .padding(.top, 8)
            }
            .padding(26)
            .background(
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .fill(Color(red: 0.08, green: 0.08, blue: 0.12))
                    .overlay(
                        RoundedRectangle(cornerRadius: 24, style: .continuous)
                            .stroke(Color.white.opacity(0.08), lineWidth: 1)
                    )
            )
            .padding(22)
        }
    }
}
