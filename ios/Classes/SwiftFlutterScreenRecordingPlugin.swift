import Flutter
import Foundation
import AVFoundation

public class SwiftFlutterScreenRecordingPlugin: NSObject, FlutterPlugin {
    var screenRecorder: ScreenRecorder?

    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "flutter_screen_recording", binaryMessenger: registrar.messenger())
        let instance = SwiftFlutterScreenRecordingPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
       if call.method == "startScreenRecording" {
        do {
            if let args = call.arguments as? [String: Any], let filePath = args["filePath"] as? String {
                let destinationUrl = URL(fileURLWithPath: filePath)
                try screenRecorder = ScreenRecorder(destination: destinationUrl)
                try screenRecorder?.start()
                result(true)
            } else {
                result(FlutterError(code: "INVALID_ARGUMENT", message: "Missing filePath argument", details: nil))
            }
        } catch {
            result(false)
        }
    } else if call.method == "stopScreenRecording" {
        screenRecorder?.stop()
        result(nil)
    } else {
        result(FlutterMethodNotImplemented)
    }
}

    func captureFullScreen() -> Data? {
        UIGraphicsBeginImageContextWithOptions(UIScreen.main.bounds.size, false, 0.0)
        defer { UIGraphicsEndImageContext() }
        guard let context = UIGraphicsGetCurrentContext() else { return nil }
        for window in UIApplication.shared.windows {
            window.layer.render(in: context)
        }
        return UIGraphicsGetImageFromCurrentImageContext()?.pngData()
    }
}


open class ScreenRecorder: NSObject, AVCaptureFileOutputRecordingDelegate {
    let destinationUrl: URL
    let session: AVCaptureSession
    let movieFileOutput: AVCaptureMovieFileOutput

    open var destination: URL {
        get {
            return self.destinationUrl
        }
    }

    public init(destination: URL) {
        destinationUrl = destination

        session = AVCaptureSession()
        session.sessionPreset = AVCaptureSessionPresetHigh

        let displayId: CGDirectDisplayID = CGDirectDisplayID(CGMainDisplayID())

        let input: AVCaptureScreenInput = AVCaptureScreenInput(displayID: displayId)


        if session.canAddInput(input) {
            session.addInput(input)
        }

        movieFileOutput = AVCaptureMovieFileOutput()

        if session.canAddOutput(movieFileOutput) {
            session.addOutput(movieFileOutput)
        }

    }

    open func start() {
        session.startRunning()
        movieFileOutput.startRecording(toOutputFileURL: self.destinationUrl, recordingDelegate: self)
    }

    open func stop() {
        movieFileOutput.stopRecording()
    }

    open func capture(
        _ captureOutput: AVCaptureFileOutput!,
        didFinishRecordingToOutputFileAt outputFileURL: URL!,
        fromConnections connections: [Any]!,
        error: Error!
    ) {
        session.stopRunning()
    }
}
