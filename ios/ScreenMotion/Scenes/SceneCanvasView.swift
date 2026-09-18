import SwiftUI

struct SceneCanvasView: View {
    let theme: ThemeType
    var vehicle: VehicleType = .sportsCar
    @ObservedObject var motion: MotionManager
    @Binding var touch: TouchState

    @State private var lastBoostEdge = false
    @State private var lastMeteorEdge = false
    @State private var lastTiltAbs: CGFloat = 0
    @State private var lastBubbleAt: Date = .distantPast

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 60.0, paused: false)) { timeline in
            Canvas { context, size in
                let t = timeline.date.timeIntervalSinceReferenceDate
                switch theme {
                case .space:
                    SpaceScene.draw(context: context, size: size, time: t, tiltX: motion.tiltX, tiltY: motion.tiltY, touch: touch)
                case .aquarium:
                    AquariumScene.draw(context: context, size: size, time: t, tiltX: motion.tiltX, tiltY: motion.tiltY, touch: touch)
                case .vehicle:
                    VehicleScene.draw(context: context, size: size, time: t, tiltX: motion.tiltX, tiltY: motion.tiltY, touch: touch, vehicle: vehicle)
                case .nature:
                    NatureScene.draw(context: context, size: size, time: t, tiltX: motion.tiltX, tiltY: motion.tiltY, touch: touch)
                }
            }
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { value in
                        if !touch.isTouching {
                            touch.begin(at: value.location)
                            SoundEffects.shared.play(.touchSplash)
                            if theme == .nature {
                                SoundEffects.shared.play(.birdChirp)
                            }
                        } else {
                            touch.move(to: value.location)
                        }
                        evaluateSfx()
                    }
                    .onEnded { value in
                        touch.end(at: value.location)
                        lastBoostEdge = false
                        lastMeteorEdge = false
                    }
            )
            .onReceive(Timer.publish(every: 0.25, on: .main, in: .common).autoconnect()) { _ in
                evaluateSfx()
                if theme == .aquarium, Date().timeIntervalSince(lastBubbleAt) > 1.4 {
                    lastBubbleAt = Date()
                    SoundEffects.shared.play(.bubblePop)
                }
            }
        }
    }

    private func evaluateSfx() {
        let boostNow = touch.boost || abs(touch.swipe.dx) + abs(touch.swipe.dy) > 900
        if theme == .vehicle && boostNow && !lastBoostEdge {
            SoundEffects.shared.play(.boost)
            lastBoostEdge = true
        } else if !boostNow {
            lastBoostEdge = false
        }

        let meteorNow = theme == .space && abs(touch.swipe.dx) + abs(touch.swipe.dy) > 400
        if meteorNow && !lastMeteorEdge {
            SoundEffects.shared.play(.meteor)
            lastMeteorEdge = true
        } else if !meteorNow {
            lastMeteorEdge = false
        }

        if theme == .nature {
            let tiltAbs = abs(motion.tiltX) + abs(motion.tiltY)
            if tiltAbs > 0.55 && tiltAbs > lastTiltAbs + 0.12 {
                SoundEffects.shared.play(.windWhoosh)
            }
            lastTiltAbs = tiltAbs
        }
    }
}
