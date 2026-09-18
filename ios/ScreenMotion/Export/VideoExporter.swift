import Foundation
import AVFoundation
import UIKit
import Photos

/// Writes a short looping-style MP4 via AVAssetWriter with clearer theme-specific frames.
enum VideoExporter {
    enum ExportError: LocalizedError {
        case writerFailed
        case photosDenied
        case renderFailed

        var errorDescription: String? {
            switch self {
            case .writerFailed: return "Could not create video writer."
            case .photosDenied: return "Photos permission denied."
            case .renderFailed: return "Frame render failed."
            }
        }
    }

    static func requestPhotosAccess() async -> Bool {
        let status = PHPhotoLibrary.authorizationStatus(for: .addOnly)
        if status == .authorized || status == .limited { return true }
        let result = await PHPhotoLibrary.requestAuthorization(for: .addOnly)
        return result == .authorized || result == .limited
    }

    /// Renders ~3s of theme frames at 20fps into a temp MP4, then saves to Photos.
    static func exportShortClip(theme: ThemeType, size: CGSize = CGSize(width: 720, height: 1280)) async throws -> URL {
        guard await requestPhotosAccess() else { throw ExportError.photosDenied }

        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent("screenmotion_\(theme.rawValue.lowercased())_\(UUID().uuidString).mp4")
        if FileManager.default.fileExists(atPath: url.path) {
            try? FileManager.default.removeItem(at: url)
        }

        let writer = try AVAssetWriter(outputURL: url, fileType: .mp4)
        let settings: [String: Any] = [
            AVVideoCodecKey: AVVideoCodecType.h264,
            AVVideoWidthKey: Int(size.width),
            AVVideoHeightKey: Int(size.height)
        ]
        let input = AVAssetWriterInput(mediaType: .video, outputSettings: settings)
        input.expectsMediaDataInRealTime = false
        let adaptor = AVAssetWriterInputPixelBufferAdaptor(
            assetWriterInput: input,
            sourcePixelBufferAttributes: [
                kCVPixelBufferPixelFormatTypeKey as String: Int(kCVPixelFormatType_32ARGB),
                kCVPixelBufferWidthKey as String: Int(size.width),
                kCVPixelBufferHeightKey as String: Int(size.height)
            ]
        )
        guard writer.canAdd(input) else { throw ExportError.writerFailed }
        writer.add(input)
        guard writer.startWriting() else { throw ExportError.writerFailed }
        writer.startSession(atSourceTime: .zero)

        let fps: Int32 = 20
        let frameCount = 60 // ~3 seconds
        let frameDuration = CMTime(value: 1, timescale: fps)

        for i in 0..<frameCount {
            while !input.isReadyForMoreMediaData {
                try await Task.sleep(nanoseconds: 5_000_000)
            }
            let t = TimeInterval(i) / TimeInterval(fps)
            guard let buffer = renderFrame(theme: theme, size: size, time: t) else {
                throw ExportError.renderFailed
            }
            let pts = CMTimeMultiply(frameDuration, multiplier: Int32(i))
            adaptor.append(buffer, withPresentationTime: pts)
        }

        input.markAsFinished()
        await writer.finishWriting()
        if writer.status != .completed {
            throw writer.error ?? ExportError.writerFailed
        }

        try await PHPhotoLibrary.shared().performChanges {
            PHAssetChangeRequest.creationRequestForAssetFromVideo(atFileURL: url)
        }
        return url
    }

    /// Richer procedural frames matching the chosen scene palette & mascots.
    private static func renderFrame(theme: ThemeType, size: CGSize, time: TimeInterval) -> CVPixelBuffer? {
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        let renderer = UIGraphicsImageRenderer(size: size, format: format)
        let image = renderer.image { ctx in
            let cg = ctx.cgContext
            switch theme {
            case .space: drawSpaceFrame(cg, size: size, time: time)
            case .aquarium: drawAquariumFrame(cg, size: size, time: time)
            case .vehicle: drawVehicleFrame(cg, size: size, time: time)
            case .nature: drawNatureFrame(cg, size: size, time: time)
            }

            let attrs: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 22, weight: .bold),
                .foregroundColor: UIColor.white.withAlphaComponent(0.75)
            ]
            NSString(string: "ScreenMotion · \(theme.rawValue)").draw(at: CGPoint(x: 28, y: 44), withAttributes: attrs)
        }
        return pixelBuffer(from: image)
    }

    private static func drawSpaceFrame(_ cg: CGContext, size: CGSize, time: TimeInterval) {
        let colors = [
            UIColor(red: 0.02, green: 0.02, blue: 0.06, alpha: 1).cgColor,
            UIColor(red: 0.12, green: 0.05, blue: 0.22, alpha: 1).cgColor
        ]
        fillGradient(cg, size: size, colors: colors)
        // Nebula blob
        cg.setFillColor(UIColor(red: 0.29, green: 0.12, blue: 0.5, alpha: 0.35).cgColor)
        cg.fillEllipse(in: CGRect(x: size.width * 0.4, y: size.height * 0.15, width: size.width * 0.45, height: size.width * 0.4))
        // Stars
        cg.setFillColor(UIColor.white.withAlphaComponent(0.9).cgColor)
        for i in 0..<70 {
            let seed = Double(i) * 1.618
            let x = (sin(seed + time * 0.3) * 0.5 + 0.5) * size.width
            let y = (cos(seed * 0.7 + time * 0.2) * 0.5 + 0.5) * size.height
            let r: CGFloat = 1.2 + CGFloat(i % 4)
            cg.fillEllipse(in: CGRect(x: x, y: y, width: r, height: r))
        }
        // Planet
        cg.setFillColor(UIColor(red: 0.42, green: 0.55, blue: 1, alpha: 0.9).cgColor)
        cg.fillEllipse(in: CGRect(x: size.width * 0.18, y: size.height * 0.25, width: 70, height: 70))
        // Friendly ship
        let cx = size.width * 0.5 + sin(time * 1.5) * 40
        let cy = size.height * 0.65
        cg.setFillColor(UIColor(red: 0.91, green: 0.93, blue: 1, alpha: 1).cgColor)
        var path = CGMutablePath()
        path.move(to: CGPoint(x: cx, y: cy - 42))
        path.addLine(to: CGPoint(x: cx + 26, y: cy + 22))
        path.addLine(to: CGPoint(x: cx, y: cy + 12))
        path.addLine(to: CGPoint(x: cx - 26, y: cy + 22))
        path.closeSubpath()
        cg.addPath(path); cg.fillPath()
        cg.setFillColor(UIColor(red: 0.42, green: 0.69, blue: 1, alpha: 1).cgColor)
        cg.fillEllipse(in: CGRect(x: cx - 8, y: cy - 12, width: 16, height: 16))
        // Engine glow
        cg.setFillColor(UIColor.cyan.withAlphaComponent(0.45).cgColor)
        cg.fillEllipse(in: CGRect(x: cx - 18, y: cy + 16, width: 36, height: 28))
    }

    private static func drawAquariumFrame(_ cg: CGContext, size: CGSize, time: TimeInterval) {
        let colors = [
            UIColor(red: 0.01, green: 0.1, blue: 0.14, alpha: 1).cgColor,
            UIColor(red: 0.05, green: 0.28, blue: 0.32, alpha: 1).cgColor
        ]
        fillGradient(cg, size: size, colors: colors)
        // Sand
        cg.setFillColor(UIColor(red: 0.36, green: 0.29, blue: 0.16, alpha: 1).cgColor)
        cg.fill(CGRect(x: 0, y: size.height * 0.88, width: size.width, height: size.height * 0.12))
        // Bubbles
        cg.setStrokeColor(UIColor.cyan.withAlphaComponent(0.55).cgColor)
        cg.setLineWidth(1.5)
        for i in 0..<28 {
            let seed = Double(i) * 1.7
            let x = (sin(seed) * 0.5 + 0.5) * size.width
            let y = size.height - CGFloat((time * 40 + seed * 50).truncatingRemainder(dividingBy: Double(size.height)))
            let r: CGFloat = 4 + CGFloat(i % 4) * 2
            cg.strokeEllipse(in: CGRect(x: x, y: y, width: r, height: r))
        }
        // Cute fish school
        let fishColors: [UIColor] = [
            UIColor(red: 1, green: 0.42, blue: 0.42, alpha: 1),
            UIColor(red: 1, green: 0.85, blue: 0.24, alpha: 1),
            UIColor(red: 0.42, green: 0.8, blue: 0.47, alpha: 1),
            UIColor(red: 0.3, green: 0.59, blue: 1, alpha: 1),
            UIColor(red: 0.88, green: 0.34, blue: 0.99, alpha: 1)
        ]
        for i in 0..<5 {
            let phase = time * 1.2 + Double(i)
            let fx = size.width * (0.25 + CGFloat(i) * 0.12) + sin(phase) * 30
            let fy = size.height * 0.45 + cos(phase * 0.8) * 40
            cg.setFillColor(fishColors[i].cgColor)
            cg.fillEllipse(in: CGRect(x: fx - 28, y: fy - 14, width: 56, height: 28))
            cg.setFillColor(UIColor.white.cgColor)
            cg.fillEllipse(in: CGRect(x: fx + 10, y: fy - 8, width: 10, height: 10))
            cg.setFillColor(UIColor.black.cgColor)
            cg.fillEllipse(in: CGRect(x: fx + 14, y: fy - 5, width: 5, height: 5))
        }
    }

    private static func drawVehicleFrame(_ cg: CGContext, size: CGSize, time: TimeInterval) {
        let colors = [
            UIColor(red: 0.05, green: 0.05, blue: 0.08, alpha: 1).cgColor,
            UIColor(red: 0.15, green: 0.08, blue: 0.14, alpha: 1).cgColor
        ]
        fillGradient(cg, size: size, colors: colors)
        // City blocks
        for i in 0..<8 {
            let bw = size.width * 0.08
            let bh = size.height * (0.12 + CGFloat((i * 17) % 30) / 100)
            let bx = CGFloat(i) / 8 * size.width
            cg.setFillColor(UIColor(white: 0.15, alpha: 1).cgColor)
            cg.fill(CGRect(x: bx, y: size.height * 0.48 - bh, width: bw, height: bh))
        }
        // Road
        cg.setFillColor(UIColor(red: 0.1, green: 0.1, blue: 0.13, alpha: 1).cgColor)
        var road = CGMutablePath()
        road.move(to: CGPoint(x: size.width * 0.05, y: size.height))
        road.addLine(to: CGPoint(x: size.width * 0.35, y: size.height * 0.48))
        road.addLine(to: CGPoint(x: size.width * 0.65, y: size.height * 0.48))
        road.addLine(to: CGPoint(x: size.width * 0.95, y: size.height))
        road.closeSubpath()
        cg.addPath(road); cg.fillPath()
        // Dashes
        cg.setStrokeColor(UIColor.white.withAlphaComponent(0.7).cgColor)
        cg.setLineWidth(3)
        for i in 0..<10 {
            let t = (CGFloat(i) / 10 + CGFloat(time * 0.6)).truncatingRemainder(dividingBy: 1)
            let y = size.height * 0.48 + t * size.height * 0.5
            cg.move(to: CGPoint(x: size.width * 0.5, y: y))
            cg.addLine(to: CGPoint(x: size.width * 0.5, y: y + 16))
            cg.strokePath()
        }
        // Neon car
        let cx = size.width * 0.5 + sin(time * 2) * 40
        let cy = size.height * 0.72
        cg.setFillColor(UIColor(red: 0, green: 0.94, blue: 1, alpha: 0.35).cgColor)
        cg.fillEllipse(in: CGRect(x: cx - 40, y: cy + 8, width: 80, height: 36))
        cg.setFillColor(UIColor(red: 0.18, green: 0.9, blue: 1, alpha: 1).cgColor)
        cg.fill(CGRect(x: cx - 36, y: cy - 14, width: 72, height: 28))
        cg.setFillColor(UIColor(red: 1, green: 0.97, blue: 0.82, alpha: 1).cgColor)
        cg.fillEllipse(in: CGRect(x: cx - 28, y: cy - 12, width: 10, height: 10))
        cg.fillEllipse(in: CGRect(x: cx + 18, y: cy - 12, width: 10, height: 10))
    }

    private static func drawNatureFrame(_ cg: CGContext, size: CGSize, time: TimeInterval) {
        let colors = [
            UIColor(red: 0.04, green: 0.07, blue: 0.09, alpha: 1).cgColor,
            UIColor(red: 0.10, green: 0.18, blue: 0.14, alpha: 1).cgColor
        ]
        fillGradient(cg, size: size, colors: colors)
        // Sun
        cg.setFillColor(UIColor(red: 1, green: 0.78, blue: 0.47, alpha: 0.3).cgColor)
        cg.fillEllipse(in: CGRect(x: size.width * 0.65, y: size.height * 0.12, width: 140, height: 140))
        cg.setFillColor(UIColor(red: 1, green: 0.88, blue: 0.63, alpha: 0.9).cgColor)
        cg.fillEllipse(in: CGRect(x: size.width * 0.72, y: size.height * 0.18, width: 44, height: 44))
        // Hills
        cg.setFillColor(UIColor(red: 0.14, green: 0.25, blue: 0.20, alpha: 1).cgColor)
        var hill = CGMutablePath()
        hill.move(to: CGPoint(x: 0, y: size.height))
        hill.addLine(to: CGPoint(x: 0, y: size.height * 0.65))
        for i in 0..<20 {
            let x = CGFloat(i) / 19 * size.width
            let y = size.height * 0.65 + sin(Double(i) * 0.7 + time * 0.3) * 30
            hill.addLine(to: CGPoint(x: x, y: y))
        }
        hill.addLine(to: CGPoint(x: size.width, y: size.height))
        hill.closeSubpath()
        cg.addPath(hill); cg.fillPath()
        cg.setFillColor(UIColor(red: 0.18, green: 0.33, blue: 0.25, alpha: 1).cgColor)
        var near = CGMutablePath()
        near.move(to: CGPoint(x: 0, y: size.height))
        near.addLine(to: CGPoint(x: 0, y: size.height * 0.8))
        for i in 0..<16 {
            let x = CGFloat(i) / 15 * size.width
            let y = size.height * 0.8 + sin(Double(i) * 0.9 + time * 0.4) * 18
            near.addLine(to: CGPoint(x: x, y: y))
        }
        near.addLine(to: CGPoint(x: size.width, y: size.height))
        near.closeSubpath()
        cg.addPath(near); cg.fillPath()
        // Clouds
        cg.setFillColor(UIColor(red: 0.85, green: 0.91, blue: 0.94, alpha: 0.45).cgColor)
        let cloudX = (CGFloat(time * 30).truncatingRemainder(dividingBy: size.width + 100)) - 50
        cg.fillEllipse(in: CGRect(x: cloudX, y: size.height * 0.2, width: 90, height: 40))
        cg.fillEllipse(in: CGRect(x: cloudX + 40, y: size.height * 0.18, width: 70, height: 36))
        // Birds
        cg.setStrokeColor(UIColor.white.withAlphaComponent(0.85).cgColor)
        cg.setLineWidth(2.5)
        for i in 0..<4 {
            let bx = size.width * (0.2 + CGFloat(i) * 0.18) + sin(time + Double(i)) * 20
            let by = size.height * 0.35 + cos(time * 0.8 + Double(i)) * 15
            cg.move(to: CGPoint(x: bx - 14, y: by - 6))
            cg.addQuadCurve(to: CGPoint(x: bx + 14, y: by - 6), control: CGPoint(x: bx, y: by - 16 + sin(time * 6) * 4))
            cg.strokePath()
        }
        // Leaf mascot
        let lx = size.width * 0.55 + sin(time * 0.8) * 40
        let ly = size.height * 0.42
        cg.setFillColor(UIColor(red: 0.24, green: 0.86, blue: 0.52, alpha: 1).cgColor)
        var leaf = CGMutablePath()
        leaf.move(to: CGPoint(x: lx, y: ly - 28))
        leaf.addCurve(to: CGPoint(x: lx, y: ly + 28), control1: CGPoint(x: lx + 22, y: ly - 18), control2: CGPoint(x: lx + 26, y: ly + 10))
        leaf.addCurve(to: CGPoint(x: lx, y: ly - 28), control1: CGPoint(x: lx - 26, y: ly + 10), control2: CGPoint(x: lx - 22, y: ly - 18))
        leaf.closeSubpath()
        cg.addPath(leaf); cg.fillPath()
        cg.setFillColor(UIColor.white.cgColor)
        cg.fillEllipse(in: CGRect(x: lx - 10, y: ly - 8, width: 8, height: 8))
        cg.fillEllipse(in: CGRect(x: lx + 2, y: ly - 8, width: 8, height: 8))
        cg.setFillColor(UIColor.black.cgColor)
        cg.fillEllipse(in: CGRect(x: lx - 7, y: ly - 6, width: 3.5, height: 3.5))
        cg.fillEllipse(in: CGRect(x: lx + 5, y: ly - 6, width: 3.5, height: 3.5))
    }

    private static func fillGradient(_ cg: CGContext, size: CGSize, colors: [CGColor]) {
        let gradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: colors as CFArray, locations: [0, 1])!
        cg.drawLinearGradient(gradient, start: .zero, end: CGPoint(x: 0, y: size.height), options: [])
    }

    private static func pixelBuffer(from image: UIImage) -> CVPixelBuffer? {
        guard let cgImage = image.cgImage else { return nil }
        let width = cgImage.width
        let height = cgImage.height
        var buffer: CVPixelBuffer?
        let attrs: [String: Any] = [
            kCVPixelBufferCGImageCompatibilityKey as String: true,
            kCVPixelBufferCGBitmapContextCompatibilityKey as String: true
        ]
        CVPixelBufferCreate(kCFAllocatorDefault, width, height, kCVPixelFormatType_32ARGB, attrs as CFDictionary, &buffer)
        guard let pb = buffer else { return nil }
        CVPixelBufferLockBaseAddress(pb, [])
        defer { CVPixelBufferUnlockBaseAddress(pb, []) }
        guard let ctx = CGContext(
            data: CVPixelBufferGetBaseAddress(pb),
            width: width,
            height: height,
            bitsPerComponent: 8,
            bytesPerRow: CVPixelBufferGetBytesPerRow(pb),
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue
        ) else { return nil }
        ctx.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))
        return pb
    }
}
