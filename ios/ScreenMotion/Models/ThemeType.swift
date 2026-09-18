import Foundation
import SwiftUI

enum ThemeType: String, CaseIterable, Identifiable {
    case space = "Space"
    case aquarium = "Aquarium"
    case vehicle = "Vehicle"

    var id: String { rawValue }

    var emoji: String {
        switch self {
        case .space: return "🚀"
        case .aquarium: return "🐠"
        case .vehicle: return "🏎️"
        }
    }

    var subtitle: String {
        switch self {
        case .space: return "Parallax stars · swipe meteors · long-press ship"
        case .aquarium: return "Fish follow · tilt current · bubbles"
        case .vehicle: return "Tilt lanes · finger drive · swipe boost"
        }
    }

    var gradient: [Color] {
        switch self {
        case .space: return [Color(red: 0.02, green: 0.02, blue: 0.06), Color(red: 0.10, green: 0.04, blue: 0.18)]
        case .aquarium: return [Color(red: 0.01, green: 0.09, blue: 0.12), Color(red: 0.04, green: 0.23, blue: 0.29)]
        case .vehicle: return [Color(red: 0.05, green: 0.05, blue: 0.07), Color(red: 0.10, green: 0.08, blue: 0.12)]
        }
    }
}

final class AppSettings: ObservableObject {
    @Published var selectedTheme: ThemeType {
        didSet { UserDefaults.standard.set(selectedTheme.rawValue, forKey: "selected_theme") }
    }
    @Published var onboardingDone: Bool {
        didSet { UserDefaults.standard.set(onboardingDone, forKey: "onboarding_done") }
    }

    init() {
        let raw = UserDefaults.standard.string(forKey: "selected_theme") ?? ThemeType.space.rawValue
        selectedTheme = ThemeType(rawValue: raw) ?? .space
        onboardingDone = UserDefaults.standard.bool(forKey: "onboarding_done")
    }
}
