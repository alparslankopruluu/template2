import SwiftUI

/// Store-listing style showcase: hero, features, theme gallery (EN+TR).
struct ShowcaseView: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    heroCard
                    featuresCard
                    gallerySection
                    Text("Store preview · v4 · Apple-inspired UI")
                        .font(.caption2)
                        .foregroundStyle(SMColor.textSecondary)
                        .frame(maxWidth: .infinity)
                        .padding(.bottom, 24)
                }
                .padding(20)
            }
            .background(SMColor.bg.ignoresSafeArea())
            .navigationTitle("Showcase · Vitrin")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Close · Kapat") { dismiss() }
                        .foregroundStyle(SMColor.accent)
                }
            }
        }
    }

    private var heroCard: some View {
        VStack(spacing: 12) {
            Text("✨").font(.system(size: 42))
            Text("ScreenMotion")
                .font(.system(size: 30, weight: .bold, design: .rounded))
                .foregroundStyle(SMColor.textPrimary)
            Text("Tilt. Touch. Play.\nCinematic live wallpapers that react to you.")
                .multilineTextAlignment(.center)
                .font(.subheadline)
                .foregroundStyle(SMColor.textSecondary)
            Text("Eğ. Dokun. Oyna.\nSana tepki veren sinematik canlı duvar kağıtları.")
                .multilineTextAlignment(.center)
                .font(.caption)
                .foregroundStyle(SMColor.accent)
        }
        .frame(maxWidth: .infinity)
        .padding(28)
        .background(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .fill(SMColor.card)
                .overlay(
                    RoundedRectangle(cornerRadius: 24, style: .continuous)
                        .stroke(SMColor.stroke, lineWidth: 1)
                )
        )
    }

    private var featuresCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Highlights · Öne çıkanlar")
                .font(.headline)
                .foregroundStyle(.white)
            featureRow("📱", "Gyro & touch — parallax layers follow every tilt and drag.")
            featureRow("🎨", "Four themes — Space, Aquarium, Vehicle, Nature.")
            featureRow("✨", "Expressive mascots — fish, ship, vehicles, birds & leaf.")
            featureRow("🔊", "Playful SFX — mute anytime; boost, meteors, bubbles & more.")
            featureRow("🏎️", "Vehicle picker — sports, truck, bike, helicopter.")
        }
        .padding(18)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(SMColor.card)
                .overlay(
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .stroke(SMColor.stroke, lineWidth: 1)
                )
        )
    }

    private func featureRow(_ emoji: String, _ text: String) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Text(emoji)
            Text(text)
                .font(.subheadline)
                .foregroundStyle(Color(red: 0.65, green: 0.68, blue: 0.8))
        }
    }

    private var gallerySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Theme gallery · Tema galerisi")
                .font(.headline)
                .foregroundStyle(.white)
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                ForEach(ThemeType.allCases) { theme in
                    VStack(spacing: 8) {
                        ZStack {
                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                .fill(LinearGradient(colors: theme.gradient, startPoint: .topLeading, endPoint: .bottomTrailing))
                                .frame(height: 72)
                            Text(theme.emoji).font(.largeTitle)
                        }
                        Text(theme.rawValue)
                            .font(.caption.weight(.bold))
                            .foregroundStyle(.white)
                        Text(theme.subtitle)
                            .font(.system(size: 9))
                            .foregroundStyle(Color(red: 0.65, green: 0.68, blue: 0.8))
                            .multilineTextAlignment(.center)
                            .lineLimit(3)
                    }
                    .padding(12)
                    .background(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(SMColor.card)
                            .overlay(
                                RoundedRectangle(cornerRadius: 16, style: .continuous)
                                    .stroke(SMColor.stroke, lineWidth: 1)
                            )
                    )
                }
            }
        }
    }
}
