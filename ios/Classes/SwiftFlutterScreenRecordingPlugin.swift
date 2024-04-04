import Flutter
import UIKit
import ReplayKit
import Photos

struct RecorderConfig {
    var fileName: String = ""
    var dirPathToSave:NSString = ""
    var isAudioEnabled: Bool = false
    var addTimeCode: Bool! = false
    var filePath: NSString = ""
    var videoFrame: Int?
    var videoBitrate: Int? = 5000
    var fileOutputFormat: String = ""
    var fileExtension: String = ""
    var videoHash: String = ""
    var width:Int?
    var height:Int?
}

public class SwiftFlutterScreenRecordingPlugin: NSObject, FlutterPlugin {

    let recorder = RPScreenRecorder.shared()
    var videoOutputURL : URL?
    var videoWriter : AVAssetWriter?
    var audioInput:AVAssetWriterInput!
    var internalAudioInput:AVAssetWriterInput!
    var videoWriterInput : AVAssetWriterInput?

    var success: Bool = false
    var startDate: Int?
    var endDate: Int?
    var isProgress: Bool = false
    var eventName: String = ""
    var message: String = ""


    var myResult: FlutterResult?

    var recorderConfig:RecorderConfig = RecorderConfig()

    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "flutter_screen_recording", binaryMessenger: registrar.messenger())
        let instance = SwiftFlutterScreenRecordingPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {

        if(call.method == "startRecordScreen"){
            do {
                DispatchQueue.main.sync { [self] in
                    let pickerView = RPSystemBroadcastPickerView(
                    frame: CGRect(x: 0, y: 0, width: 0, height: 0))
                    var tap = pickerView.subviews.first as! UIButton
                    pickerView.translatesAutoresizingMaskIntoConstraints = false
                    let extensionId = Bundle.main.object(forInfoDictionaryKey: "RTCScreenSharingExtension") as? String
                    pickerView.preferredExtension = extensionId
                    tap.sendActions(for: .touchUpInside)
                }
                result(self.success)
                return
            }catch let error as NSError {
                NSLog("Error starting capture")
                NSLog("\(error)")
                result(false)
                return
           }
        }else if(call.method == "stopRecordScreen"){
            do {
                    DispatchQueue.main.sync{ [self] in
                    let pickerView = RPSystemBroadcastPickerView(
                    frame: CGRect(x: 0, y: 0, width: 0, height: 0))
                    var tap = pickerView.subviews.first as! UIButton
                    pickerView.translatesAutoresizingMaskIntoConstraints = false
                    let extensionId = Bundle.main.object(forInfoDictionaryKey: "RTCScreenSharingExtension") as? String
                    pickerView.preferredExtension = extensionId
                    tap.sendActions(for: .touchUpInside)
                }
                result("lalala")
                return
            }catch let error as NSError {
                NSLog("Error starting capture")
                NSLog("\(error)")
                result("")
                return
           }
        }
        else {
            result("")
        }
    }

    func randomString(length: Int) -> String {
        let letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return String((0..<length).map{ _ in letters.randomElement()! })
    }

    @objc func startRecording(width: Int32, height: Int32) -> Bool {
        var res : Bool = true
        if(recorder.isAvailable){
            if recorderConfig.dirPathToSave != "" {
                recorderConfig.filePath = (recorderConfig.dirPathToSave ) as NSString
                self.videoOutputURL = URL(fileURLWithPath: String(recorderConfig.filePath.appendingPathComponent(recorderConfig.fileName ) ))
            } else {
                recorderConfig.filePath = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)[0] as NSString
                self.videoOutputURL = URL(fileURLWithPath: String(recorderConfig.filePath.appendingPathComponent(recorderConfig.fileName ) ))
            }
            do {
                let fileManager = FileManager.default
                if (fileManager.fileExists(atPath: videoOutputURL!.path)){
                    try FileManager.default.removeItem(at: videoOutputURL!)}
            } catch let fileError as NSError{
                self.message=String(fileError as! Substring) as String
                res = Bool(false);
            }

            do {
                try videoWriter = AVAssetWriter(outputURL: videoOutputURL!, fileType: AVFileType.mp4)
                self.message=String("Started Video")
            } catch let writerError as NSError {
                self.message=String(writerError as! Substring) as String
                videoWriter = nil
                res = Bool(false)
            }
            if #available(iOS 11.0, *) {
                recorder.isMicrophoneEnabled = recorderConfig.isAudioEnabled
                let videoSettings: [String : Any] = [
                    AVVideoCodecKey  : AVVideoCodecType.h264,
                    AVVideoWidthKey  : NSNumber.init(value: width),
                    AVVideoHeightKey : NSNumber.init(value: height),
                    AVVideoCompressionPropertiesKey: [
                        AVVideoProfileLevelKey: AVVideoProfileLevelH264HighAutoLevel,
                        AVVideoAverageBitRateKey: recorderConfig.videoBitrate!
                    ] as [String : Any],
                ]
                self.videoWriterInput = AVAssetWriterInput(mediaType: AVMediaType.video, outputSettings: videoSettings)
                self.videoWriterInput?.expectsMediaDataInRealTime = true
                self.videoWriter?.add(videoWriterInput!);
                if(recorderConfig.isAudioEnabled) {
                    let audioOutputSettings: [String : Any] = [
                        AVNumberOfChannelsKey : 2,
                        AVFormatIDKey : kAudioFormatMPEG4AAC,
                        AVSampleRateKey: 44100,
                        AVEncoderAudioQualityKey: AVAudioQuality.high.rawValue
                    ]
                    self.audioInput = AVAssetWriterInput(mediaType: AVMediaType.audio, outputSettings: audioOutputSettings)
                    self.audioInput?.expectsMediaDataInRealTime = true
                    self.videoWriter?.add(audioInput!)

                    self.internalAudioInput = AVAssetWriterInput(mediaType: AVMediaType.audio, outputSettings: audioOutputSettings)
                    self.internalAudioInput?.expectsMediaDataInRealTime = true
                    self.videoWriter?.add(internalAudioInput!)
                }

                recorder.startCapture(
                    handler: {
                    (cmSampleBuffer, rpSampleType, error) in guard error == nil else { return }
                        switch rpSampleType {
                            case RPSampleBufferType.video:
                                if self.videoWriter?.status == AVAssetWriter.Status.unknown {
                                    self.videoWriter?.startWriting()
                                    self.videoWriter?.startSession(atSourceTime:  CMSampleBufferGetPresentationTimeStamp(cmSampleBuffer));
                                }else if self.videoWriter?.status == AVAssetWriter.Status.writing {
                                    if (self.videoWriterInput?.isReadyForMoreMediaData == true) {
                                        if  self.videoWriterInput?.append(cmSampleBuffer) == false {
                                            res = Bool(false)
                                            self.message="Error starting capture"
                                        }
                                    }
                                }
                            case RPSampleBufferType.audioMic:
                                if (self.recorderConfig.isAudioEnabled){
                                    if self.audioInput?.isReadyForMoreMediaData == true {
                                        if self.audioInput?.append(cmSampleBuffer) == false {
                                            NSLog("Audio mic writing error")
                                            NSLog("\(self.videoWriter?.status)")
                                            NSLog("\(self.videoWriter?.error)")
                                        }
                                    }
                                }
                            case RPSampleBufferType.audioApp:
                               if (self.recorderConfig.isAudioEnabled){
                                    if self.internalAudioInput?.isReadyForMoreMediaData == true {
                                        if self.internalAudioInput?.append(cmSampleBuffer) == false {
                                            NSLog("Audio app writing error")
                                            NSLog("\(self.videoWriter?.status)")
                                            NSLog("\(self.videoWriter?.error)")
                                        }
                                    }
                                }
                        default:
                            break;
                        }
                    }){(error) in guard error == nil else {
                        return
                    }
                }
            }
        }
        return  Bool(res)
    }

    @objc func stopRecording() -> Bool {
        var res : Bool = true;
        if(recorder.isRecording){
            if #available(iOS 11.0, *) {

                recorder.stopCapture( handler: { (error) in
                    if(error != nil){
                        res = Bool(false)
                        self.message = "Has Got Error in stop record"
                    }
                })
            } else {
                res = Bool(false)
                self.message="You dont Support this plugin"
            }

            self.videoWriterInput?.markAsFinished();
            if(recorderConfig.isAudioEnabled) {
                self.audioInput?.markAsFinished();
                self.internalAudioInput?.markAsFinished();
            }

            self.videoWriter?.finishWriting {
                PHPhotoLibrary.shared().performChanges({
                    PHAssetChangeRequest.creationRequestForAssetFromVideo(atFileURL: self.videoOutputURL!)
                })
                self.message="stopRecordScreenFromApp"
            }
        } else{
            self.message="You haven't start the recording unit now!"
        }
        return Bool(res)

    }
}