import SwiftUI

struct TouchState {
    var location: CGPoint? = nil
    var isTouching: Bool = false
    var isLongPress: Bool = false
    var swipe: CGVector = .zero
    var boost: Bool = false
    var downTime: Date? = nil
    var downLocation: CGPoint? = nil
    var lastLocation: CGPoint? = nil
    var lastTime: Date? = nil

    mutating func begin(at point: CGPoint) {
        location = point
        isTouching = true
        isLongPress = false
        boost = false
        swipe = .zero
        downTime = Date()
        downLocation = point
        lastLocation = point
        lastTime = Date()
    }

    mutating func move(to point: CGPoint) {
        let now = Date()
        if let last = lastLocation, let t = lastTime {
            let dt = max(now.timeIntervalSince(t), 0.001)
            let vx = (point.x - last.x) / dt
            let vy = (point.y - last.y) / dt
            let speed = hypot(vx, vy)
            if speed > 1800 {
                boost = true
                swipe = CGVector(dx: vx, dy: vy)
            } else if speed > 600 {
                swipe = CGVector(dx: vx, dy: vy)
            }
        }
        location = point
        lastLocation = point
        lastTime = now
        if let down = downTime, let origin = downLocation {
            let held = now.timeIntervalSince(down)
            let dist = hypot(point.x - origin.x, point.y - origin.y)
            if held > 0.45 && dist < 40 {
                isLongPress = true
            }
        }
    }

    mutating func end(at point: CGPoint) {
        move(to: point)
        isTouching = false
        isLongPress = false
        location = nil
    }

    mutating func clearSwipe() {
        swipe = .zero
    }
}
