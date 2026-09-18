import SwiftUI

struct OnboardingOverlay: View {
    let onDismiss: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.88).ignoresSafeArea()
            VStack(alignment: .leading, spacing: 16) {
                Text("Make it move")
                    .font(.title.bold())
                    .foregroundStyle(.white)
                Label("Tilt your phone to shift the scene (parallax & current).", systemImage: "gyroscope")
                    .foregroundStyle(Color(white: 0.75))
                    .fixedSize(horizontal: false, vertical: true)
                Label("Drag, swipe, and long-press for theme-specific magic.", systemImage: "hand.tap")
                    .foregroundStyle(Color(white: 0.75))
                    .fixedSize(horizontal: false, vertical: true)
                Text("On iOS, interactive wallpapers run inside the app — export a clip or follow the tutorial to set a still/Live Photo.")
                    .font(.footnote)
                    .foregroundStyle(Color(white: 0.55))
                    .padding(.top, 4)
                Button("Got it — let's play", action: onDismiss)
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(Color(red: 1, green: 0.42, blue: 0.54), in: RoundedRectangle(cornerRadius: 14))
                    .foregroundStyle(.white)
                    .padding(.top, 8)
            }
            .padding(28)
            .background(
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .fill(Color(red: 0.09, green: 0.09, blue: 0.13))
                    .overlay(
                        RoundedRectangle(cornerRadius: 24, style: .continuous)
                            .stroke(Color.white.opacity(0.08), lineWidth: 1)
                    )
            )
            .padding(28)
        }
    }
}
