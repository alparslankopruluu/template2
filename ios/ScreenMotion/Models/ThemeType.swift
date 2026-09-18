import Foundation
import SwiftUI

enum ThemeType: String, CaseIterable, Identifiable {
    case space = "Space"
    case aquarium = "Aquarium"
    case vehicle = "Vehicle"
    case nature = "Nature"

    var id: String { rawValue }

    var emoji: String {
        switch self {
        case .space: return "🚀"
        case .aquarium: return "🐠"
        case .vehicle: return "🏎️"
        case .nature: return "🌿"
        }
    }

    var subtitle: String {
        switch self {
        case .space: return "Parallax stars · swipe meteors · friendly ship"
        case .aquarium: return "Curious fish · tilt current · bubbles"
        case .vehicle: return "Neon drive · tilt lanes · swipe boost"
        case .nature: return "Clouds · birds follow · wind · leaf mascot"
        }
    }

    var subtitleTR: String {
        switch self {
        case .space: return "Yıldız paralaksı · meteor · dost gemi"
        case .aquarium: return "Meraklı balıklar · akıntı · kabarcık"
        case .vehicle: return "Neon sürüş · şerit · boost"
        case .nature: return "Bulut · kuşlar · rüzgar · yaprak"
        }
    }

    var gradient: [Color] {
        switch self {
        case .space: return [Color(red: 0.02, green: 0.02, blue: 0.06), Color(red: 0.10, green: 0.04, blue: 0.18)]
        case .aquarium: return [Color(red: 0.01, green: 0.09, blue: 0.12), Color(red: 0.04, green: 0.23, blue: 0.29)]
        case .vehicle: return [Color(red: 0.05, green: 0.05, blue: 0.07), Color(red: 0.10, green: 0.08, blue: 0.12)]
        case .nature: return [Color(red: 0.04, green: 0.07, blue: 0.09), Color(red: 0.10, green: 0.18, blue: 0.14)]
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
