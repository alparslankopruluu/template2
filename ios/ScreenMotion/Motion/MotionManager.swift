import Foundation
import CoreMotion
import Combine

/// Device tilt for interactive preview (CMMotionManager).
final class MotionManager: ObservableObject {
    @Published var tiltX: CGFloat = 0
    @Published var tiltY: CGFloat = 0

    private let manager = CMMotionManager()
    private let queue = OperationQueue()

    func start() {
        guard manager.isDeviceMotionAvailable else {
            // Simulator / missing hardware — gentle idle sway
            return
        }
        manager.deviceMotionUpdateInterval = 1.0 / 60.0
        manager.startDeviceMotionUpdates(to: queue) { [weak self] motion, _ in
            guard let attitude = motion?.attitude else { return }
            let x = CGFloat(attitude.roll / 0.6).clamped(to: -1...1)
            let y = CGFloat(-attitude.pitch / 0.6).clamped(to: -1...1)
            DispatchQueue.main.async {
                self?.tiltX = x
                self?.tiltY = y
            }
        }
    }

    func stop() {
        manager.stopDeviceMotionUpdates()
    }
}

extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self {
        min(max(self, range.lowerBound), range.upperBound)
    }
}
