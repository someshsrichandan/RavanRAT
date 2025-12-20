package com.security.ravan;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.util.Locale;

public class DeviceInfo {

    @SuppressLint("MissingPermission")
    public static String getDeviceInfoHtml(Context c) {
        StringBuilder html = new StringBuilder();

        // Device Information Section
        html.append("<div class=\"info-section\">");
        html.append("<h3 style=\"color: #3498db; margin-bottom: 15px;\">&#128241; Device Information</h3>");
        html.append("<div class=\"info-grid\">");

        html.append(createInfoItem("Device Name", getDeviceName(c)));
        html.append(createInfoItem("Model", Build.MODEL));
        html.append(createInfoItem("Brand", Build.BRAND));
        html.append(createInfoItem("Manufacturer", Build.MANUFACTURER));
        html.append(createInfoItem("Board", Build.BOARD));
        html.append(createInfoItem("Hardware", Build.HARDWARE));
        html.append(createInfoItem("Display", Build.DISPLAY));
        html.append(createInfoItem("Device ID", Build.ID));

        html.append("</div></div>");

        // System Information Section
        html.append("<div class=\"info-section\">");
        html.append("<h3 style=\"color: #9b59b6; margin-bottom: 15px;\">&#9881; System Information</h3>");
        html.append("<div class=\"info-grid\">");

        html.append(createInfoItem("Android Version", Build.VERSION.RELEASE));
        html.append(createInfoItem("SDK Level", String.valueOf(Build.VERSION.SDK_INT)));
        html.append(createInfoItem("API Level", getAndroidVersionName(Build.VERSION.SDK_INT)));
        html.append(createInfoItem("Language", Locale.getDefault().getDisplayLanguage()));
        html.append(createInfoItem("Country", Locale.getDefault().getDisplayCountry()));
        html.append(createInfoItem("Bootloader", Build.BOOTLOADER));
        html.append(createInfoItem("Build Host", Build.HOST));

        html.append("</div></div>");

        // SIM / Network Section
        html.append("<div class=\"info-section\">");
        html.append("<h3 style=\"color: #e67e22; margin-bottom: 15px;\">&#128225; SIM &amp; Network</h3>");
        html.append("<div class=\"info-grid\">");

        try {
            TelephonyManager tm = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                String operator = tm.getNetworkOperatorName();
                html.append(createInfoItem("Operator", operator.isEmpty() ? "N/A" : operator));

                String simCountry = tm.getSimCountryIso();
                html.append(createInfoItem("SIM Country", simCountry.isEmpty() ? "N/A" : simCountry.toUpperCase()));

                String networkType = getNetworkType(c);
                html.append(createInfoItem("Network Type", networkType));

                int simState = tm.getSimState();
                html.append(createInfoItem("SIM State", getSimStateName(simState)));
            }
        } catch (Exception e) {
            html.append(createInfoItem("Network Info", "Permission denied"));
        }

        html.append("</div></div>");

        // WiFi Section
        html.append("<div class=\"info-section\">");
        html.append("<h3 style=\"color: #1abc9c; margin-bottom: 15px;\">&#128246; WiFi Information</h3>");
        html.append("<div class=\"info-grid\">");

        try {
            WifiManager wm = (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (wm != null && cm != null) {
                NetworkInfo ni = cm.getActiveNetworkInfo();
                if (ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI && ni.isConnected()) {
                    WifiInfo wi = wm.getConnectionInfo();
                    if (wi != null) {
                        String ssid = wi.getSSID();
                        html.append(createInfoItem("SSID", ssid != null ? ssid.replace("\"", "") : "N/A"));
                        html.append(createInfoItem("Link Speed", wi.getLinkSpeed() + " Mbps"));

                        int signalLevel = WifiManager.calculateSignalLevel(wi.getRssi(), 5);
                        html.append(createInfoItem("Signal Strength", signalLevel + "/5"));

                        html.append(createInfoItem("WiFi Enabled", wm.isWifiEnabled() ? "Yes" : "No"));
                    }
                } else {
                    html.append(
                            createInfoItem("WiFi Status", wm.isWifiEnabled() ? "Enabled (Not Connected)" : "Disabled"));
                }
            }
        } catch (Exception e) {
            html.append(createInfoItem("WiFi Info", "Permission denied"));
        }

        html.append("</div></div>");

        // Battery Section
        html.append("<div class=\"info-section\">");
        html.append("<h3 style=\"color: #2ecc71; margin-bottom: 15px;\">&#128267; Battery Status</h3>");
        html.append("<div class=\"info-grid\">");

        try {
            IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = c.registerReceiver(null, iFilter);

            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                int batteryPct = (level * 100) / scale;

                html.append(createInfoItem("Battery Level", batteryPct + "%"));

                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL;
                html.append(createInfoItem("Charging", isCharging ? "Yes" : "No"));

                int plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                String plugType = "Not Plugged";
                if (plugged == BatteryManager.BATTERY_PLUGGED_AC)
                    plugType = "AC Charger";
                else if (plugged == BatteryManager.BATTERY_PLUGGED_USB)
                    plugType = "USB";
                else if (plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS)
                    plugType = "Wireless";
                html.append(createInfoItem("Power Source", plugType));

                int health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
                html.append(createInfoItem("Battery Health", getBatteryHealthName(health)));

                int temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10;
                html.append(createInfoItem("Temperature", temperature + "°C"));
            }
        } catch (Exception e) {
            html.append(createInfoItem("Battery Info", "Unable to read"));
        }

        // Power Manager info
        try {
            PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    html.append(createInfoItem("Power Saver", pm.isPowerSaveMode() ? "Enabled" : "Disabled"));
                    html.append(createInfoItem("Idle Mode", pm.isDeviceIdleMode() ? "Yes" : "No"));
                }
                html.append(createInfoItem("Screen Active", pm.isInteractive() ? "Yes" : "No"));
            }
        } catch (Exception e) {
            // Ignore
        }

        html.append("</div></div>");

        // Audio Settings Section
        html.append("<div class=\"info-section\">");
        html.append("<h3 style=\"color: #e74c3c; margin-bottom: 15px;\">&#128266; Audio Settings</h3>");
        html.append("<div class=\"info-grid\">");

        try {
            AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                String ringerMode;
                switch (am.getRingerMode()) {
                    case AudioManager.RINGER_MODE_NORMAL:
                        ringerMode = "Normal";
                        break;
                    case AudioManager.RINGER_MODE_VIBRATE:
                        ringerMode = "Vibrate";
                        break;
                    case AudioManager.RINGER_MODE_SILENT:
                        ringerMode = "Silent";
                        break;
                    default:
                        ringerMode = "Unknown";
                }
                html.append(createInfoItem("Ringer Mode", ringerMode));

                int ringVol = am.getStreamVolume(AudioManager.STREAM_RING);
                int ringMax = am.getStreamMaxVolume(AudioManager.STREAM_RING);
                html.append(createInfoItem("Ring Volume", ringVol + "/" + ringMax));

                int mediaVol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                int mediaMax = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                html.append(createInfoItem("Media Volume", mediaVol + "/" + mediaMax));

                int notifVol = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
                int notifMax = am.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
                html.append(createInfoItem("Notification Volume", notifVol + "/" + notifMax));
            }
        } catch (Exception e) {
            html.append(createInfoItem("Audio Info", "Unable to read"));
        }

        html.append("</div></div>");

        return html.toString();
    }

    private static String createInfoItem(String label, String value) {
        return "<div class=\"info-item\">" +
                "<span class=\"info-label\">" + label + "</span>" +
                "<span class=\"info-value\">" + (value != null ? value : "N/A") + "</span>" +
                "</div>";
    }

    public static String getDeviceName(Context c) {
        try {
            String name = "";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                name = Settings.Global.getString(c.getContentResolver(), Settings.Global.DEVICE_NAME);
            }
            if (name == null || name.isEmpty()) {
                name = Build.MODEL;
            }
            return name;
        } catch (Exception e) {
            return Build.MODEL;
        }
    }

    private static String getAndroidVersionName(int sdk) {
        switch (sdk) {
            case 26:
                return "Oreo (8.0)";
            case 27:
                return "Oreo (8.1)";
            case 28:
                return "Pie (9)";
            case 29:
                return "Android 10";
            case 30:
                return "Android 11";
            case 31:
                return "Android 12";
            case 32:
                return "Android 12L";
            case 33:
                return "Android 13";
            case 34:
                return "Android 14";
            case 35:
                return "Android 15";
            default:
                return "API " + sdk;
        }
    }

    private static String getNetworkType(Context c) {
        try {
            ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo ni = cm.getActiveNetworkInfo();
                if (ni != null && ni.isConnected()) {
                    switch (ni.getType()) {
                        case ConnectivityManager.TYPE_WIFI:
                            return "WiFi";
                        case ConnectivityManager.TYPE_MOBILE:
                            return "Mobile Data";
                        case ConnectivityManager.TYPE_ETHERNET:
                            return "Ethernet";
                        default:
                            return ni.getTypeName();
                    }
                }
            }
            return "Not Connected";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private static String getSimStateName(int state) {
        switch (state) {
            case TelephonyManager.SIM_STATE_READY:
                return "Ready";
            case TelephonyManager.SIM_STATE_ABSENT:
                return "Absent";
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                return "PIN Required";
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                return "PUK Required";
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                return "Network Locked";
            default:
                return "Unknown";
        }
    }

    private static String getBatteryHealthName(int health) {
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD:
                return "Good";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                return "Overheat";
            case BatteryManager.BATTERY_HEALTH_DEAD:
                return "Dead";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                return "Over Voltage";
            case BatteryManager.BATTERY_HEALTH_COLD:
                return "Cold";
            default:
                return "Unknown";
        }
    }
}
