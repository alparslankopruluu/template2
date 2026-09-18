import SwiftUI

enum AquariumScene {
    private static let fishData: [(CGFloat, CGFloat, CGFloat, Color)] = {
        var rng = SeededRNG(seed: 7)
        let colors: [Color] = [
            Color(red: 0.82, green: 0.50, blue: 0.50),
            Color(red: 0.83, green: 0.72, blue: 0.42),
            Color(red: 0.42, green: 0.61, blue: 0.48),
            Color(red: 0.36, green: 0.54, blue: 0.72),
            Color(red: 0.77, green: 0.54, blue: 0.35),
            Color(red: 0.54, green: 0.48, blue: 0.67),
            Color(red: 0.42, green: 0.61, blue: 0.60),
            Color(red: 0.69, green: 0.53, blue: 0.63)
        ]
        return (0..<10).map { i in
            (rng.next(), 0.15 + rng.next() * 0.55, 16 + rng.next() * 26, colors[i % colors.count])
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

        for i in 0..<6 {
            let cx = size.width * (0.12 + CGFloat(i) * 0.16) + sin(time * 0.7 + Double(i)) * 34 + tiltX * 22
            let cy = size.height * (0.18 + 0.1 * sin(time + Double(i)))
            context.fill(
                Path(ellipseIn: CGRect(x: cx - 80, y: cy - 45, width: 160, height: 90)),
                with: .color(Color.cyan.opacity(0.09))
            )
        }

        let sway = tiltX * 20 + sin(time * 1.5) * 7
        for bx in [0.1, 0.28, 0.45, 0.62, 0.78, 0.92] as [CGFloat] {
            var plant = Path()
            let base = CGPoint(x: bx * size.width, y: size.height * 0.9)
            plant.move(to: base)
            plant.addQuadCurve(
                to: CGPoint(x: base.x + sway * 1.5, y: base.y - 175),
                control: CGPoint(x: base.x + sway, y: base.y - 90)
            )
            context.stroke(plant, with: .color(Color(red: 0.1, green: 0.42, blue: 0.29).opacity(0.85)), lineWidth: 6)
        }

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

        for i in 0..<42 {
            let seed = Double(i) * 1.7
            let bx = (sin(seed) * 0.5 + 0.5) * size.width + sin(time * 2 + seed) * 14 + tiltX * 22
            let by = size.height - CGFloat((time * (28 + Double(i % 5) * 10) + seed * 40).truncatingRemainder(dividingBy: Double(size.height + 40)))
            let r: CGFloat = 2.5 + CGFloat(i % 5) * 2
            context.stroke(Path(ellipseIn: CGRect(x: bx - r, y: by - r, width: r * 2, height: r * 2)), with: .color(Color.cyan.opacity(0.55)), lineWidth: 1.2)
        }

        // School centroid
        var sumX: CGFloat = 0
        var sumY: CGFloat = 0
        var positions: [CGPoint] = []
        for (idx, f) in fishData.enumerated() {
            let phase = time * 0.8 + Double(idx)
            var fx = f.0 * size.width + sin(phase) * 40 + tiltX * 55
            var fy = f.1 * size.height + cos(phase * 0.7) * 20
            if touch.isTouching, let loc = touch.location {
                let dx = loc.x - fx
                let dy = loc.y - fy
                let dist = max(hypot(dx, dy), 1)
                let pull = min(200 / dist, 1.0) * 0.6
                fx += dx * pull
                fy += dy * pull
            } else {
                // mild idle pull toward school center will be applied after first pass — approximate mid
                fx += sin(phase * 0.5) * 8
            }
            positions.append(CGPoint(x: fx, y: fy))
            sumX += fx; sumY += fy
        }
        let cx = sumX / CGFloat(max(positions.count, 1))
        let cy = sumY / CGFloat(max(positions.count, 1))

        for (idx, f) in fishData.enumerated() {
            var p = positions[idx]
            if !touch.isTouching {
                p.x += (cx - p.x) * 0.08
                p.y += (cy - p.y) * 0.06
            }
            let excited = touch.isTouching
            drawFish(
                context: context,
                at: p,
                size: f.2,
                color: f.3,
                flap: time * 4.5 + Double(idx),
                facingRight: p.x >= (idx > 0 ? positions[idx - 1].x : p.x - 1),
                excited: excited
            )
        }

        if touch.isTouching, let loc = touch.location {
            context.fill(
                Path(ellipseIn: CGRect(x: loc.x - 45, y: loc.y - 45, width: 90, height: 90)),
                with: .color(Color.yellow.opacity(0.28))
            )
            context.fill(Path(ellipseIn: CGRect(x: loc.x - 7, y: loc.y - 7, width: 14, height: 14)), with: .color(.white.opacity(0.9)))
        }
    }

    private static func drawFish(context: GraphicsContext, at p: CGPoint, size: CGFloat, color: Color, flap: TimeInterval, facingRight: Bool, excited: Bool) {
        var ctx = context
        ctx.translateBy(x: p.x, y: p.y)
        ctx.scaleBy(x: facingRight ? 1 : -1, y: 1)
        ctx.fill(
            Path(ellipseIn: CGRect(x: -size * 1.1, y: -size * 0.7, width: size * 2.2, height: size * 1.4)),
            with: .color(color.opacity(0.15))
        )
        ctx.fill(Path(ellipseIn: CGRect(x: -size * 0.95, y: -size * 0.52, width: size * 1.7, height: size * 1.04)), with: .color(color))
        ctx.fill(Path(ellipseIn: CGRect(x: -size * 0.35, y: size * 0.05, width: size * 0.8, height: size * 0.35)), with: .color(.white.opacity(0.22)))
        let wag = sin(flap * 9) * (14 + (excited ? 6 : 0))
        var tail = Path()
        tail.move(to: CGPoint(x: -size * 0.85, y: 0))
        tail.addLine(to: CGPoint(x: -size * 1.55, y: -size * 0.55 + wag))
        tail.addLine(to: CGPoint(x: -size * 1.35, y: 0))
        tail.addLine(to: CGPoint(x: -size * 1.55, y: size * 0.55 - wag))
        tail.closeSubpath()
        ctx.fill(tail, with: .color(color.opacity(0.9)))
        var fin = Path()
        fin.move(to: CGPoint(x: -size * 0.1, y: -size * 0.4))
        fin.addLine(to: CGPoint(x: size * 0.15, y: -size * 0.95 + sin(flap * 6) * 4))
        fin.addLine(to: CGPoint(x: size * 0.4, y: -size * 0.35))
        fin.closeSubpath()
        ctx.fill(fin, with: .color(color.opacity(0.7)))
        if excited {
            ctx.fill(Path(ellipseIn: CGRect(x: size * 0.05, y: size * 0.08, width: size * 0.22, height: size * 0.18)), with: .color(Color.pink.opacity(0.55)))
        }
        ctx.fill(Path(ellipseIn: CGRect(x: size * 0.28, y: -size * 0.22, width: size * 0.28, height: size * 0.28)), with: .color(.white))
        ctx.fill(Path(ellipseIn: CGRect(x: size * 0.38, y: -size * 0.12, width: size * 0.12, height: size * 0.12)), with: .color(.black))
        ctx.fill(Path(ellipseIn: CGRect(x: size * 0.42, y: -size * 0.14, width: size * 0.04, height: size * 0.04)), with: .color(.white))
        var smile = Path()
        smile.move(to: CGPoint(x: size * 0.55, y: size * 0.12))
        smile.addQuadCurve(to: CGPoint(x: size * 0.72, y: size * 0.06), control: CGPoint(x: size * 0.68, y: size * (excited ? 0.22 : 0.16)))
        ctx.stroke(smile, with: .color(.black.opacity(0.55)), lineWidth: 1.5)
    }
}
