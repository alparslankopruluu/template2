import AVFoundation
import Foundation

enum SfxKind {
    case themeSelect, touchSplash, boost, meteor, bubblePop, birdChirp, windWhoosh, applySuccess
}

/// Lightweight procedural SFX. Fails silently; respects mute.
final class SoundEffects {
    static let shared = SoundEffects()

    var isMuted: Bool = false {
        didSet { UserDefaults.standard.set(isMuted, forKey: "sound_muted") }
    }

    private var engineReady = false
    private let lock = NSLock()
    private var lastPlay: [String: TimeInterval] = [:]

    private init() {
        isMuted = UserDefaults.standard.bool(forKey: "sound_muted")
        do {
            try AVAudioSession.sharedInstance().setCategory(.ambient, options: [.mixWithOthers])
            try AVAudioSession.sharedInstance().setActive(true, options: [])
            engineReady = true
        } catch {
            engineReady = false
        }
    }

    func play(_ kind: SfxKind) {
        guard !isMuted, engineReady else { return }
        let key = String(describing: kind)
        let now = Date().timeIntervalSinceReferenceDate
        let gap: TimeInterval
        switch kind {
        case .bubblePop, .touchSplash, .windWhoosh: gap = 0.18
        case .birdChirp: gap = 0.32
        case .meteor: gap = 0.22
        default: gap = 0.08
        }
        lock.lock()
        if let last = lastPlay[key], now - last < gap {
            lock.unlock()
            return
        }
        lastPlay[key] = now
        lock.unlock()

        let (freq, dur, vol): (Double, Double, Float)
        switch kind {
        case .themeSelect: freq = 880; dur = 0.08; vol = 0.25
        case .touchSplash: freq = 520; dur = 0.05; vol = 0.18
        case .boost: freq = 220; dur = 0.16; vol = 0.3
        case .meteor: freq = 160; dur = 0.14; vol = 0.28
        case .bubblePop: freq = 980; dur = 0.04; vol = 0.2
        case .birdChirp: freq = 1400; dur = 0.07; vol = 0.22
        case .windWhoosh: freq = 90; dur = 0.18; vol = 0.2
        case .applySuccess: freq = 660; dur = 0.15; vol = 0.28
        }

        DispatchQueue.global(qos: .userInitiated).async {
            self.playTone(frequency: freq, duration: dur, volume: vol)
            if kind == .birdChirp {
                Thread.sleep(forTimeInterval: 0.08)
                self.playTone(frequency: 1700, duration: 0.055, volume: vol * 0.9)
            }
            if kind == .boost {
                self.playTone(frequency: 330, duration: 0.1, volume: vol * 0.7)
            }
        }
    }

    private func playTone(frequency: Double, duration: Double, volume: Float) {
        do {
            let sampleRate = 22050.0
            let n = Int(sampleRate * duration)
            var data = Data(count: 44 + n * 2)
            data.withUnsafeMutableBytes { raw in
                guard let base = raw.bindMemory(to: UInt8.self).baseAddress else { return }
                // Minimal WAV header
                let hdr = base
                let write4: (Int, [UInt8]) -> Void = { off, bytes in
                    for i in 0..<4 { hdr.advanced(by: off + i).pointee = bytes[i] }
                }
                let le32: (Int, UInt32) -> Void = { off, v in
                    hdr.advanced(by: off).pointee = UInt8(v & 0xff)
                    hdr.advanced(by: off + 1).pointee = UInt8((v >> 8) & 0xff)
                    hdr.advanced(by: off + 2).pointee = UInt8((v >> 16) & 0xff)
                    hdr.advanced(by: off + 3).pointee = UInt8((v >> 24) & 0xff)
                }
                let le16: (Int, UInt16) -> Void = { off, v in
                    hdr.advanced(by: off).pointee = UInt8(v & 0xff)
                    hdr.advanced(by: off + 1).pointee = UInt8((v >> 8) & 0xff)
                }
                write4(0, [0x52, 0x49, 0x46, 0x46]) // RIFF
                le32(4, UInt32(36 + n * 2))
                write4(8, [0x57, 0x41, 0x56, 0x45]) // WAVE
                write4(12, [0x66, 0x6d, 0x74, 0x20]) // fmt
                le32(16, 16)
                le16(20, 1) // PCM
                le16(22, 1) // mono
                le32(24, UInt32(sampleRate))
                le32(28, UInt32(sampleRate * 2))
                le16(32, 2)
                le16(34, 16)
                write4(36, [0x64, 0x61, 0x74, 0x61]) // data
                le32(40, UInt32(n * 2))

                let samples = UnsafeMutablePointer<Int16>(OpaquePointer(hdr.advanced(by: 44)))
                for i in 0..<n {
                    let t = Double(i) / sampleRate
                    let env = min(1.0, Double(i) / (sampleRate * 0.01)) *
                        min(1.0, Double(n - i) / (sampleRate * 0.03))
                    let s = sin(2.0 * Double.pi * frequency * t) * Double(volume) * env
                    samples[i] = Int16(max(-1.0, min(1.0, s)) * 32767.0)
                }
            }
            let player = try AVAudioPlayer(data: data)
            player.volume = 1
            player.prepareToPlay()
            player.play()
            // Keep player alive briefly
            Thread.sleep(forTimeInterval: duration + 0.05)
            _ = player
        } catch {
            // Fail silently
        }
    }
}
