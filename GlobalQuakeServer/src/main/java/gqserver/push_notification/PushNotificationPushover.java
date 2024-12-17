package gqserver.push_notification;

import globalquake.core.GlobalQuake;
import globalquake.core.analysis.Event;
import globalquake.core.earthquake.data.Earthquake;
import globalquake.core.events.GlobalQuakeEventListener;
import globalquake.core.events.specific.*;
import globalquake.core.geo.DistanceUnit;
import globalquake.core.geo.taup.TauPTravelTimeCalculator;
import globalquake.core.intensity.IntensityScales;
import globalquake.core.intensity.Level;
import globalquake.core.report.EarthquakeReporter;
import globalquake.core.report.StationReport;
import globalquake.core.station.AbstractStation;
import globalquake.utils.GeoUtils;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import globalquake.core.Settings;
import org.tinylog.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;


public class PushNotificationPushover extends ListenerAdapter {

    private static final String PUSHOVER_URL = "https://api.pushover.net/1/messages.json";
    private static final Set<Earthquake> measuredQuakes = new HashSet<>();

    private static String homeMMI;
    private static String homeShindo;

    private static String [][] earthquakeList = new String[100][3];
    /*[x][0]: Quake ID
    [x][1]: Intensity at home location
    [x][2]: Max intensity at home location*/
    private static int currentEarthquake = -1;

    public static final File ANALYSIS_FOLDER = new File(GlobalQuake.mainFolder, "/volume/events/");
    private static final DateTimeFormatter fileFormat = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss").withZone(ZoneId.systemDefault());

    public static void init() {
        if (Settings.pushoverFeltShaking || Settings.pushoverNearbyShaking) {
            GlobalQuake.instance.getEventHandler().registerEventListener(new GlobalQuakeEventListener() {
                @Override
                public void onQuakeCreate(QuakeCreateEvent event) {
                    if (currentEarthquake < 99) {
                        currentEarthquake++;
                    } else {
                        currentEarthquake = 0;
                    }
                    earthquakeList[currentEarthquake][0] = String.valueOf(event.earthquake().getUuid());
                    earthquakeList[currentEarthquake][1] = String.valueOf(determineHomeShakingIntensity(event.earthquake()));
                    earthquakeList[currentEarthquake][2] = earthquakeList[currentEarthquake][1];

                    createMap(event.earthquake());

                    if ((Settings.pushoverNearbyShaking && Settings.pushoverSendRevisions) && Settings.pushoverFeltShaking) {
                        if (earthquakeList[currentEarthquake][1].equals("0")) {
                            sendQuakeCreateInfo(event.earthquake());
                        } else {
                            sendQuakeCreateInfoEEW(event.earthquake());
                        }
                    } else if (Settings.pushoverNearbyShaking && Settings.pushoverSendRevisions) {
                        sendQuakeCreateInfo(event.earthquake());
                    } else if (Settings.pushoverFeltShaking) {
                        if (!earthquakeList[currentEarthquake][1].equals("0")) {
                            sendQuakeCreateInfoEEW(event.earthquake());
                        }
                    }
                }

                @Override
                public void onQuakeUpdate(QuakeUpdateEvent event) {
                    for (int i = 0; i < 99; i++) {
                        if (earthquakeList[i][0] != null && earthquakeList[i][0].equals(String.valueOf(event.earthquake().getUuid()))) {
                            earthquakeList[i][1] = String.valueOf(determineHomeShakingIntensity(event.earthquake()));

                            createMap(event.earthquake());

                            if ((Settings.pushoverNearbyShaking && Settings.pushoverSendRevisions) && Settings.pushoverFeltShaking) {
                                if (earthquakeList[currentEarthquake][1].equals("0") && earthquakeList[currentEarthquake][2].equals("0")) {
                                    sendQuakeUpdateInfo(event.earthquake());
                                } else {
                                    determineType(event, i);
                                }
                            } else if (Settings.pushoverNearbyShaking && Settings.pushoverSendRevisions) {
                                sendQuakeUpdateInfo(event.earthquake());
                            } else if (Settings.pushoverFeltShaking) {
                                determineType(event, i);
                            }

                            if (Integer.parseInt(earthquakeList[i][2]) < Integer.parseInt(earthquakeList[i][1])) {
                                earthquakeList[i][2] = earthquakeList[i][1];
                            }
                        }
                    }
                }

                @Override
                public void onQuakeArchive(QuakeArchiveEvent event) {
                    if (Settings.pushoverNearbyShaking)
                        sendQuakeReportInfo(event.earthquake());
                }

                @Override
                public void onQuakeRemove(QuakeRemoveEvent event) {
                    for (int i = 0; i < 99; i++) {
                        if (earthquakeList[i][0] != null && earthquakeList[i][0].equals(String.valueOf(event.earthquake().getUuid()))) {
                            if ((Settings.pushoverNearbyShaking && Settings.pushoverSendRevisions) && Settings.pushoverFeltShaking) {
                                if (earthquakeList[currentEarthquake][1].equals("0") && earthquakeList[currentEarthquake][2].equals("0")) {
                                    sendQuakeRemoveInfo(event.earthquake());
                                } else {
                                    sendQuakeRemoveInfoEEW(event.earthquake());
                                }
                            } else if (Settings.pushoverNearbyShaking && Settings.pushoverSendRevisions) {
                                sendQuakeRemoveInfo(event.earthquake());
                            } else if (Settings.pushoverFeltShaking) {
                                if (!earthquakeList[currentEarthquake][1].equals("0")) {
                                    sendQuakeRemoveInfoEEW(event.earthquake());
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    private static void createMap(Earthquake earthquake){
        File folder = new File(ANALYSIS_FOLDER, earthquake.getUuid() + "/");
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                Logger.error("Unable to create directory for reports! %s".formatted(folder.getAbsolutePath()));
                return;
            }
        }

        for (Event e : earthquake.getCluster().getAssignedEvents().values()) {
            AbstractStation station = e.getAnalysis().getStation();
            e.report = new StationReport(station.getNetworkCode(), station.getStationCode(),
                    station.getChannelName(), station.getLocationCode(), station.getLatitude(), station.getLongitude(),
                    station.getAlt());
        }

        EarthquakeReporter.drawMap(folder, earthquake);
    }

    private static void removeTempFolder(Earthquake earthquake) {
        File folder = new File(ANALYSIS_FOLDER, earthquake.getUuid() + "/");
        if (folder.exists()) {
            deleteDirectory(folder);
        }
    }

    private static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) { // Check if directory is not empty
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    if (!file.delete()) {
                        Logger.error("Unable to delete file: %s".formatted(file.getAbsolutePath()));
                    }
                }
            }
        }
        if (!directory.delete()) {
            Logger.error("Unable to delete directory: %s".formatted(directory.getAbsolutePath()));
        }
    }

    private static List<String> createUserIDList() {
        String[] userIds = Settings.pushoverUserID.split(",");
        return Arrays.asList(userIds);
    }

    private static void determineType(QuakeUpdateEvent event, int i) {
        // if current earthquake was not felt but now is, send notification
        if (Integer.parseInt(earthquakeList[i][1]) > 0 && Integer.parseInt(earthquakeList[i][2]) == 0) {
            sendQuakeCreateInfoEEW(event.earthquake());
        // if current earthquake was shown as felt but shaking is not expected, send notification
        } else if (Integer.parseInt(earthquakeList[i][1]) == 0 && Integer.parseInt(earthquakeList[i][2]) > 0) {
            sendQuakeUpdateInfoEEW(event.earthquake(), i);
        // otherwise, update the notification
        } else if (Integer.parseInt(earthquakeList[i][1]) > 0) {
            sendQuakeUpdateInfoEEW(event.earthquake(), i);
        }
    }

    private static void sendQuakeCreateInfo(Earthquake earthquake) {
        if (!Settings.usePushover || !isQuakeNearby(earthquake)) {
            return;
        }

        String Title = "Earthquake Detected";

        File folder = new File(ANALYSIS_FOLDER, earthquake.getUuid() + "/");

        sendNotificationWithImage(folder, Title, createDescription(earthquake), Settings.pushoverNearbyShakingPriorityList,
                Settings.usePushoverCustomSounds, Settings.pushoverSoundDetected);
    }

    private static void sendQuakeCreateInfoEEW(Earthquake earthquake) {
        if (!Settings.usePushover) {
            return;
        }

        String Title = "";
        String customSound = "";
        int priority = -1;

        if (earthquakeList[currentEarthquake][1].equals("1")) {
            priority = Settings.pushoverLightShakingPriorityList;
            customSound = Settings.pushoverSoundFeltLight;
            Title = "Light shaking is expected";
        } else if (earthquakeList[currentEarthquake][1].equals("2")) {
            priority = Settings.pushoverStrongShakingPriorityList;
            customSound = Settings.pushoverSoundFeltStrong;
            Title = "Strong shaking is expected";
        }

        sendNotification(Title, createDescription(earthquake), priority, Settings.usePushoverCustomSounds, customSound);
    }

    private static void sendQuakeUpdateInfo(Earthquake earthquake) {
        if (!Settings.usePushover || !isQuakeNearby(earthquake)) {
            return;
        }

        String Title = "Revision #%d".formatted(earthquake.getRevisionID());

        File folder = new File(ANALYSIS_FOLDER, earthquake.getUuid() + "/");

        sendNotificationWithImage(folder, Title, createDescription(earthquake), -1, false, null);
    }

    private static void sendQuakeUpdateInfoEEW(Earthquake earthquake, int currentEarthquakeUpdate) {
        if (!Settings.usePushover) {
            return;
        }

        String Title = "";
        String customSound = "";
        int priority = -1; // do not make an audible notification when revising, unless shaking intensity increased
        boolean useCustomSounds = false;

        Title = switch (earthquakeList[currentEarthquakeUpdate][1]) {
            case "0" -> "No shaking is expected";
            case "1" -> "Light shaking is expected";
            case "2" -> "Strong shaking is expected";
            default -> Title;
        };

        // if shaking intensity increased, prioritize the notification
        if (Integer.parseInt(earthquakeList[currentEarthquakeUpdate][2]) < Integer.parseInt(earthquakeList[currentEarthquakeUpdate][1])) {
            priority = Settings.pushoverStrongShakingPriorityList;
            customSound = Settings.pushoverSoundFeltStrong;
            useCustomSounds = true;
        }

        sendNotification(Title, createDescription(earthquake), priority, useCustomSounds, customSound);
    }

    private static void sendQuakeReportInfo(Earthquake earthquake) {
        if (!Settings.usePushover || !isQuakeNearby(earthquake)) {
            return;
        }

        File folder = new File(ANALYSIS_FOLDER, earthquake.getUuid() + "/");

        String Title = "Final quake report";

        CompletableFuture<Void> future = sendNotificationWithImage(folder, Title, createDescription(earthquake), Settings.pushoverNearbyShakingPriorityList,
                Settings.usePushoverCustomSounds, Settings.pushoverSoundDetected);

        future.thenRun(() -> removeTempFolder(earthquake));
    }

    private static void sendQuakeRemoveInfo(Earthquake earthquake) {
        if (!Settings.usePushover || !isQuakeNearby(earthquake)) {
            return;
        }

        String Title = "Cancelled Earthquake";
        String message = "M%.1f %s".formatted(earthquake.getMag(), earthquake.getRegion());

        sendNotification(Title, message, Settings.pushoverNearbyShakingPriorityList, false, null);

        removeTempFolder(earthquake);
    }

    private static void sendQuakeRemoveInfoEEW(Earthquake earthquake) {
        if (!Settings.usePushover) {
            return;
        }

        String Title = "Early Earthquake Warning Cancelled";
        String message = "M%.1f %s".formatted(earthquake.getMag(), earthquake.getRegion());

        sendNotification(Title, message, 0, false, null);
    }
    // Only to be used for EEWs as it is faster without image
    private static void sendNotification(String title, String description, int priority, boolean useCustomSounds, String customSound) {
        if (!Settings.usePushover) {
            return;
        }

        List<String> userIdList = createUserIDList();

        for (String userId : userIdList) {
            CompletableFuture.runAsync(() -> {
                String urlParametersPushover = "token=" + Settings.pushoverToken +
                        "&user=" + userId +
                        "&priority=" + priority +
                        "&title=" + title +
                        "&message=" + description;
                if (useCustomSounds) {
                    urlParametersPushover += "&sound=" + customSound;
                }
                try {
                    URL url = new URL(PUSHOVER_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setDoOutput(true);

                    byte[] postData = urlParametersPushover.getBytes(StandardCharsets.UTF_8);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(postData, 0, postData.length);
                    }

                    int responseCode = conn.getResponseCode();
                    System.out.println("Pushover detected earthquake nearby. Response Code: " + responseCode);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private static CompletableFuture<Void> sendNotificationWithImage(File folder, String title, String description, int priority, boolean useCustomSounds, String customSound) {
        if (!Settings.usePushover) {
            return CompletableFuture.completedFuture(null);
        }

        List<String> userIdList = createUserIDList();
        String boundary = Long.toHexString(System.currentTimeMillis());
        String CRLF = "\r\n";
        String filePath = folder.getAbsolutePath() + "/map.png";

        while (!new File(filePath).exists()) { // Wait for the image to be created
            try {
                Thread.sleep(1000); // Wait for 1 second before checking again
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (!useCustomSounds) customSound = "";
        String finalCustomSound = customSound;

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String userId : userIdList) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    URL url = new URL(PUSHOVER_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                    conn.setDoOutput(true);

                    try (OutputStream os = conn.getOutputStream()) {
                        String[] params = {"token", Settings.pushoverToken, "user", userId, "priority", String.valueOf(priority),
                                "sound", finalCustomSound, "title", title, "message", description};
                        for (int i = 0; i < params.length; i += 2) {
                            os.write(("--" + boundary + CRLF).getBytes(StandardCharsets.UTF_8));
                            os.write(("Content-Disposition: form-data; name=\"" + params[i] + "\"" + CRLF).getBytes(StandardCharsets.UTF_8));
                            os.write(("Content-Type: text/plain; charset=UTF-8" + CRLF + CRLF).getBytes(StandardCharsets.UTF_8));
                            os.write((params[i + 1] + CRLF).getBytes(StandardCharsets.UTF_8));
                        }

                        os.write(("--" + boundary + CRLF).getBytes(StandardCharsets.UTF_8));
                        os.write(("Content-Disposition: form-data; name=\"attachment\"; filename=\"" + filePath + "\"" + CRLF).getBytes(StandardCharsets.UTF_8));
                        os.write(("Content-Type: application/octet-stream" + CRLF + CRLF).getBytes(StandardCharsets.UTF_8));

                        try (FileInputStream fis = new FileInputStream(filePath)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = fis.read(buffer)) != -1) {
                                os.write(buffer, 0, bytesRead);
                            }
                        }

                        os.write((CRLF + "--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8));
                    }

                    System.out.println("Message with Image. Response Code: " + conn.getResponseCode());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private static String formatLevel(Level level) {
        if (level == null) {
            return "-";
        } else {
            return level.toString();
        }
    }

    public static void startNotification() {
        List<String> userIdList = createUserIDList();

        for (String userId : userIdList) {
            CompletableFuture.runAsync(() -> {
                String urlParametersPushover = "token=" + Settings.pushoverToken +
                        "&user=" + userId +
                        "&priority=" + -1 +
                        "&title=Server Starting" +
                        "&message=This may take a few minutes to complete.";
                try {
                    URL url = new URL(PUSHOVER_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setDoOutput(true);

                    byte[] postData = urlParametersPushover.getBytes(StandardCharsets.UTF_8);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(postData, 0, postData.length);
                    }

                    int responseCode = conn.getResponseCode();
                    System.out.println("Start message. Response Code: " + responseCode);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private static long bestDetectionTime = Long.MAX_VALUE;
    private static double detectionTimeSum = 0.0;
    private static int detections = 0;

    private static String createDescription(Earthquake earthquake) {
        String description = "M%.1f %s".formatted(
                earthquake.getMag(),
                earthquake.getRegion());

        double pga = GeoUtils.getMaxPGA(earthquake.getLat(), earthquake.getLon(), earthquake.getDepth(), earthquake.getMag());

        long detectionTime = earthquake.getCreatedAt() - earthquake.getOrigin();
        if (detectionTime < bestDetectionTime) {
            bestDetectionTime = detectionTime;
        }

        if (!measuredQuakes.contains(earthquake)) {
            detections++;
            detectionTimeSum += detectionTime / 1000.0;
            measuredQuakes.add(earthquake);
        }

        description += "\n" +
                "Depth: %.1fkm / %.1fmi\n".formatted(earthquake.getDepth(), earthquake.getDepth() * DistanceUnit.MI.getKmRatio()) +
                "MMI: %s / Shindo: %s\n".formatted(formatLevel(IntensityScales.MMI.getLevel(pga)),
                        formatLevel(IntensityScales.SHINDO.getLevel(pga))) +
                "Home MMI: %s / Shindo: %s\n".formatted(homeMMI, homeShindo) +
                "SWave Arrival: %ds\n".formatted(calculateSWaveArrival(earthquake)) +
                "Time: %s\n".formatted(Settings.formatDateTime(Instant.ofEpochMilli(earthquake.getOrigin()))) +
                "Detection Time: %.1fs (best %.1fs, avg %.1fs)\n".formatted(detectionTime / 1000.0, bestDetectionTime / 1000.0, detectionTimeSum / detections) +
                "Quality: %s (%d stations)".formatted(earthquake.getCluster().getPreviousHypocenter().quality.getSummary(), earthquake.getCluster().getAssignedEvents().size());

        return description;
    }

    private static int determineHomeShakingIntensity(Earthquake earthquake) {
        double dist = GeoUtils.geologicalDistance(earthquake.getLat(), earthquake.getLon(), -earthquake.getDepth(), Settings.homeLat, Settings.homeLon, 0);
        double pga = GeoUtils.pgaFunction(earthquake.getMag(), dist, earthquake.getDepth());

        homeMMI = formatLevel(IntensityScales.MMI.getLevel(pga));
        homeShindo = formatLevel(IntensityScales.SHINDO.getLevel(pga));

        if (pga >= IntensityScales.INTENSITY_SCALES[Settings.shakingLevelScale].getLevels().get(Settings.shakingLevelIndex).getPga()) {
            return 1; // weak shaking
        } else if (pga >= IntensityScales.INTENSITY_SCALES[Settings.strongShakingLevelScale].getLevels().get(Settings.strongShakingLevelIndex).getPga()) {
            return 2; // strong shaking
        } else {
            return 0; // no shaking
        }
    }

    private static int calculateSWaveArrival(Earthquake earthquake) {
        double distGC = GeoUtils.greatCircleDistance(earthquake.getLat(), earthquake.getLon(), Settings.homeLat, Settings.homeLon);
        double age = (GlobalQuake.instance.currentTimeMillis() - earthquake.getOrigin()) / 1000.0;

        double sTravel = TauPTravelTimeCalculator.getSWaveTravelTime(earthquake.getDepth(), TauPTravelTimeCalculator.toAngle(distGC));

        int sArrival = (int) Math.round(sTravel - age) - 1; // There is 1-second delay when sending the message

        return Math.max(sArrival, 0);
    }

    private static boolean isQuakeNearby(Earthquake earthquake) {
        double distGC = GeoUtils.greatCircleDistance(earthquake.getLat(), earthquake.getLon(), Settings.homeLat, Settings.homeLon);

        return distGC <= Settings.alertLocalDist || (distGC <= Settings.alertRegionDist && earthquake.getMag() >= Settings.alertRegionMag);
    }
}
