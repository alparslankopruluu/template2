import SwiftUI

enum VehicleScene {
    static func draw(
        context: GraphicsContext,
        size: CGSize,
        time: TimeInterval,
        tiltX: CGFloat,
        tiltY: CGFloat,
        touch: TouchState,
        vehicle: VehicleType = .sportsCar
    ) {
        let rect = CGRect(origin: .zero, size: size)
        context.fill(
            Path(rect),
            with: .linearGradient(
                Gradient(colors: [
                    Color(red: 0.04, green: 0.04, blue: 0.05),
                    Color(red: 0.09, green: 0.09, blue: 0.10)
                ]),
                startPoint: .zero,
                endPoint: CGPoint(x: 0, y: size.height * 0.5)
            )
        )

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
        let speed: CGFloat = boost ? vehicle.boostMult : 1.05

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
                context.stroke(line, with: .color(vehicle.trailColor.opacity(0.55)), lineWidth: 2)
            }
        }

        var lane = tiltX.clamped(to: -1...1)
        let idleY: CGFloat = vehicle == .helicopter ? 0.52 + sin(time * 1.2) * 0.03 : 0.72 + sin(time * 0.8) * 0.01
        var yNorm: CGFloat = idleY
        if touch.isTouching, let loc = touch.location {
            lane = ((loc.x / max(size.width, 1)) * 2 - 1).clamped(to: -1...1)
            let yMin: CGFloat = vehicle == .helicopter ? 0.28 : 0.45
            let yMax: CGFloat = vehicle == .helicopter ? 0.72 : 0.88
            yNorm = (loc.y / max(size.height, 1)).clamped(to: yMin...yMax)
        }
        let t = ((yNorm - 0.48) / 0.52).clamped(to: 0...1)
        let roadHalf = size.width * (0.12 + t * 0.30)
        let cx = size.width * 0.5 + lane * roadHalf * 0.7
        let cy = yNorm * size.height + sin(time * 14 * Double(speed)) * 1.8
        let scale = 0.55 + t * 0.6

        for i in 0..<8 {
            let fade = CGFloat(i) / 8
            let ty = cy + 20 + fade * 50
            context.fill(
                Path(ellipseIn: CGRect(x: cx - 14 + fade * 4, y: ty - 8, width: 28 - fade * 12, height: 16)),
                with: .color(vehicle.trailColor.opacity(0.35 * (1 - fade)))
            )
            context.fill(
                Path(ellipseIn: CGRect(x: cx - 6, y: ty - 3, width: 12, height: 8)),
                with: .color(vehicle.bodyColor.opacity(0.25 * (1 - fade)))
            )
        }

        drawVehicle(context: context, at: CGPoint(x: cx, y: cy), scale: scale, boosting: boost, vehicle: vehicle, time: time)
    }

    private static func drawVehicle(
        context: GraphicsContext,
        at p: CGPoint,
        scale: CGFloat,
        boosting: Bool,
        vehicle: VehicleType,
        time: TimeInterval
    ) {
        var ctx = context
        ctx.translateBy(x: p.x, y: p.y)
        ctx.scaleBy(x: scale, y: scale)
        switch vehicle {
        case .sportsCar: drawSports(ctx: &ctx, boosting: boosting, vehicle: vehicle)
        case .truck: drawTruck(ctx: &ctx, boosting: boosting, vehicle: vehicle)
        case .motorcycle: drawBike(ctx: &ctx, boosting: boosting, vehicle: vehicle)
        case .helicopter: drawHeli(ctx: &ctx, boosting: boosting, vehicle: vehicle, time: time)
        }
    }

    private static func drawSports(ctx: inout GraphicsContext, boosting: Bool, vehicle: VehicleType) {
        ctx.fill(Path(ellipseIn: CGRect(x: -40, y: 20, width: 80, height: 14)), with: .color(.black.opacity(0.4)))
        var beamL = Path()
        beamL.move(to: CGPoint(x: -26, y: -4)); beamL.addLine(to: CGPoint(x: -40, y: -85)); beamL.addLine(to: CGPoint(x: -8, y: -85)); beamL.closeSubpath()
        ctx.fill(beamL, with: .color(Color(red: 1, green: 0.94, blue: 0.63).opacity(0.22)))
        var beamR = Path()
        beamR.move(to: CGPoint(x: 26, y: -4)); beamR.addLine(to: CGPoint(x: 8, y: -85)); beamR.addLine(to: CGPoint(x: 40, y: -85)); beamR.closeSubpath()
        ctx.fill(beamR, with: .color(Color(red: 1, green: 0.94, blue: 0.63).opacity(0.22)))
        var body = Path()
        body.move(to: CGPoint(x: -34, y: 10)); body.addLine(to: CGPoint(x: -30, y: -4)); body.addLine(to: CGPoint(x: -14, y: -20))
        body.addLine(to: CGPoint(x: 8, y: -22)); body.addLine(to: CGPoint(x: 30, y: -6)); body.addLine(to: CGPoint(x: 36, y: 10))
        body.addLine(to: CGPoint(x: 28, y: 18)); body.addLine(to: CGPoint(x: -28, y: 18)); body.closeSubpath()
        ctx.fill(body, with: .color(boosting ? Color(red: 0.69, green: 0.44, blue: 0.50) : vehicle.bodyColor))
        ctx.fill(Path(ellipseIn: CGRect(x: -40, y: 0, width: 80, height: 40)), with: .color(vehicle.trailColor.opacity(0.3)))
        var cabin = Path()
        cabin.move(to: CGPoint(x: -10, y: -4)); cabin.addLine(to: CGPoint(x: -4, y: -18)); cabin.addLine(to: CGPoint(x: 10, y: -18)); cabin.addLine(to: CGPoint(x: 14, y: -4)); cabin.closeSubpath()
        ctx.fill(cabin, with: .color(Color(red: 0.07, green: 0.09, blue: 0.13)))
        ctx.fill(Path(ellipseIn: CGRect(x: -27, y: -7, width: 10, height: 10)), with: .color(Color(red: 1, green: 0.97, blue: 0.82)))
        ctx.fill(Path(ellipseIn: CGRect(x: 17, y: -7, width: 10, height: 10)), with: .color(Color(red: 1, green: 0.97, blue: 0.82)))
        ctx.fill(Path(ellipseIn: CGRect(x: -32, y: 8, width: 16, height: 16)), with: .color(Color(white: 0.12)))
        ctx.fill(Path(ellipseIn: CGRect(x: 16, y: 8, width: 16, height: 16)), with: .color(Color(white: 0.12)))
        ctx.fill(Path(ellipseIn: CGRect(x: -28, y: 12, width: 8, height: 8)), with: .color(vehicle.trailColor))
        ctx.fill(Path(ellipseIn: CGRect(x: 20, y: 12, width: 8, height: 8)), with: .color(vehicle.trailColor))
        if boosting { ctx.fill(Path(ellipseIn: CGRect(x: -32, y: 8, width: 64, height: 52)), with: .color(Color.orange.opacity(0.4))) }
    }

    private static func drawTruck(ctx: inout GraphicsContext, boosting: Bool, vehicle: VehicleType) {
        ctx.fill(Path(ellipseIn: CGRect(x: -48, y: 22, width: 96, height: 14)), with: .color(.black.opacity(0.35)))
        ctx.fill(Path(roundedRect: CGRect(x: -44, y: -8, width: 28, height: 26), cornerRadius: 4), with: .color(boosting ? Color(red: 0.8, green: 0.33, blue: 0.13) : Color(red: 0.87, green: 0.47, blue: 0.27)))
        var cab = Path()
        cab.move(to: CGPoint(x: -20, y: 12)); cab.addLine(to: CGPoint(x: -18, y: -18)); cab.addLine(to: CGPoint(x: 8, y: -20)); cab.addLine(to: CGPoint(x: 14, y: 12)); cab.closeSubpath()
        ctx.fill(cab, with: .color(boosting ? Color(red: 1, green: 0.4, blue: 0.13) : vehicle.bodyColor))
        ctx.fill(Path(CGRect(x: -12, y: -16, width: 18, height: 14)), with: .color(Color(red: 0.1, green: 0.12, blue: 0.19)))
        ctx.fill(Path(ellipseIn: CGRect(x: 5, y: -7, width: 10, height: 10)), with: .color(Color(red: 1, green: 0.97, blue: 0.82)))
        for x in [-36.0, -22.0, 6.0] as [CGFloat] {
            ctx.fill(Path(ellipseIn: CGRect(x: x - 9, y: 9, width: 18, height: 18)), with: .color(Color(white: 0.12)))
            ctx.fill(Path(ellipseIn: CGRect(x: x - 3, y: 15, width: 6, height: 6)), with: .color(vehicle.trailColor))
        }
        if boosting { ctx.fill(Path(ellipseIn: CGRect(x: -36, y: 6, width: 70, height: 48)), with: .color(Color.orange.opacity(0.35))) }
    }

    private static func drawBike(ctx: inout GraphicsContext, boosting: Bool, vehicle: VehicleType) {
        ctx.fill(Path(ellipseIn: CGRect(x: -28, y: 18, width: 56, height: 12)), with: .color(.black.opacity(0.35)))
        ctx.fill(Path(ellipseIn: CGRect(x: -29, y: 3, width: 22, height: 22)), with: .color(Color(white: 0.13)))
        ctx.fill(Path(ellipseIn: CGRect(x: 7, y: 3, width: 22, height: 22)), with: .color(Color(white: 0.13)))
        ctx.fill(Path(ellipseIn: CGRect(x: -22, y: 10, width: 8, height: 8)), with: .color(vehicle.trailColor))
        ctx.fill(Path(ellipseIn: CGRect(x: 14, y: 10, width: 8, height: 8)), with: .color(vehicle.trailColor))
        var tank = Path()
        tank.move(to: CGPoint(x: -14, y: 6)); tank.addLine(to: CGPoint(x: -6, y: -14)); tank.addLine(to: CGPoint(x: 10, y: -12))
        tank.addLine(to: CGPoint(x: 16, y: 4)); tank.addLine(to: CGPoint(x: 4, y: 10)); tank.addLine(to: CGPoint(x: -10, y: 10)); tank.closeSubpath()
        ctx.fill(tank, with: .color(boosting ? Color(red: 0.69, green: 0.47, blue: 0.56) : vehicle.bodyColor))
        ctx.fill(Path(ellipseIn: CGRect(x: -7, y: -25, width: 14, height: 14)), with: .color(Color(red: 0.1, green: 0.08, blue: 0.12)))
        ctx.fill(Path(roundedRect: CGRect(x: -6, y: -12, width: 14, height: 16), cornerRadius: 3), with: .color(Color(red: 0.1, green: 0.08, blue: 0.12)))
        var bar = Path(); bar.move(to: CGPoint(x: 8, y: -10)); bar.addLine(to: CGPoint(x: 20, y: -18))
        ctx.stroke(bar, with: .color(Color(white: 0.8)), lineWidth: 3)
        if boosting { ctx.fill(Path(ellipseIn: CGRect(x: -40, y: 0, width: 40, height: 36)), with: .color(Color.yellow.opacity(0.35))) }
    }

    private static func drawHeli(ctx: inout GraphicsContext, boosting: Bool, vehicle: VehicleType, time: TimeInterval) {
        ctx.fill(Path(ellipseIn: CGRect(x: -36, y: 16, width: 72, height: 14)), with: .color(.black.opacity(0.3)))
        var cabin = Path()
        cabin.move(to: CGPoint(x: -22, y: 6))
        cabin.addQuadCurve(to: CGPoint(x: 0, y: -22), control: CGPoint(x: -24, y: -18))
        cabin.addQuadCurve(to: CGPoint(x: 22, y: 4), control: CGPoint(x: 18, y: -16))
        cabin.addLine(to: CGPoint(x: 14, y: 14)); cabin.addLine(to: CGPoint(x: -14, y: 14)); cabin.closeSubpath()
        ctx.fill(cabin, with: .color(boosting ? Color(red: 0.48, green: 0.67, blue: 0.54) : vehicle.bodyColor))
        ctx.fill(Path(ellipseIn: CGRect(x: -10, y: -16, width: 22, height: 18)), with: .color(Color(red: 0.06, green: 0.12, blue: 0.09).opacity(0.7)))
        ctx.fill(Path(roundedRect: CGRect(x: -48, y: -4, width: 30, height: 8), cornerRadius: 3), with: .color(vehicle.bodyColor))
        ctx.fill(Path(CGRect(x: -50, y: -12, width: 6, height: 22)), with: .color(vehicle.bodyColor))
        let a = time * (boosting ? 28 : 14)
        let r: CGFloat = boosting ? 48 : 42
        var blade = Path()
        blade.move(to: CGPoint(x: cos(a) * r, y: -24 + sin(a) * 3))
        blade.addLine(to: CGPoint(x: -cos(a) * r, y: -24 - sin(a) * 3))
        ctx.stroke(blade, with: .color(Color.white.opacity(boosting ? 0.8 : 0.55)), lineWidth: 3)
        var blade2 = Path()
        blade2.move(to: CGPoint(x: cos(a + 1.57) * r * 0.9, y: -24))
        blade2.addLine(to: CGPoint(x: -cos(a + 1.57) * r * 0.9, y: -24))
        ctx.stroke(blade2, with: .color(Color.white.opacity(0.45)), lineWidth: 2)
        ctx.fill(Path(ellipseIn: CGRect(x: -4, y: -28, width: 8, height: 8)), with: .color(Color(white: 0.2)))
        var skid = Path(); skid.move(to: CGPoint(x: -18, y: 16)); skid.addLine(to: CGPoint(x: 18, y: 16))
        ctx.stroke(skid, with: .color(Color(white: 0.55)), lineWidth: 3)
        if boosting { ctx.fill(Path(ellipseIn: CGRect(x: -40, y: 0, width: 80, height: 50)), with: .color(vehicle.trailColor.opacity(0.35))) }
    }
}
