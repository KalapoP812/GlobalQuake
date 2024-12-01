package gqserver.push_notification;

import globalquake.core.GlobalQuake;
import globalquake.core.earthquake.data.Earthquake;
import globalquake.core.events.GlobalQuakeEventListener;
import globalquake.core.events.specific.*;
import globalquake.core.geo.DistanceUnit;
import globalquake.core.geo.taup.TauPTravelTimeCalculator;
import globalquake.core.intensity.IntensityScales;
import globalquake.core.intensity.Level;
import globalquake.utils.GeoUtils;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import globalquake.core.Settings;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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

        sendNotification(Title, createDescription(earthquake), Settings.pushoverNearbyShakingPriorityList,
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

        sendNotification(Title, createDescription(earthquake), -1, false, null);
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

        String Title = "Final quake report";

        sendNotification(Title, createDescription(earthquake), Settings.pushoverNearbyShakingPriorityList,
                Settings.usePushoverCustomSounds, Settings.pushoverSoundDetected);
    }

    private static void sendQuakeRemoveInfo(Earthquake earthquake) {
        if (!Settings.usePushover || !isQuakeNearby(earthquake)) {
            return;
        }

        String Title = "Cancelled Earthquake";
        String message = "M%.1f %s".formatted(earthquake.getMag(), earthquake.getRegion());

        sendNotification(Title, message, Settings.pushoverNearbyShakingPriorityList, false, null);
    }

    private static void sendQuakeRemoveInfoEEW(Earthquake earthquake) {
        if (!Settings.usePushover) {
            return;
        }

        String Title = "Early Earthquake Warning Cancelled";
        String message = "M%.1f %s".formatted(earthquake.getMag(), earthquake.getRegion());

        sendNotification(Title, message, 0, false, null);
    }

    private static void sendNotification(String title, String description, int priority, boolean useCustomSounds, String customSound) {
        if (!Settings.usePushover) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            String urlParametersPushover = "token=" + Settings.pushoverToken +
                    "&user=" + Settings.pushoverUserID +
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
    private static String formatLevel(Level level) {
        if (level == null) {
            return "-";
        } else {
            return level.toString();
        }
    }

    public static void startNotification() {
        String urlParametersPushover = "token=" + Settings.pushoverToken +
                "&user=" + Settings.pushoverUserID +
                "&priority=" + 0 +
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
