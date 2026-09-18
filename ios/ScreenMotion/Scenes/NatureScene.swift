import SwiftUI

enum NatureScene {
    private static let cloudSeed: [(CGFloat, CGFloat, CGFloat, CGFloat, CGFloat)] = {
        var rng = SeededRNG(seed: 21)
        return (0..<7).map { _ in
            (rng.next(), 0.08 + rng.next() * 0.28, 0.55 + rng.next() * 0.9, 0.012 + rng.next() * 0.028, 0.35 + rng.next() * 0.55)
        }
    }()

    private static let birdSeed: [(CGFloat, CGFloat, CGFloat, Color)] = {
        var rng = SeededRNG(seed: 33)
        let colors: [Color] = [
            Color(red: 0.91, green: 0.94, blue: 1),
            Color(red: 1, green: 0.84, blue: 0.65),
            Color(red: 0.72, green: 0.88, blue: 0.82),
            Color(red: 1, green: 0.71, blue: 0.78)
        ]
        return (0..<6).map { i in
            (rng.next(), 0.18 + rng.next() * 0.35, 10 + rng.next() * 8, colors[i % colors.count])
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
                    Color(red: 0.04, green: 0.07, blue: 0.09),
                    Color(red: 0.10, green: 0.18, blue: 0.14)
                ]),
                startPoint: .zero,
                endPoint: CGPoint(x: 0, y: size.height)
            )
        )

        // Soft sun glow
        let sunX = size.width * 0.78 + tiltX * 18
        let sunY = size.height * 0.22 + tiltY * 10
        context.fill(
            Path(ellipseIn: CGRect(x: sunX - 90, y: sunY - 90, width: 180, height: 180)),
            with: .radialGradient(
                Gradient(colors: [Color(red: 1, green: 0.78, blue: 0.47).opacity(0.28), .clear]),
                center: CGPoint(x: sunX, y: sunY),
                startRadius: 0,
                endRadius: 90
            )
        )
        context.fill(
            Path(ellipseIn: CGRect(x: sunX - 18, y: sunY - 18, width: 36, height: 36)),
            with: .color(Color(red: 1, green: 0.88, blue: 0.63).opacity(0.85))
        )

        drawHills(context: context, size: size, time: time, tiltX: tiltX, depth: 0)
        drawTrees(context: context, size: size, time: time, tiltX: tiltX, far: true)
        drawClouds(context: context, size: size, time: time, tiltX: tiltX, tiltY: tiltY)
        drawHills(context: context, size: size, time: time, tiltX: tiltX, depth: 1)
        drawTrees(context: context, size: size, time: time, tiltX: tiltX, far: false)
        drawWind(context: context, size: size, time: time, tiltX: tiltX)
        drawHills(context: context, size: size, time: time, tiltX: tiltX, depth: 2)

        // Birds follow drag
        for (idx, b) in birdSeed.enumerated() {
            let phase = time * 0.9 + Double(idx)
            var bx = b.0 * size.width + sin(phase) * 50 + tiltX * 40
            var by = b.1 * size.height + cos(phase * 0.7) * 18
            if touch.isTouching, let loc = touch.location {
                let dx = loc.x - bx
                let dy = loc.y - by
                let dist = max(hypot(dx, dy), 1)
                let pull = min(200 / dist, 1.0) * 0.6
                bx += dx * pull
                by += dy * pull
            }
            drawBird(context: context, at: CGPoint(x: bx, y: by), size: b.2, color: b.3, wing: time * 5 + Double(idx), facingRight: sin(phase) >= 0)
        }

        // Leaf mascot
        let idleX = size.width * (0.55 + sin(time * 0.55) * 0.12) + tiltX * 30
        let idleY = size.height * (0.38 + cos(time * 0.4) * 0.06)
        let leafPos: CGPoint
        if touch.isTouching, let loc = touch.location {
            leafPos = CGPoint(
                x: loc.x * 0.55 + idleX * 0.45,
                y: loc.y * 0.55 + idleY * 0.45
            )
        } else {
            leafPos = CGPoint(x: idleX, y: idleY)
        }
        drawLeaf(context: context, at: leafPos, time: time, tiltX: tiltX)

        if touch.isTouching, let loc = touch.location {
            context.fill(
                Path(ellipseIn: CGRect(x: loc.x - 40, y: loc.y - 40, width: 80, height: 80)),
                with: .color(Color(red: 0.66, green: 0.9, blue: 0.81).opacity(0.25))
            )
        }
    }

    private static func drawHills(context: GraphicsContext, size: CGSize, time: TimeInterval, tiltX: CGFloat, depth: Int) {
        let parallax: CGFloat = depth == 0 ? tiltX * 12 : (depth == 1 ? tiltX * 28 : tiltX * 48)
        let baseY = size.height * (depth == 0 ? 0.58 : (depth == 1 ? 0.68 : 0.82))
        let color: Color = {
            switch depth {
            case 0: return Color(red: 0.10, green: 0.18, blue: 0.16)
            case 1: return Color(red: 0.14, green: 0.25, blue: 0.20)
            default: return Color(red: 0.18, green: 0.33, blue: 0.25)
            }
        }()
        let amp: CGFloat = depth == 0 ? 28 : (depth == 1 ? 40 : 22)
        let freq: CGFloat = depth == 0 ? 0.008 : (depth == 1 ? 0.012 : 0.018)
        var hill = Path()
        hill.move(to: CGPoint(x: -40 + parallax, y: size.height))
        hill.addLine(to: CGPoint(x: -40 + parallax, y: baseY))
        var x: CGFloat = -40
        while x <= size.width + 40 {
            let y = baseY + sin(Double((x + parallax) * freq) + time * 0.15 + Double(depth)) * amp
            hill.addLine(to: CGPoint(x: x + parallax, y: y))
            x += 16
        }
        hill.addLine(to: CGPoint(x: size.width + 40 + parallax, y: size.height))
        hill.closeSubpath()
        context.fill(hill, with: .color(color))
    }

    private static func drawTrees(context: GraphicsContext, size: CGSize, time: TimeInterval, tiltX: CGFloat, far: Bool) {
        let count = far ? 9 : 6
        let depthScale: CGFloat = far ? 0.55 : 1.0
        let baseNorm: CGFloat = far ? 0.62 : 0.78
        let px = tiltX * (far ? 12 : 22)
        for i in 0..<count {
            let tx = (0.08 + CGFloat(i) / CGFloat(count) * 0.85) * size.width + px
            let th = size.height * (far ? 0.14 : 0.22) * depthScale
            let baseY = size.height * baseNorm
            let sway = sin(time * 1.8 + Double(i)) * 5 * depthScale + tiltX * 6
            var trunk = Path()
            trunk.move(to: CGPoint(x: tx, y: baseY))
            trunk.addQuadCurve(
                to: CGPoint(x: tx + sway, y: baseY - th),
                control: CGPoint(x: tx + sway * 0.4, y: baseY - th * 0.5)
            )
            context.stroke(trunk, with: .color(Color(red: 0.23, green: 0.16, blue: 0.09).opacity(far ? 0.55 : 0.85)), lineWidth: 5 * depthScale)
            let cx = tx + sway
            let cy = baseY - th
            context.fill(Path(ellipseIn: CGRect(x: cx - 20 * depthScale, y: cy - 20 * depthScale, width: 40 * depthScale, height: 40 * depthScale)), with: .color(Color(red: 0.12, green: 0.42, blue: 0.27).opacity(far ? 0.55 : 0.9)))
            context.fill(Path(ellipseIn: CGRect(x: cx - 30 * depthScale, y: cy - 8 * depthScale, width: 28 * depthScale, height: 28 * depthScale)), with: .color(Color(red: 0.16, green: 0.56, blue: 0.35).opacity(0.7)))
        }
    }

    private static func drawClouds(context: GraphicsContext, size: CGSize, time: TimeInterval, tiltX: CGFloat, tiltY: CGFloat) {
        for (i, c) in cloudSeed.enumerated() {
            let drift = CGFloat(time) * c.3 * size.width * 0.15
            let cx = ((c.0 * size.width + drift).truncatingRemainder(dividingBy: size.width + 80)) - 40 + tiltX * (1.1 - c.4) * 35
            let cy = c.1 * size.height + tiltY * (1.1 - c.4) * 18
            let s = c.2 * 40
            let opacity = 0.25 + Double(c.4) * 0.35
            context.fill(Path(ellipseIn: CGRect(x: cx - s * 0.7, y: cy - s * 0.35, width: s * 1.4, height: s * 0.7)), with: .color(Color(red: 0.85, green: 0.91, blue: 0.94).opacity(opacity)))
            context.fill(Path(ellipseIn: CGRect(x: cx - s * 1.1, y: cy - s * 0.2, width: s, height: s * 0.55)), with: .color(Color(red: 0.85, green: 0.91, blue: 0.94).opacity(opacity * 0.9)))
            context.fill(Path(ellipseIn: CGRect(x: cx + s * 0.1, y: cy - s * 0.25, width: s * 0.95, height: s * 0.55)), with: .color(Color(red: 0.85, green: 0.91, blue: 0.94).opacity(opacity * 0.9)))
            _ = i
        }
    }

    private static func drawWind(context: GraphicsContext, size: CGSize, time: TimeInterval, tiltX: CGFloat) {
        for i in 0..<40 {
            let seed = Double(i) * 1.9
            let x = ((sin(seed) * 0.5 + 0.5) * size.width + CGFloat(time * (40 + Double(i % 5) * 12)) + tiltX * 50)
                .truncatingRemainder(dividingBy: size.width + 20)
            let y = (cos(seed * 0.6) * 0.5 + 0.5) * size.height
            var streak = Path()
            streak.move(to: CGPoint(x: x, y: y))
            streak.addLine(to: CGPoint(x: x + 10, y: y + sin(time * 4 + seed) * 2))
            context.stroke(streak, with: .color(Color(red: 0.78, green: 1, blue: 0.88).opacity(0.35)), lineWidth: 1.2)
        }
    }

    private static func drawBird(context: GraphicsContext, at p: CGPoint, size: CGFloat, color: Color, wing: TimeInterval, facingRight: Bool) {
        var ctx = context
        ctx.translateBy(x: p.x, y: p.y)
        ctx.scaleBy(x: facingRight ? 1 : -1, y: 1)
        let flap = sin(wing * 2.4)
        var left = Path()
        left.move(to: .zero)
        left.addQuadCurve(to: CGPoint(x: -size * 1.6, y: -size * 0.15 * flap), control: CGPoint(x: -size * 0.9, y: -size * (0.7 + flap * 0.55)))
        ctx.stroke(left, with: .color(color), lineWidth: 2.4)
        var right = Path()
        right.move(to: .zero)
        right.addQuadCurve(to: CGPoint(x: size * 1.6, y: -size * 0.15 * flap), control: CGPoint(x: size * 0.9, y: -size * (0.7 + flap * 0.55)))
        ctx.stroke(right, with: .color(color), lineWidth: 2.4)
        ctx.fill(Path(ellipseIn: CGRect(x: -size * 0.55, y: -size * 0.28, width: size * 1.2, height: size * 0.56)), with: .color(color))
        ctx.fill(Path(ellipseIn: CGRect(x: size * 0.2, y: -size * 0.16, width: size * 0.22, height: size * 0.22)), with: .color(.white))
        ctx.fill(Path(ellipseIn: CGRect(x: size * 0.28, y: -size * 0.1, width: size * 0.1, height: size * 0.1)), with: .color(.black))
    }

    private static func drawLeaf(context: GraphicsContext, at p: CGPoint, time: TimeInterval, tiltX: CGFloat) {
        var ctx = context
        ctx.translateBy(x: p.x, y: p.y)
        ctx.rotate(by: .radians(tiltX * 0.15 + sin(time * 2.2) * 0.25))
        let pulse = 1.05 + sin(time * 1.1) * 0.04
        ctx.scaleBy(x: pulse, y: pulse)
        ctx.fill(
            Path(ellipseIn: CGRect(x: -30, y: -30, width: 60, height: 60)),
            with: .color(Color(red: 0.49, green: 1, blue: 0.7).opacity(0.2))
        )
        var leaf = Path()
        leaf.move(to: CGPoint(x: 0, y: -28))
        leaf.addCurve(to: CGPoint(x: 0, y: 28), control1: CGPoint(x: 22, y: -18), control2: CGPoint(x: 26, y: 10))
        leaf.addCurve(to: CGPoint(x: 0, y: -28), control1: CGPoint(x: -26, y: 10), control2: CGPoint(x: -22, y: -18))
        leaf.closeSubpath()
        ctx.fill(leaf, with: .color(Color(red: 0.24, green: 0.86, blue: 0.52)))
        var vein = Path()
        vein.move(to: CGPoint(x: 0, y: -22))
        vein.addLine(to: CGPoint(x: 0, y: 22))
        ctx.stroke(vein, with: .color(Color(red: 0.1, green: 0.42, blue: 0.25)), lineWidth: 2)
        ctx.fill(Path(ellipseIn: CGRect(x: -11, y: -8, width: 9, height: 9)), with: .color(.white))
        ctx.fill(Path(ellipseIn: CGRect(x: 2, y: -8, width: 9, height: 9)), with: .color(.white))
        ctx.fill(Path(ellipseIn: CGRect(x: -8, y: -6, width: 4, height: 4)), with: .color(.black))
        ctx.fill(Path(ellipseIn: CGRect(x: 5, y: -6, width: 4, height: 4)), with: .color(.black))
        var smile = Path()
        smile.move(to: CGPoint(x: -5, y: 6))
        smile.addQuadCurve(to: CGPoint(x: 5, y: 6), control: CGPoint(x: 0, y: 11))
        ctx.stroke(smile, with: .color(.black.opacity(0.7)), lineWidth: 1.6)
    }
}
