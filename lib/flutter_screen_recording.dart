import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter_foreground_task/flutter_foreground_task.dart';
import 'package:flutter_screen_recording_platform_interface/flutter_screen_recording_platform_interface.dart';

class FlutterScreenRecording {
  static Future<bool> startRecordScreen(
    String name, {
    String? titleNotification,
    String? messageNotification,
    bool micAudio = false,
    bool internalAudio = false,
  }) async {
    if (titleNotification == null) {
      titleNotification = name;
      if (titleNotification == "") titleNotification = "Recording";
    }
    if (messageNotification == null) {
      messageNotification = "";
    }

    await _maybeStartFGS(titleNotification, messageNotification);
    final bool start = await FlutterScreenRecordingPlatform.instance.startRecordScreen(
      name,
      notificationTitle: titleNotification,
      notificationMessage: messageNotification,
      micAudio: micAudio,
      internalAudio: internalAudio,
    );

    return start;
  }

  static Future<String> get stopRecordScreen async {
    final String path = await FlutterScreenRecordingPlatform.instance.stopRecordScreen;
    if (!kIsWeb && Platform.isAndroid) {
      FlutterForegroundTask.stopService();
    }
    if (path == "") {
      throw Exception("Error: Stop recording failed.");
    }
    return path;
  }


  static Future<String> createJson({
    required String fileName,
    required String appGroupIdentifier,
    required String pathDirectory,
    required String jsonFileName,
  }) async {
    final String path = await FlutterScreenRecordingPlatform.instance.createJsonFile(fileName: fileName, appGroupIdentifier: appGroupIdentifier, pathDirectory: pathDirectory, jsonFileName: jsonFileName);
    if (path == "") {
      throw Exception("Error: Create json file failed.");
    }
    return path;
  }



  static _maybeStartFGS(
      String titleNotification, String messageNotification) async {
    if (!kIsWeb && Platform.isAndroid) {
      FlutterForegroundTask.init(
        androidNotificationOptions: AndroidNotificationOptions(
          channelId: 'notification_channel_id',
          channelName: titleNotification,
          channelDescription: messageNotification,
          channelImportance: NotificationChannelImportance.LOW,
          priority: NotificationPriority.LOW,
          iconData: const NotificationIconData(
            resType: ResourceType.mipmap,
            resPrefix: ResourcePrefix.ic,
            name: 'launcher',
          ),
          buttons: [
            // const NotificationButton(id: 'sendButton', text: 'Send'),
            // const NotificationButton(id: 'testButton', text: 'Test'),
          ],
        ),
        iosNotificationOptions: const IOSNotificationOptions(
          showNotification: true,
          playSound: false,
        ),
        foregroundTaskOptions: const ForegroundTaskOptions(
          interval: 5000,
          autoRunOnBoot: true,
          allowWifiLock: true,
        ),
        //iosNotificationOptions:true,
        //intDevLog: true,
      );
    }
  }

  static void globalForegroundService() {
    print("current datetime is ${DateTime.now()}");
  }
}
