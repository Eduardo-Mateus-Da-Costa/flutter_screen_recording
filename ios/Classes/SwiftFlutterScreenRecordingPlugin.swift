import Flutter
import ReplayKit

public class SwiftFlutterScreenRecordingPlugin: NSObject, FlutterPlugin {

    var myResult: FlutterResult?

    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "flutter_screen_recording", binaryMessenger: registrar.messenger())
        let instance = SwiftFlutterScreenRecordingPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if(call.method == "startRecordScreen"){
            do {
                let pickerView = RPSystemBroadcastPickerView(
                frame: CGRect(x: 0, y: 0, width: 0, height: 0))
                var tap = pickerView.subviews.first as! UIButton
                pickerView.translatesAutoresizingMaskIntoConstraints = false
                let extensionId = Bundle.main.object(forInfoDictionaryKey: "RTCScreenSharingExtension") as? String
                pickerView.preferredExtension = extensionId
                tap.sendActions(for: .touchUpInside)
                result(true)
                return
            }catch let error as NSError {
                NSLog("Error starting capture")
                NSLog("\(error)")
                result(false)
                return
           }
        }else if(call.method == "stopRecordScreen"){
            do {
                let pickerView = RPSystemBroadcastPickerView(
                frame: CGRect(x: 0, y: 0, width: 0, height: 0))
                var tap = pickerView.subviews.first as! UIButton
                pickerView.translatesAutoresizingMaskIntoConstraints = false
                let extensionId = Bundle.main.object(forInfoDictionaryKey: "RTCScreenSharingExtension") as? String
                pickerView.preferredExtension = extensionId
                tap.sendActions(for: .touchUpInside)
                result("NoEmpty")
                return
            }catch let error as NSError {
                NSLog("Error starting capture")
                NSLog("\(error)")
                result("")
                return
           }
        } else if (call.method == "makeJson"){
            var args = call.arguments as! [String: Any]

            var fileName = args["fileName"] as! String
            var appGroupIdentifier = args["appGroupIdentifier"] as! String
            var pathDirectory = args["pathDirectory"] as! String
            var jsonFileName = args["jsonFileName"] as! String
            var sharedContainer: URL = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: appGroupIdentifier) as URL!
            var directory: URL = sharedContainer!.appendingPathComponent(pathDirectory) as URL!
            var error: NSError?
            if !FileManager.default.fileExists(atPath: directory!.path) {
                do {
                    try FileManager.default.createDirectory(atPath: directory!.path, withIntermediateDirectories: true, attributes: nil)
                } catch let err as NSError{
                    NSLog("Error creating directory: \(err)")
                    error = err
                    result("")
                    return
                }
            }

            var jsonPath: URL = directory!.appendingPathComponent(jsonFileName)

            var filePath: String = directory!.appendingPathComponent(fileName).path

            let jsonDictionary: [String: Any] = [
                    "filePath": filePath,
                    "error": error?.localizedDescription,
                    "startedDatetime": NSNull(),
                    "finishedDatetime": NSNull()
                ]
            do {
                let jsonData = try JSONSerialization.data(withJSONObject: jsonDictionary, options: .prettyPrinted)
                try jsonData.write(to: jsonPath)
                result(jsonPath.path)
            } catch let err as NSError{
                NSLog("Error writing JSON data: \(err)")
                result("")
                return
            }
        }
        else {
            result("")
        }
    }
}