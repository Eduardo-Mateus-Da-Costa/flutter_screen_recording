import 'dart:async';
import 'dart:io';

import 'package:flutter/services.dart';

import 'flutter_screen_recording_platform_interface.dart';

class MethodChannelFlutterScreenRecording
    extends FlutterScreenRecordingPlatform {
  static const MethodChannel _channel =
      const MethodChannel('flutter_screen_recording');

  /// to capture internal audio you need CAPTURE_AUDIO_OUTPUT permission
  Future<bool> startRecordScreen(
    String name, {
    String notificationTitle = "",
    String notificationMessage = "",
    bool internalAudio = false,
    bool micAudio = false,
  }) async {
    final bool start = await _channel.invokeMethod('startRecordScreen', {
      "name": name,
      "audio": micAudio,
      "internalaudio": internalAudio,
      "title": notificationTitle,
      "message": notificationMessage,
    });
    return start;
  }

  Future<String> get stopRecordScreen async {
    final String path = await _channel.invokeMethod('stopRecordScreen');
    return path;
  }

  Future<String> createJsonFile(
      {required String filePath,
      required String appGroupIdentifier,
      required String pathDirectory,
      required String jsonFileName}
      ) async {
    if (!Platform.isIOS){
      throw Exception("This method is only available on iOS");
    }
    final String path = await _channel.invokeMethod('createJsonFile', {
      "filePath": filePath,
      "appGroupIdentifier": appGroupIdentifier,
      "pathDirectory": pathDirectory,
      "jsonFileName": jsonFileName,
    });
    return path;
  }
}
