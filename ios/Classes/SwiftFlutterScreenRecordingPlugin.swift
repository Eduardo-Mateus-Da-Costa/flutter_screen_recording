import Flutter
import Photos
import ReplayKit
import UIKit

public class SwiftFlutterScreenRecordingPlugin: NSObject, FlutterPlugin {

  let recorder = RPScreenRecorder.shared()

  var videoOutputURL: URL?
  var videoWriter: AVAssetWriter?

  var audioInput: AVAssetWriterInput!
  var videoWriterInput: AVAssetWriterInput?
  var nameVideo: String = ""
  var recordAudio: Bool = false
  var recordInternalAudio: Bool = false
  var myResult: FlutterResult?
  let screenSize = UIScreen.main.bounds

  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(
      name: "flutter_screen_recording", binaryMessenger: registrar.messenger())
    let instance = SwiftFlutterScreenRecordingPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {

    if call.method == "startRecordScreen" {
      NSLog("SwiftLog startRecordScreen")
      myResult = result
      let args = call.arguments as? [String: Any]

      do {
        try AVAudioSession.sharedInstance().setCategory(
          .playAndRecord, mode: .videoRecording, options: [.defaultToSpeaker])
        try AVAudioSession.sharedInstance().setActive(true, options: .notifyOthersOnDeactivation)
      } catch let error {
          NSLog(error.localizedDescription)
          NSLog("Setting category to AVAudioSessionCategoryPlayback failed.")
      }

      self.recordAudio = (args?["audio"] as? Bool)!
      self.recordInternalAudio = (args?["internalaudio"] as? Bool)!
      self.nameVideo = (args?["name"] as? String)! + ".mp4"
      NSLog("swift recordAudio: \(recordAudio)")
      NSLog("swift recordInternalAudio: \(recordInternalAudio)")
      NSLog("swift nameVideo: \(nameVideo)")
      startRecording()

    } else if call.method == "stopRecordScreen" {
        NSLog("Stop recording")
      if videoWriter != nil {
        stopRecording()
        let documentsPath =
          NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)[0]
          as NSString
        result(String(documentsPath.appendingPathComponent(nameVideo)))
      }
      NSLog("recordStop error")
      result("")
    }
  }

  @objc func startRecording() {

    //Use ReplayKit to record the screen
    //Create the file path to write to
    let documentsPath = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)[0] as NSString
    self.videoOutputURL = URL(fileURLWithPath: documentsPath.appendingPathComponent(nameVideo))
    NSLog("videoOutputURL: \(videoOutputURL)")
    //Check the file does not already exist by deleting it if it does
    do {
      try FileManager.default.removeItem(at: videoOutputURL!)
    } catch let error {
        NSLog("Error deleting existing file")
        NSLog(error.localizedDescription)
    }

    do {
      try videoWriter = AVAssetWriter(outputURL: videoOutputURL!, fileType: AVFileType.mp4)
    } catch let writerError as NSError {
      NSLog("Error opening video file", writerError)
      videoWriter = nil
      return
    }

    //Create the video settings
    if #available(iOS 11.0, *) {

      var codec = AVVideoCodecJPEG

      if recordAudio || recordInternalAudio {
        codec = AVVideoCodecH264
      }

      let videoSettings: [String: Any] = [
        AVVideoCodecKey: codec,
        AVVideoWidthKey: screenSize.width,
        AVVideoHeightKey: screenSize.height,
        AVVideoCompressionPropertiesKey: [
          AVVideoProfileLevelKey: AVVideoProfileLevelH264HighAutoLevel,
          AVVideoAverageBitRateKey: 6000000
        ],
      ]

      if recordAudio || recordInternalAudio {

        let audioOutputSettings: [String: Any] = [
          AVNumberOfChannelsKey: 2,
          AVFormatIDKey: kAudioFormatMPEG4AAC,
          AVSampleRateKey: 44100,
        ]

        audioInput = AVAssetWriterInput(
          mediaType: AVMediaType.audio, outputSettings: audioOutputSettings)
        videoWriter?.add(audioInput)

      }

      //Create the asset writer input object which is actually used to write out the video
      videoWriterInput = AVAssetWriterInput(
        mediaType: AVMediaType.video, outputSettings: videoSettings)
      videoWriter?.add(videoWriterInput!)

    }

    //Tell the screen recorder to start capturing and to call the handler
    if #available(iOS 11.0, *) {

      if recordAudio {
        RPScreenRecorder.shared().isMicrophoneEnabled = true
      } else {
        RPScreenRecorder.shared().isMicrophoneEnabled = false
      }

      NSLog("Starting recording screen...")

      RPScreenRecorder.shared().startCapture(
        handler: { (cmSampleBuffer, rpSampleType, error) in
          guard error == nil else {
            //Handle error
            NSLog("Error starting capture")
            self.myResult!(false)
            return
          }

          if CMSampleBufferDataIsReady(cmSampleBuffer) {

            DispatchQueue.main.async {
              switch rpSampleType {
              case RPSampleBufferType.video:
                NSLog("writing sample....")
                if self.videoWriter?.status == AVAssetWriter.Status.unknown {

                  if (self.videoWriter?.startWriting) != nil {
                    NSLog("Starting writing")
                    self.myResult!(true)
                    self.videoWriter?.startWriting()
                    self.videoWriter?.startSession(
                      atSourceTime: CMSampleBufferGetPresentationTimeStamp(cmSampleBuffer))
                  }
                }

                if self.videoWriter?.status == AVAssetWriter.Status.writing {
                  if self.videoWriterInput?.isReadyForMoreMediaData == true {
                    NSLog("Writing a sample")
                    if self.videoWriterInput?.append(cmSampleBuffer) == false {
                      NSLog(" we have a problem writing video")
                      self.myResult!(false)
                    }
                  }
                }

              case RPSampleBufferType.audioMic:
                if self.recordAudio {
                  if self.audioInput.isReadyForMoreMediaData {
                    // NSLog("audioMic data added")
                    if self.audioInput.append(cmSampleBuffer) == false {
                      NSLog(" we have a problem writing audio")
                      self.myResult!(false)
                    }
                  }
                }

               case RPSampleBufferType.audioApp:
                if self.recordInternalAudio {
                  if self.audioInput.isReadyForMoreMediaData {
                    // NSLog("audioApp data added")
                    if self.audioInput.append(cmSampleBuffer) == false {
                      NSLog(" we have a problem writing audio")
                      self.myResult!(false)
                    }
                  }
                }

              default:
              ();
              // NSLog("not a video sample, so ignore")
              }
            }
          }
        }) { (error) in
          guard error == nil else {
            //Handle error
            NSLog("Screen record not allowed")
            self.myResult!(false)
            return
          }
        }

        NSLog("Screen recording started")

        self.myResult!(true)
    } else {
      NSLog("Screen recording not available for this version of iOS")
    }
  }

  @objc func stopRecording() {
    //Stop Recording the screen
    if #available(iOS 11.0, *) {
        NSLog("Stopping recording")
        do{
          try RPScreenRecorder.shared().stopCapture(handler: { (error) in
            NSLog("stopping recording")
          })
        } catch let error {
            NSLog("Error stopping recording")
            NSLog(error.localizedDescription)
        }
    } else {
        NSLog("Screen recording not available for this version of iOS")
    }

    //Finish writing the video
    NSLog("Finishing writing video")

    self.videoWriterInput?.markAsFinished()
    self.audioInput?.markAsFinished()

    self.videoWriter?.finishWriting {
      NSLog("finished writing video")

      //Now save the video
      PHPhotoLibrary.shared().performChanges({
        PHAssetChangeRequest.creationRequestForAssetFromVideo(atFileURL: self.videoOutputURL!)
      }) { saved, error in
        if saved {
          let alertController = UIAlertController(
            title: "Your video was successfully saved", message: nil, preferredStyle: .alert)
          let defaultAction = UIAlertAction(title: "OK", style: .default, handler: nil)
          alertController.addAction(defaultAction)
          //self.present(alertController, animated: true, completion: nil)
        }
        if error != nil {
          NSLog("Video did not save for some reason", error.debugDescription)
          NSLog(error?.localizedDescription ?? "error is nil")
        }
      }
    }
    NSLog("Video saved")
  }

}
