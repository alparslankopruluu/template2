import SwiftUI

struct SceneCanvasView: View {
    let theme: ThemeType
    @ObservedObject var motion: MotionManager
    @Binding var touch: TouchState

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
                    VehicleScene.draw(context: context, size: size, time: t, tiltX: motion.tiltX, tiltY: motion.tiltY, touch: touch)
                case .nature:
                    NatureScene.draw(context: context, size: size, time: t, tiltX: motion.tiltX, tiltY: motion.tiltY, touch: touch)
                }
            }
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { value in
                        if !touch.isTouching {
                            touch.begin(at: value.location)
                        } else {
                            touch.move(to: value.location)
                        }
                    }
                    .onEnded { value in
                        touch.end(at: value.location)
                    }
            )
        }
    }
}
