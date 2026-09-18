import SwiftUI

enum SpaceScene {
    private static let starSeed: [(CGFloat, CGFloat, CGFloat, CGFloat, CGFloat)] = {
        var rng = SeededRNG(seed: 42)
        return (0..<130).map { _ in
            (rng.next(), rng.next(), rng.next() * 0.9 + 0.1, rng.next() * 2.8 + 0.6, rng.next() * 6.28)
        }
    }()

    static func draw(
        context: GraphicsContext,
        size: CGSize,
        time: TimeInterval,
        tiltX: CGFloat,
        tiltY: CGFloat,
        touch: TouchState
    ) {
        let rect = CGRect(origin: .zero, size: size)
        // Background
        context.fill(
            Path(rect),
            with: .linearGradient(
                Gradient(colors: [
                    Color(red: 0.02, green: 0.02, blue: 0.06),
                    Color(red: 0.10, green: 0.04, blue: 0.18)
                ]),
                startPoint: .zero,
                endPoint: CGPoint(x: 0, y: size.height)
            )
        )

        // Nebula
        var nebula = context
        nebula.addFilter(.blur(radius: 40))
        nebula.fill(
            Path(ellipseIn: CGRect(x: size.width * 0.4 + tiltX * 30, y: size.height * 0.15 + tiltY * 20, width: size.width * 0.5, height: size.width * 0.45)),
            with: .color(Color(red: 0.29, green: 0.12, blue: 0.5).opacity(0.35))
        )

        for s in starSeed {
            let depth = s.2
            let ox = tiltX * (1.2 - depth) * 55
            let oy = tiltY * (1.2 - depth) * 55
            let sx = s.0 * size.width + ox
            let sy = s.1 * size.height + oy
            let tw = 0.55 + 0.45 * sin(time * Double(s.3) + Double(s.4))
            let alpha = 0.35 + 0.65 * tw * Double(depth)
            let r = s.3 * (0.5 + depth)
            context.fill(
                Path(ellipseIn: CGRect(x: sx - r, y: sy - r, width: r * 2, height: r * 2)),
                with: .color(.white.opacity(alpha))
            )
        }

        // Swipe meteor trail hint
        if abs(touch.swipe.dx) + abs(touch.swipe.dy) > 400, let loc = touch.location ?? touch.lastLocation {
            let ang = atan2(touch.swipe.dy, touch.swipe.dx)
            var path = Path()
            path.move(to: loc)
            path.addLine(to: CGPoint(x: loc.x - cos(ang) * 70, y: loc.y - sin(ang) * 70))
            context.stroke(path, with: .color(Color(red: 1, green: 0.9, blue: 0.6)), lineWidth: 3)
            context.fill(Path(ellipseIn: CGRect(x: loc.x - 4, y: loc.y - 4, width: 8, height: 8)), with: .color(.white))
        }

        // Ambient meteors
        for i in 0..<3 {
            let phase = time * (0.35 + Double(i) * 0.1) + Double(i) * 2.1
            let frac = phase.truncatingRemainder(dividingBy: 1.0)
            let mx = size.width * (0.1 + 0.3 * CGFloat(i)) + CGFloat(frac) * size.width * 0.4
            let my = CGFloat(frac) * size.height * 1.1 - 40
            var trail = Path()
            trail.move(to: CGPoint(x: mx, y: my))
            trail.addLine(to: CGPoint(x: mx - 28, y: my - 48))
            context.stroke(trail, with: .color(Color.orange.opacity(0.7)), lineWidth: 2.5)
        }

        // Long-press spaceship
        if touch.isLongPress, let loc = touch.location {
            drawShip(context: context, at: loc, time: time)
        }

        if touch.isTouching, let loc = touch.location {
            context.fill(
                Path(ellipseIn: CGRect(x: loc.x - 50, y: loc.y - 50, width: 100, height: 100)),
                with: .radialGradient(
                    Gradient(colors: [Color.blue.opacity(0.35), .clear]),
                    center: loc,
                    startRadius: 0,
                    endRadius: 50
                )
            )
        }
    }

    private static func drawShip(context: GraphicsContext, at point: CGPoint, time: TimeInterval) {
        let bob = sin(time * 3) * 4
        var ctx = context
        ctx.translateBy(x: point.x, y: point.y + bob)
        // Glow
        ctx.fill(
            Path(ellipseIn: CGRect(x: -20, y: 10, width: 40, height: 40)),
            with: .radialGradient(
                Gradient(colors: [Color.cyan.opacity(0.6), .clear]),
                center: CGPoint(x: 0, y: 30),
                startRadius: 0,
                endRadius: 28
            )
        )
        var body = Path()
        body.move(to: CGPoint(x: 0, y: -36))
        body.addLine(to: CGPoint(x: 22, y: 18))
        body.addLine(to: CGPoint(x: 8, y: 12))
        body.addLine(to: CGPoint(x: 0, y: 22))
        body.addLine(to: CGPoint(x: -8, y: 12))
        body.addLine(to: CGPoint(x: -22, y: 18))
        body.closeSubpath()
        ctx.fill(body, with: .color(Color(red: 0.91, green: 0.93, blue: 1.0)))
        ctx.fill(Path(ellipseIn: CGRect(x: -6, y: -10, width: 12, height: 12)), with: .color(Color(red: 0.42, green: 0.69, blue: 1.0)))
    }
}

/// Tiny deterministic RNG for stable starfield.
struct SeededRNG {
    private var state: UInt64
    init(seed: UInt64) { state = seed == 0 ? 1 : seed }
    mutating func next() -> CGFloat {
        state = state &* 6364136223846793005 &+ 1
        return CGFloat((state >> 33) % 10_000) / 10_000
    }
}
