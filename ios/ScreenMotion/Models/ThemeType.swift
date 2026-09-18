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
        case .vehicle: return "4 vehicles · tilt lanes · swipe boost"
        case .nature: return "Clouds · birds follow · wind · leaf mascot"
        }
    }

    var subtitleTR: String {
        switch self {
        case .space: return "Yıldız paralaksı · meteor · dost gemi"
        case .aquarium: return "Meraklı balıklar · akıntı · kabarcık"
        case .vehicle: return "4 araç · şerit · boost"
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

enum VehicleType: String, CaseIterable, Identifiable {
    case sportsCar = "Sports"
    case truck = "Truck"
    case motorcycle = "Bike"
    case helicopter = "Heli"

    var id: String { rawValue }

    var emoji: String {
        switch self {
        case .sportsCar: return "🏎️"
        case .truck: return "🚛"
        case .motorcycle: return "🏍️"
        case .helicopter: return "🚁"
        }
    }

    var displayNameTR: String {
        switch self {
        case .sportsCar: return "Spor"
        case .truck: return "Kamyon"
        case .motorcycle: return "Motor"
        case .helicopter: return "Helikopter"
        }
    }

    var trailColor: Color {
        switch self {
        case .sportsCar: return Color(red: 0, green: 0.94, blue: 1)
        case .truck: return Color(red: 1, green: 0.67, blue: 0.2)
        case .motorcycle: return Color(red: 1, green: 0.18, blue: 0.58)
        case .helicopter: return Color(red: 0.53, green: 1, blue: 0.67)
        }
    }

    var bodyColor: Color {
        switch self {
        case .sportsCar: return Color(red: 0.18, green: 0.9, blue: 1)
        case .truck: return Color(red: 1, green: 0.53, blue: 0.27)
        case .motorcycle: return Color(red: 0.88, green: 0.25, blue: 0.98)
        case .helicopter: return Color(red: 0.4, green: 0.93, blue: 0.6)
        }
    }

    var boostMult: CGFloat {
        switch self {
        case .sportsCar: return 3.2
        case .truck: return 2.4
        case .motorcycle: return 4.0
        case .helicopter: return 2.8
        }
    }
}

final class AppSettings: ObservableObject {
    @Published var selectedTheme: ThemeType {
        didSet { UserDefaults.standard.set(selectedTheme.rawValue, forKey: "selected_theme") }
    }
    @Published var selectedVehicle: VehicleType {
        didSet { UserDefaults.standard.set(selectedVehicle.rawValue, forKey: "selected_vehicle") }
    }
    @Published var onboardingDone: Bool {
        didSet { UserDefaults.standard.set(onboardingDone, forKey: "onboarding_done") }
    }
    @Published var soundMuted: Bool {
        didSet {
            UserDefaults.standard.set(soundMuted, forKey: "sound_muted")
            SoundEffects.shared.isMuted = soundMuted
        }
    }

    init() {
        let raw = UserDefaults.standard.string(forKey: "selected_theme") ?? ThemeType.space.rawValue
        selectedTheme = ThemeType(rawValue: raw) ?? .space
        let vRaw = UserDefaults.standard.string(forKey: "selected_vehicle") ?? VehicleType.sportsCar.rawValue
        selectedVehicle = VehicleType(rawValue: vRaw) ?? .sportsCar
        onboardingDone = UserDefaults.standard.bool(forKey: "onboarding_done")
        soundMuted = UserDefaults.standard.bool(forKey: "sound_muted")
        SoundEffects.shared.isMuted = soundMuted
    }
}
