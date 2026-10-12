import Foundation
import AVFoundation
import CoreVideo
import CoreGraphics
let output = URL(fileURLWithPath: CommandLine.arguments[1])
try? FileManager.default.removeItem(at: output)
let writer = try AVAssetWriter(outputURL: output, fileType: .mp4)
let input = AVAssetWriterInput(mediaType: .video, outputSettings: [AVVideoCodecKey: AVVideoCodecType.h264, AVVideoWidthKey: 640, AVVideoHeightKey: 360, AVVideoCompressionPropertiesKey: [AVVideoAverageBitRateKey: 240000, AVVideoProfileLevelKey: AVVideoProfileLevelH264BaselineAutoLevel]])
input.expectsMediaDataInRealTime = false
let adaptor = AVAssetWriterInputPixelBufferAdaptor(assetWriterInput: input, sourcePixelBufferAttributes: [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32ARGB, kCVPixelBufferWidthKey as String: 640, kCVPixelBufferHeightKey as String: 360, kCVPixelBufferCGImageCompatibilityKey as String: true, kCVPixelBufferCGBitmapContextCompatibilityKey as String: true])
writer.add(input)
writer.startWriting()
writer.startSession(atSourceTime: .zero)
for frame in 0..<720 {
    while !input.isReadyForMoreMediaData { Thread.sleep(forTimeInterval: 0.002) }
    var buffer: CVPixelBuffer?
    CVPixelBufferPoolCreatePixelBuffer(nil, adaptor.pixelBufferPool!, &buffer)
    let pixel = buffer!
    CVPixelBufferLockBaseAddress(pixel, [])
    let context = CGContext(data: CVPixelBufferGetBaseAddress(pixel), width: 640, height: 360, bitsPerComponent: 8, bytesPerRow: CVPixelBufferGetBytesPerRow(pixel), space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue)!
    let colors: [(CGFloat, CGFloat, CGFloat)] = [(0.22,0.35,0.30),(0.44,0.52,0.47),(0.84,0.82,0.67),(0.94,0.77,0.62)]
    for band in 0..<4 {
        let c = colors[band]
        context.setFillColor(CGColor(red: c.0, green: c.1, blue: c.2, alpha: 1))
        context.fill(CGRect(x: band * 160, y: 0, width: 160, height: 360))
    }
    context.setFillColor(CGColor(gray: 1, alpha: 0.9))
    context.fill(CGRect(x: (frame * 3) % 580, y: 130, width: 60, height: 100))
    CVPixelBufferUnlockBaseAddress(pixel, [])
    adaptor.append(pixel, withPresentationTime: CMTime(value: Int64(frame), timescale: 12))
}
input.markAsFinished()
let done = DispatchSemaphore(value: 0)
writer.finishWriting { done.signal() }
done.wait()
if writer.status != .completed { fatalError(String(describing: writer.error)) }
print("Synthetic H.264 fixture generated")
