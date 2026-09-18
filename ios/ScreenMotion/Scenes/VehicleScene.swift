import SwiftUI

enum VehicleScene {
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
                    Color(red: 0.05, green: 0.05, blue: 0.07),
                    Color(red: 0.16, green: 0.09, blue: 0.22)
                ]),
                startPoint: .zero,
                endPoint: CGPoint(x: 0, y: size.height * 0.5)
            )
        )

        // Far city
        for i in 0..<10 {
            let bw = size.width * (0.05 + CGFloat(i % 3) * 0.025)
            let bh = size.height * (0.1 + CGFloat((i * 29) % 28) / 120)
            let bx = CGFloat(i) / 10 * size.width + tiltX * 12
            let top = size.height * 0.48 - bh
            context.fill(
                Path(CGRect(x: bx, y: top, width: bw, height: bh + size.height * 0.04)),
                with: .color(Color(white: 0.1 + Double(i % 3) * 0.03))
            )
        }

        for i in 0..<12 {
            let bw = size.width * (0.06 + CGFloat(i % 3) * 0.03)
            let bh = size.height * (0.15 + CGFloat((i * 37) % 35) / 100)
            let bx = CGFloat(i) / 12 * size.width + tiltX * 26
            let top = size.height * 0.48 - bh
            context.fill(
                Path(CGRect(x: bx, y: top, width: bw, height: bh + size.height * 0.04)),
                with: .color(Color(white: 0.15 + Double(i % 4) * 0.05))
            )
            var wy = top + 10
            while wy < size.height * 0.48 - 8 {
                var wx = bx + 6
                while wx < bx + bw - 8 {
                    if Int(wx + wy) % 17 != 0 {
                        context.fill(Path(CGRect(x: wx, y: wy, width: 4, height: 6)), with: .color(Color.yellow.opacity(0.55)))
                    }
                    wx += 11
                }
                wy += 14
            }
        }

        var road = Path()
        road.move(to: CGPoint(x: size.width * 0.05, y: size.height))
        road.addLine(to: CGPoint(x: size.width * 0.35, y: size.height * 0.48))
        road.addLine(to: CGPoint(x: size.width * 0.65, y: size.height * 0.48))
        road.addLine(to: CGPoint(x: size.width * 0.95, y: size.height))
        road.closeSubpath()
        context.fill(road, with: .color(Color(red: 0.1, green: 0.1, blue: 0.13)))

        var left = Path(); left.move(to: CGPoint(x: size.width * 0.05, y: size.height)); left.addLine(to: CGPoint(x: size.width * 0.35, y: size.height * 0.48))
        var right = Path(); right.move(to: CGPoint(x: size.width * 0.95, y: size.height)); right.addLine(to: CGPoint(x: size.width * 0.65, y: size.height * 0.48))
        context.stroke(left, with: .color(Color(red: 0.8, green: 0.67, blue: 0.27)), lineWidth: 3)
        context.stroke(right, with: .color(Color(red: 0.8, green: 0.67, blue: 0.27)), lineWidth: 3)

        let boost = touch.boost || abs(touch.swipe.dx) + abs(touch.swipe.dy) > 900
        let speed: CGFloat = boost ? 3.2 : 1.05

        for i in 0..<12 {
            let t = (CGFloat(i) / 12 + CGFloat(time * 0.55 * Double(speed))).truncatingRemainder(dividingBy: 1.0)
            let y = size.height * 0.48 + t * size.height * 0.52
            let dashH = 8 + t * 20
            var dash = Path()
            dash.move(to: CGPoint(x: size.width * 0.5, y: y))
            dash.addLine(to: CGPoint(x: size.width * 0.5, y: y + dashH))
            context.stroke(dash, with: .color(.white.opacity(0.7)), lineWidth: 3)
        }

        if boost {
            for i in 0..<14 {
                let sx = CGFloat(i) / 14 * size.width
                let sy = CGFloat((time * 2.2 + Double(i) * 0.4).truncatingRemainder(dividingBy: 1.0)) * size.height
                var line = Path()
                line.move(to: CGPoint(x: sx, y: sy))
                line.addLine(to: CGPoint(x: sx, y: sy + 55 * speed))
                context.stroke(line, with: .color(Color.cyan.opacity(0.55)), lineWidth: 2)
            }
        }

        var lane = tiltX.clamped(to: -1...1)
        var yNorm: CGFloat = 0.72 + sin(time * 0.8) * 0.01
        if touch.isTouching, let loc = touch.location {
            lane = ((loc.x / max(size.width, 1)) * 2 - 1).clamped(to: -1...1)
            yNorm = (loc.y / max(size.height, 1)).clamped(to: 0.45...0.88)
        }
        let t = ((yNorm - 0.48) / 0.52).clamped(to: 0...1)
        let roadHalf = size.width * (0.12 + t * 0.30)
        let cx = size.width * 0.5 + lane * roadHalf * 0.7
        let cy = yNorm * size.height + sin(time * 14 * Double(speed)) * 1.8
        let scale = 0.55 + t * 0.6

        // Neon trail
        for i in 0..<8 {
            let fade = CGFloat(i) / 8
            let ty = cy + 20 + fade * 50
            context.fill(
                Path(ellipseIn: CGRect(x: cx - 14 + fade * 4, y: ty - 8, width: 28 - fade * 12, height: 16)),
                with: .color(Color(red: 0, green: 0.94, blue: 1).opacity(0.35 * (1 - fade)))
            )
            context.fill(
                Path(ellipseIn: CGRect(x: cx - 6, y: ty - 3, width: 12, height: 8)),
                with: .color(Color(red: 1, green: 0.18, blue: 0.58).opacity(0.25 * (1 - fade)))
            )
        }

        drawCar(context: context, at: CGPoint(x: cx, y: cy), scale: scale, boosting: boost)
    }

    private static func drawCar(context: GraphicsContext, at p: CGPoint, scale: CGFloat, boosting: Bool) {
        var ctx = context
        ctx.translateBy(x: p.x, y: p.y)
        ctx.scaleBy(x: scale, y: scale)
        ctx.fill(Path(ellipseIn: CGRect(x: -40, y: 20, width: 80, height: 14)), with: .color(.black.opacity(0.4)))

        // Headlight beams
        var beamL = Path()
        beamL.move(to: CGPoint(x: -26, y: -4))
        beamL.addLine(to: CGPoint(x: -40, y: -85))
        beamL.addLine(to: CGPoint(x: -8, y: -85))
        beamL.closeSubpath()
        ctx.fill(beamL, with: .color(Color(red: 1, green: 0.94, blue: 0.63).opacity(0.22)))
        var beamR = Path()
        beamR.move(to: CGPoint(x: 26, y: -4))
        beamR.addLine(to: CGPoint(x: 8, y: -85))
        beamR.addLine(to: CGPoint(x: 40, y: -85))
        beamR.closeSubpath()
        ctx.fill(beamR, with: .color(Color(red: 1, green: 0.94, blue: 0.63).opacity(0.22)))

        var body = Path()
        body.move(to: CGPoint(x: -34, y: 10))
        body.addLine(to: CGPoint(x: -30, y: -4))
        body.addLine(to: CGPoint(x: -14, y: -20))
        body.addLine(to: CGPoint(x: 8, y: -22))
        body.addLine(to: CGPoint(x: 30, y: -6))
        body.addLine(to: CGPoint(x: 36, y: 10))
        body.addLine(to: CGPoint(x: 28, y: 18))
        body.addLine(to: CGPoint(x: -28, y: 18))
        body.closeSubpath()
        ctx.fill(body, with: .color(boosting ? Color(red: 1, green: 0.18, blue: 0.42) : Color(red: 0.18, green: 0.9, blue: 1)))

        ctx.fill(
            Path(ellipseIn: CGRect(x: -40, y: 0, width: 80, height: 40)),
            with: .color((boosting ? Color(red: 1, green: 0.18, blue: 0.58) : Color(red: 0, green: 0.94, blue: 1)).opacity(0.3))
        )

        var cabin = Path()
        cabin.move(to: CGPoint(x: -10, y: -4))
        cabin.addLine(to: CGPoint(x: -4, y: -18))
        cabin.addLine(to: CGPoint(x: 10, y: -18))
        cabin.addLine(to: CGPoint(x: 14, y: -4))
        cabin.closeSubpath()
        ctx.fill(cabin, with: .color(Color(red: 0.07, green: 0.09, blue: 0.13)))

        ctx.fill(Path(ellipseIn: CGRect(x: -27, y: -7, width: 10, height: 10)), with: .color(Color(red: 1, green: 0.97, blue: 0.82)))
        ctx.fill(Path(ellipseIn: CGRect(x: 17, y: -7, width: 10, height: 10)), with: .color(Color(red: 1, green: 0.97, blue: 0.82)))
        ctx.fill(Path(ellipseIn: CGRect(x: -32, y: -14, width: 20, height: 20)), with: .color(Color(red: 1, green: 0.94, blue: 0.63).opacity(0.35)))
        ctx.fill(Path(ellipseIn: CGRect(x: 12, y: -14, width: 20, height: 20)), with: .color(Color(red: 1, green: 0.94, blue: 0.63).opacity(0.35)))

        ctx.fill(Path(ellipseIn: CGRect(x: -32, y: 8, width: 16, height: 16)), with: .color(Color(white: 0.12)))
        ctx.fill(Path(ellipseIn: CGRect(x: 16, y: 8, width: 16, height: 16)), with: .color(Color(white: 0.12)))
        ctx.fill(Path(ellipseIn: CGRect(x: -28, y: 12, width: 8, height: 8)), with: .color(Color(red: 0, green: 0.94, blue: 1)))
        ctx.fill(Path(ellipseIn: CGRect(x: 20, y: 12, width: 8, height: 8)), with: .color(Color(red: 0, green: 0.94, blue: 1)))

        if boosting {
            ctx.fill(
                Path(ellipseIn: CGRect(x: -32, y: 8, width: 64, height: 52)),
                with: .color(Color.orange.opacity(0.4))
            )
        }
    }
}
