import SwiftUI

enum SpaceScene {
    private static let starSeed: [(CGFloat, CGFloat, CGFloat, CGFloat, CGFloat)] = {
        var rng = SeededRNG(seed: 42)
        return (0..<170).map { _ in
            (rng.next(), rng.next(), rng.next() * 0.9 + 0.1, rng.next() * 2.8 + 0.5, rng.next() * 6.28)
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

        var nebula = context
        nebula.addFilter(.blur(radius: 40))
        nebula.fill(
            Path(ellipseIn: CGRect(x: size.width * 0.4 + tiltX * 30, y: size.height * 0.15 + tiltY * 20, width: size.width * 0.5, height: size.width * 0.45)),
            with: .color(Color(red: 0.29, green: 0.12, blue: 0.5).opacity(0.38))
        )

        // Planets (parallax accents)
        drawPlanet(context: context, at: CGPoint(x: size.width * 0.22 + tiltX * 24, y: size.height * 0.28 + tiltY * 16), r: 38, color: Color(red: 0.42, green: 0.55, blue: 1), ring: true)
        drawPlanet(context: context, at: CGPoint(x: size.width * 0.78 + tiltX * 14, y: size.height * 0.55 + tiltY * 10), r: 22, color: Color(red: 0.78, green: 0.63, blue: 0.66), ring: false)
        drawPlanet(context: context, at: CGPoint(x: size.width * 0.55 + tiltX * 30, y: size.height * 0.18 + tiltY * 20), r: 14, color: Color(red: 1, green: 0.84, blue: 0.5), ring: false)

        for s in starSeed {
            let depth = s.2
            let ox = tiltX * (1.25 - depth) * 60
            let oy = tiltY * (1.25 - depth) * 60
            let sx = s.0 * size.width + ox
            let sy = s.1 * size.height + oy
            let tw = 0.55 + 0.45 * sin(time * Double(s.3) + Double(s.4))
            let alpha = 0.3 + 0.7 * tw * Double(depth)
            let r = s.3 * (0.45 + depth)
            context.fill(
                Path(ellipseIn: CGRect(x: sx - r, y: sy - r, width: r * 2, height: r * 2)),
                with: .color(.white.opacity(alpha))
            )
        }

        if abs(touch.swipe.dx) + abs(touch.swipe.dy) > 400, let loc = touch.location ?? touch.lastLocation {
            let ang = atan2(touch.swipe.dy, touch.swipe.dx)
            var path = Path()
            path.move(to: loc)
            path.addLine(to: CGPoint(x: loc.x - cos(ang) * 70, y: loc.y - sin(ang) * 70))
            context.stroke(path, with: .color(Color(red: 1, green: 0.9, blue: 0.6)), lineWidth: 3)
            context.fill(Path(ellipseIn: CGRect(x: loc.x - 4, y: loc.y - 4, width: 8, height: 8)), with: .color(.white))
        }

        for i in 0..<4 {
            let phase = time * (0.35 + Double(i) * 0.1) + Double(i) * 2.1
            let frac = phase.truncatingRemainder(dividingBy: 1.0)
            let mx = size.width * (0.1 + 0.28 * CGFloat(i)) + CGFloat(frac) * size.width * 0.4
            let my = CGFloat(frac) * size.height * 1.1 - 40
            var trail = Path()
            trail.move(to: CGPoint(x: mx, y: my))
            trail.addLine(to: CGPoint(x: mx - 28, y: my - 48))
            context.stroke(trail, with: .color(Color.orange.opacity(0.7)), lineWidth: 2.5)
        }

        // Friendly ship — long-press follows finger; otherwise idle drift
        let shipPos: CGPoint
        if touch.isLongPress, let loc = touch.location {
            shipPos = loc
        } else {
            shipPos = CGPoint(
                x: size.width * (0.5 + sin(time * 0.35) * 0.12) + tiltX * 40,
                y: size.height * (0.68 + cos(time * 0.28) * 0.04)
            )
        }
        drawShip(context: context, at: shipPos, time: time, prominent: touch.isLongPress)

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

    private static func drawPlanet(context: GraphicsContext, at p: CGPoint, r: CGFloat, color: Color, ring: Bool) {
        context.fill(
            Path(ellipseIn: CGRect(x: p.x - r, y: p.y - r, width: r * 2, height: r * 2)),
            with: .radialGradient(
                Gradient(colors: [color.opacity(0.95), color.opacity(0.25)]),
                center: CGPoint(x: p.x - r * 0.25, y: p.y - r * 0.25),
                startRadius: 0,
                endRadius: r * 1.2
            )
        )
        context.fill(Path(ellipseIn: CGRect(x: p.x - r * 0.45, y: p.y - r * 0.45, width: r * 0.4, height: r * 0.35)), with: .color(.white.opacity(0.25)))
        if ring {
            var ringPath = Path(ellipseIn: CGRect(x: p.x - r * 1.6, y: p.y - r * 0.35, width: r * 3.2, height: r * 0.7))
            context.stroke(ringPath, with: .color(Color(red: 0.78, green: 0.85, blue: 1).opacity(0.55)), lineWidth: 2.5)
        }
    }

    private static func drawShip(context: GraphicsContext, at point: CGPoint, time: TimeInterval, prominent: Bool) {
        let bob = sin(time * 2.8) * 5
        var ctx = context
        ctx.translateBy(x: point.x, y: point.y + bob)
        let scale: CGFloat = prominent ? 1.25 : 1.0
        ctx.scaleBy(x: scale, y: scale)
        ctx.opacity = prominent ? 1 : 0.75
        ctx.fill(
            Path(ellipseIn: CGRect(x: -22, y: 8, width: 44, height: 44)),
            with: .radialGradient(
                Gradient(colors: [Color.cyan.opacity(0.65), .clear]),
                center: CGPoint(x: 0, y: 30),
                startRadius: 0,
                endRadius: 30
            )
        )
        var body = Path()
        body.move(to: CGPoint(x: 0, y: -38))
        body.addCurve(to: CGPoint(x: 20, y: 20), control1: CGPoint(x: 18, y: -20), control2: CGPoint(x: 26, y: 8))
        body.addLine(to: CGPoint(x: 8, y: 14))
        body.addLine(to: CGPoint(x: 0, y: 24))
        body.addLine(to: CGPoint(x: -8, y: 14))
        body.addLine(to: CGPoint(x: -20, y: 20))
        body.addCurve(to: CGPoint(x: 0, y: -38), control1: CGPoint(x: -26, y: 8), control2: CGPoint(x: -18, y: -20))
        body.closeSubpath()
        ctx.fill(body, with: .color(Color(red: 0.91, green: 0.93, blue: 1.0)))
        ctx.fill(Path(ellipseIn: CGRect(x: -9, y: -15, width: 18, height: 18)), with: .color(Color(red: 0.42, green: 0.69, blue: 1.0)))
        // Friendly face
        ctx.fill(Path(ellipseIn: CGRect(x: -5.5, y: -12, width: 5, height: 5)), with: .color(.white))
        ctx.fill(Path(ellipseIn: CGRect(x: 0.5, y: -12, width: 5, height: 5)), with: .color(.white))
        ctx.fill(Path(ellipseIn: CGRect(x: -4, y: -11, width: 2.5, height: 2.5)), with: .color(.black))
        ctx.fill(Path(ellipseIn: CGRect(x: 2, y: -11, width: 2.5, height: 2.5)), with: .color(.black))
        var smile = Path()
        smile.move(to: CGPoint(x: -3, y: -4))
        smile.addQuadCurve(to: CGPoint(x: 3, y: -4), control: CGPoint(x: 0, y: -1))
        ctx.stroke(smile, with: .color(.black.opacity(0.7)), lineWidth: 1.3)
        ctx.fill(Path(ellipseIn: CGRect(x: -19, y: 7, width: 7, height: 7)), with: .color(Color(red: 0.75, green: 0.50, blue: 0.56)))
        ctx.fill(Path(ellipseIn: CGRect(x: 12, y: 7, width: 7, height: 7)), with: .color(Color(red: 0.75, green: 0.50, blue: 0.56)))
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
