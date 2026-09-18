import SwiftUI

/// Mock paywall — no StoreKit. Yearly primary, monthly secondary.
struct PaywallView: View {
    @EnvironmentObject var settings: AppSettings
    @Environment(\.dismiss) private var dismiss
    @State private var toast = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("ScreenMotion Pro")
                        .font(.system(size: 28, weight: .bold))
                        .foregroundStyle(SMColor.textPrimary)

                    Text("Reklamsız sahneler ve daha zengin araçlar — eğlence kalsın, panolar gitsin.")
                        .font(.subheadline)
                        .foregroundStyle(SMColor.textSecondary)

                    VStack(alignment: .leading, spacing: 12) {
                        bullet("✦ Temaya gömülü reklam panolarını gizle")
                        bullet("✦ Kamyon, Motor ve Helikopter’i aç")
                        bullet("✦ 4 tema sonsuza kadar ücretsiz")
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(SMColor.card)
                            .overlay(
                                RoundedRectangle(cornerRadius: 16, style: .continuous)
                                    .stroke(SMColor.stroke, lineWidth: 1)
                            )
                    )

                    ZStack(alignment: .topTrailing) {
                        Button {
                            mockPurchase()
                        } label: {
                            Text("399,99 TL / yıl")
                                .font(.headline)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 16)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(SMColor.accent)

                        Text("En iyi değer")
                            .font(.caption2.weight(.bold))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(SMColor.accent)
                            .clipShape(Capsule())
                            .offset(x: -4, y: -8)
                    }

                    Button {
                        mockPurchase()
                    } label: {
                        Text("79,99 TL / ay")
                            .font(.subheadline.weight(.semibold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    }
                    .buttonStyle(.bordered)
                    .tint(SMColor.accent)

                    Text("Deneme yok. İstediğin zaman iptal et.")
                        .font(.caption)
                        .foregroundStyle(SMColor.textSecondary)
                        .frame(maxWidth: .infinity)

                    if toast {
                        Text("Mock satın alma başarılı — Pro açıldı")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(SMColor.accent)
                            .frame(maxWidth: .infinity)
                    }
                }
                .padding(24)
            }
            .background(SMColor.bg.ignoresSafeArea())
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Şimdi değil") { dismiss() }
                        .foregroundStyle(SMColor.textSecondary)
                }
            }
        }
    }

    private func bullet(_ text: String) -> some View {
        Text(text)
            .font(.body)
            .foregroundStyle(SMColor.textPrimary)
    }

    private func mockPurchase() {
        settings.isPro = true
        toast = true
        SoundEffects.shared.play(.applySuccess)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
            dismiss()
        }
    }
}
