package example.android.app.src.main.java.io.flutter.plugins;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import io.flutter.Log;

import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.plugins.shim.ShimPluginRegistry;

/**
 * Generated file. Do not edit.
 * This file is generated by the Flutter tool based on the
 * plugins that support the Android platform.
 */
@Keep
public final class GeneratedPluginRegistrant {
  private static final String TAG = "GeneratedPluginRegistrant";
  public static void registerWith(@NonNull FlutterEngine flutterEngine) {
    ShimPluginRegistry shimPluginRegistry = new ShimPluginRegistry(flutterEngine);
    try {
      flutterEngine.getPlugins().add(new com.pravera.flutter_foreground_task.FlutterForegroundTaskPlugin());
    } catch (Exception e) {
      Log.e(TAG, "Error registering plugin flutter_foreground_task, com.pravera.flutter_foreground_task.FlutterForegroundTaskPlugin", e);
    }
    try {
      com.isvisoft.flutter_screen_recording.FlutterScreenRecordingPlugin.registerWith(shimPluginRegistry.registrarFor("com.isvisoft.flutter_screen_recording.FlutterScreenRecordingPlugin"));
    } catch (Exception e) {
      Log.e(TAG, "Error registering plugin flutter_screen_recording, com.isvisoft.flutter_screen_recording.FlutterScreenRecordingPlugin", e);
    }
    try {
      flutterEngine.getPlugins().add(new com.crazecoder.openfile.OpenFilePlugin());
    } catch (Exception e) {
      Log.e(TAG, "Error registering plugin open_file, com.crazecoder.openfile.OpenFilePlugin", e);
    }
    try {
      flutterEngine.getPlugins().add(new com.baseflow.permissionhandler.PermissionHandlerPlugin());
    } catch (Exception e) {
      Log.e(TAG, "Error registering plugin permission_handler, com.baseflow.permissionhandler.PermissionHandlerPlugin", e);
    }
    try {
      flutterEngine.getPlugins().add(new io.flutter.plugins.sharedpreferences.SharedPreferencesPlugin());
    } catch (Exception e) {
      Log.e(TAG, "Error registering plugin shared_preferences_android, io.flutter.plugins.sharedpreferences.SharedPreferencesPlugin", e);
    }
  }
}
