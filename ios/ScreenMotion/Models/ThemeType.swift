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
        case .space: return [Color(red: 0.02, green: 0.02, blue: 0.03), Color(red: 0.07, green: 0.09, blue: 0.16)]
        case .aquarium: return [Color(red: 0.02, green: 0.08, blue: 0.09), Color(red: 0.05, green: 0.16, blue: 0.20)]
        case .vehicle: return [Color(red: 0.04, green: 0.04, blue: 0.05), Color(red: 0.09, green: 0.09, blue: 0.10)]
        case .nature: return [Color(red: 0.04, green: 0.06, blue: 0.08), Color(red: 0.08, green: 0.12, blue: 0.09)]
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
        case .sportsCar: return Color(red: 0.36, green: 0.64, blue: 0.85)
        case .truck: return Color(red: 0.83, green: 0.65, blue: 0.45)
        case .motorcycle: return Color(red: 0.77, green: 0.48, blue: 0.54)
        case .helicopter: return Color(red: 0.48, green: 0.67, blue: 0.54)
        }
    }

    var bodyColor: Color {
        switch self {
        case .sportsCar: return Color(red: 0.48, green: 0.72, blue: 0.88)
        case .truck: return Color(red: 0.77, green: 0.54, blue: 0.35)
        case .motorcycle: return Color(red: 0.66, green: 0.41, blue: 0.56)
        case .helicopter: return Color(red: 0.42, green: 0.61, blue: 0.48)
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

    /// Free: Sports only. Truck / Bike / Heli → mock Pro.
    var requiresPro: Bool {
        self != .sportsCar
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

    /// Mock Pro — UserDefaults only. No StoreKit / AdMob.
    @Published var isPro: Bool {
        didSet {
            UserDefaults.standard.set(isPro, forKey: "is_pro")
            if !isPro && selectedVehicle.requiresPro {
                selectedVehicle = .sportsCar
            }
        }
    }

    init() {
        let raw = UserDefaults.standard.string(forKey: "selected_theme") ?? ThemeType.space.rawValue
        selectedTheme = ThemeType(rawValue: raw) ?? .space
        let vRaw = UserDefaults.standard.string(forKey: "selected_vehicle") ?? VehicleType.sportsCar.rawValue
        selectedVehicle = VehicleType(rawValue: vRaw) ?? .sportsCar
        onboardingDone = UserDefaults.standard.bool(forKey: "onboarding_done")
        soundMuted = UserDefaults.standard.bool(forKey: "sound_muted")
        isPro = UserDefaults.standard.bool(forKey: "is_pro")
        SoundEffects.shared.isMuted = soundMuted
        if !isPro && selectedVehicle.requiresPro {
            selectedVehicle = .sportsCar
        }
    }
}
