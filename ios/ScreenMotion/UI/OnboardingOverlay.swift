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
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(.white)
                Text("Canlı duvar kağıdı · kısa ipuçları")
                    .font(.caption)
                    .foregroundStyle(SMColor.textSecondary)

                ForEach(Array(tips.enumerated()), id: \.offset) { _, tip in
                    HStack(alignment: .top, spacing: 12) {
                        Image(systemName: tip.icon)
                            .font(.title3)
                            .foregroundStyle(SMColor.accent)
                            .frame(width: 28)
                        VStack(alignment: .leading, spacing: 3) {
                            Text(tip.title)
                                .font(.subheadline.weight(.bold))
                                .foregroundStyle(SMColor.textPrimary)
                            Text(tip.detail)
                                .font(.footnote)
                                .foregroundStyle(SMColor.textSecondary)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }
                    .padding(.vertical, 4)
                }

                Text("On iOS, interactive wallpapers live in-app — export a clip or follow the tutorial. · iOS’ta etkileşim uygulamada; dışa aktar veya öğreticiyi izle.")
                    .font(.caption2)
                    .foregroundStyle(SMColor.textSecondary)
                    .padding(.top, 4)

                Button("Got it — let's play · Tamam, oynayalım", action: onDismiss)
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(SMColor.accent, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .foregroundStyle(.white)
                    .padding(.top, 8)
            }
            .padding(26)
            .background(
                RoundedRectangle(cornerRadius: 28, style: .continuous)
                    .fill(SMColor.elevated)
                    .overlay(
                        RoundedRectangle(cornerRadius: 28, style: .continuous)
                            .stroke(SMColor.stroke, lineWidth: 1)
                    )
            )
            .padding(22)
        }
    }
}
