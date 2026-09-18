import SwiftUI

enum AquariumScene {
    private static let fishData: [(CGFloat, CGFloat, CGFloat, Color)] = {
        var rng = SeededRNG(seed: 7)
        let colors: [Color] = [
            Color(red: 1, green: 0.42, blue: 0.42),
            Color(red: 1, green: 0.85, blue: 0.24),
            Color(red: 0.42, green: 0.8, blue: 0.47),
            Color(red: 0.3, green: 0.59, blue: 1),
            Color(red: 1, green: 0.55, blue: 0.26),
            Color(red: 0.88, green: 0.34, blue: 0.99)
        ]
        return (0..<8).map { i in
            (rng.next(), 0.15 + rng.next() * 0.55, 18 + rng.next() * 22, colors[i % colors.count])
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
                    Color(red: 0.01, green: 0.09, blue: 0.12),
                    Color(red: 0.04, green: 0.23, blue: 0.29)
                ]),
                startPoint: .zero,
                endPoint: CGPoint(x: 0, y: size.height)
            )
        )

        // Caustics
        for i in 0..<5 {
            let cx = size.width * (0.15 + CGFloat(i) * 0.18) + sin(time * 0.7 + Double(i)) * 30 + tiltX * 20
            let cy = size.height * (0.2 + 0.1 * sin(time + Double(i)))
            context.fill(
                Path(ellipseIn: CGRect(x: cx - 70, y: cy - 40, width: 140, height: 80)),
                with: .color(Color.cyan.opacity(0.08))
            )
        }

        // Plants
        let sway = tiltX * 18 + sin(time * 1.5) * 6
        for bx in [0.12, 0.35, 0.55, 0.78, 0.92] as [CGFloat] {
            var plant = Path()
            let base = CGPoint(x: bx * size.width, y: size.height * 0.9)
            plant.move(to: base)
            plant.addQuadCurve(
                to: CGPoint(x: base.x + sway * 1.4, y: base.y - 160),
                control: CGPoint(x: base.x + sway, y: base.y - 80)
            )
            context.stroke(plant, with: .color(Color(red: 0.1, green: 0.42, blue: 0.29).opacity(0.85)), lineWidth: 6)
        }

        // Sand
        var sand = Path()
        sand.move(to: CGPoint(x: 0, y: size.height))
        sand.addLine(to: CGPoint(x: 0, y: size.height * 0.88))
        var x: CGFloat = 0
        while x <= size.width {
            sand.addLine(to: CGPoint(x: x, y: size.height * 0.88 + sin(Double(x) * 0.04 + time * 0.3) * 8))
            x += 12
        }
        sand.addLine(to: CGPoint(x: size.width, y: size.height))
        sand.closeSubpath()
        context.fill(sand, with: .color(Color(red: 0.36, green: 0.29, blue: 0.16)))

        // Bubbles
        for i in 0..<30 {
            let seed = Double(i) * 1.7
            let bx = (sin(seed) * 0.5 + 0.5) * size.width + sin(time * 2 + seed) * 12 + tiltX * 20
            let by = size.height - CGFloat((time * (30 + Double(i % 5) * 10) + seed * 40).truncatingRemainder(dividingBy: Double(size.height + 40)))
            let r: CGFloat = 3 + CGFloat(i % 5) * 2
            context.stroke(Path(ellipseIn: CGRect(x: bx - r, y: by - r, width: r * 2, height: r * 2)), with: .color(Color.cyan.opacity(0.5)), lineWidth: 1.2)
        }

        // Fish approach finger
        for (idx, f) in fishData.enumerated() {
            let phase = time * 0.8 + Double(idx)
            var fx = f.0 * size.width + sin(phase) * 40 + tiltX * 50
            var fy = f.1 * size.height + cos(phase * 0.7) * 20
            if touch.isTouching, let loc = touch.location {
                let dx = loc.x - fx
                let dy = loc.y - fy
                let dist = max(hypot(dx, dy), 1)
                let pull = min(180 / dist, 1.0) * 0.55
                fx += dx * pull
                fy += dy * pull
            }
            drawFish(context: context, at: CGPoint(x: fx, y: fy), size: f.2, color: f.3, flap: time * 4 + Double(idx), facingRight: sin(phase) >= 0)
        }

        if touch.isTouching, let loc = touch.location {
            context.fill(
                Path(ellipseIn: CGRect(x: loc.x - 40, y: loc.y - 40, width: 80, height: 80)),
                with: .color(Color.yellow.opacity(0.25))
            )
            context.fill(Path(ellipseIn: CGRect(x: loc.x - 6, y: loc.y - 6, width: 12, height: 12)), with: .color(.white.opacity(0.9)))
        }
    }

    private static func drawFish(context: GraphicsContext, at p: CGPoint, size: CGFloat, color: Color, flap: TimeInterval, facingRight: Bool) {
        var ctx = context
        ctx.translateBy(x: p.x, y: p.y)
        ctx.scaleBy(x: facingRight ? 1 : -1, y: 1)
        ctx.fill(Path(ellipseIn: CGRect(x: -size, y: -size * 0.45, width: size * 1.7, height: size * 0.9)), with: .color(color))
        let wag = sin(flap * 8) * 12
        var tail = Path()
        tail.move(to: CGPoint(x: -size * 0.85, y: 0))
        tail.addLine(to: CGPoint(x: -size * 1.5, y: -size * 0.5 + wag))
        tail.addLine(to: CGPoint(x: -size * 1.5, y: size * 0.5 - wag))
        tail.closeSubpath()
        ctx.fill(tail, with: .color(color.opacity(0.85)))
        ctx.fill(Path(ellipseIn: CGRect(x: size * 0.25, y: -size * 0.2, width: size * 0.28, height: size * 0.28)), with: .color(.white))
        ctx.fill(Path(ellipseIn: CGRect(x: size * 0.35, y: -size * 0.12, width: size * 0.14, height: size * 0.14)), with: .color(.black))
    }
}
