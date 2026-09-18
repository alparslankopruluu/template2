import Foundation
import AVFoundation
import UIKit
import Photos

/// Writes a short looping-style MP4 via AVAssetWriter (a few animated frames).
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

    /// Renders ~3s of theme frames at 15fps into a temp MP4, then saves to Photos.
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

        let fps: Int32 = 15
        let frameCount = 45 // ~3 seconds
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

    /// UIImageRenderer still frames — procedural placeholders matching theme colors.
    private static func renderFrame(theme: ThemeType, size: CGSize, time: TimeInterval) -> CVPixelBuffer? {
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        let renderer = UIGraphicsImageRenderer(size: size, format: format)
        let image = renderer.image { ctx in
            let cg = ctx.cgContext
            let colors: [CGColor]
            switch theme {
            case .space:
                colors = [UIColor(red: 0.02, green: 0.02, blue: 0.06, alpha: 1).cgColor,
                          UIColor(red: 0.12, green: 0.05, blue: 0.22, alpha: 1).cgColor]
            case .aquarium:
                colors = [UIColor(red: 0.01, green: 0.1, blue: 0.14, alpha: 1).cgColor,
                          UIColor(red: 0.05, green: 0.28, blue: 0.32, alpha: 1).cgColor]
            case .vehicle:
                colors = [UIColor(red: 0.05, green: 0.05, blue: 0.08, alpha: 1).cgColor,
                          UIColor(red: 0.15, green: 0.08, blue: 0.14, alpha: 1).cgColor]
            }
            let gradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: colors as CFArray, locations: [0, 1])!
            cg.drawLinearGradient(gradient, start: .zero, end: CGPoint(x: 0, y: size.height), options: [])

            // Animated particles / accents
            cg.setFillColor(UIColor.white.withAlphaComponent(0.85).cgColor)
            for i in 0..<40 {
                let seed = Double(i) * 1.618
                let x = (sin(seed + time) * 0.5 + 0.5) * size.width
                let y = (cos(seed * 0.7 + time * 0.5) * 0.5 + 0.5) * size.height
                let r: CGFloat = 1.5 + CGFloat(i % 4)
                cg.fillEllipse(in: CGRect(x: x, y: y, width: r, height: r))
            }

            // Theme mascot hint
            cg.setFillColor(UIColor(red: 0.42, green: 0.55, blue: 1, alpha: 0.9).cgColor)
            let cx = size.width * 0.5 + sin(time * 2) * 30
            let cy = size.height * 0.65
            switch theme {
            case .space:
                var path = CGMutablePath()
                path.move(to: CGPoint(x: cx, y: cy - 40))
                path.addLine(to: CGPoint(x: cx + 24, y: cy + 20))
                path.addLine(to: CGPoint(x: cx, y: cy + 10))
                path.addLine(to: CGPoint(x: cx - 24, y: cy + 20))
                path.closeSubpath()
                cg.addPath(path); cg.fillPath()
            case .aquarium:
                cg.fillEllipse(in: CGRect(x: cx - 28, y: cy - 14, width: 56, height: 28))
            case .vehicle:
                cg.fill(CGRect(x: cx - 30, y: cy - 12, width: 60, height: 24))
            }

            // Branding
            let attrs: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 22, weight: .bold),
                .foregroundColor: UIColor.white.withAlphaComponent(0.7)
            ]
            NSString(string: "ScreenMotion").draw(at: CGPoint(x: 32, y: 48), withAttributes: attrs)
        }

        return pixelBuffer(from: image)
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
