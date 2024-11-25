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


public class PushNotificationNtfy extends ListenerAdapter {

    private static final Set<Earthquake> measuredQuakes = new HashSet<>();

    private static String homeMMI;
    private static String homeShindo;

    private static String [][] earthquakeList = new String[100][3];
    /*[x][0]: Quake ID
    [x][1]: Intensity at home location
    [x][2]: Max intensity at home location*/
    private static int currentEarthquake = -1;

    public static void init() {
        if (Settings.ntfyFeltShaking || Settings.ntfyNearbyShaking) {
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

                    if (Settings.ntfyNearbyShaking && Settings.ntfyFeltShaking) {
                        if (earthquakeList[currentEarthquake][1].equals("0")) {
                            sendQuakeCreateInfo(event.earthquake());
                        } else {
                            sendQuakeCreateInfoEEW(event.earthquake());
                        }
                    } else if (Settings.ntfyNearbyShaking) {
                        sendQuakeCreateInfo(event.earthquake());
                    } else if (Settings.ntfyFeltShaking) {
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

                            if (Settings.ntfyNearbyShaking && Settings.ntfyFeltShaking) {
                                if (earthquakeList[currentEarthquake][1].equals("0") && earthquakeList[currentEarthquake][2].equals("0")) {
                                    sendQuakeUpdateInfo(event.earthquake());
                                } else {
                                    determineType(event, i);
                                }
                            } else if (Settings.ntfyNearbyShaking) {
                                sendQuakeUpdateInfo(event.earthquake());
                            } else if (Settings.ntfyFeltShaking) {
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
                    if (Settings.ntfyNearbyShaking)
                        sendQuakeReportInfo(event.earthquake());
                }

                @Override
                public void onQuakeRemove(QuakeRemoveEvent event) {
                    for (int i = 0; i < 99; i++) {
                        if (earthquakeList[i][0] != null && earthquakeList[i][0].equals(String.valueOf(event.earthquake().getUuid()))) {
                            if (Settings.ntfyNearbyShaking && Settings.ntfyFeltShaking) {
                                if (earthquakeList[currentEarthquake][1].equals("0") && earthquakeList[currentEarthquake][2].equals("0")) {
                                    sendQuakeRemoveInfo(event.earthquake());
                                } else {
                                    sendQuakeRemoveInfoEEW(event.earthquake());
                                }
                            } else if (Settings.ntfyNearbyShaking) {
                                sendQuakeRemoveInfo(event.earthquake());
                            } else if (Settings.ntfyFeltShaking) {
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
        if (!Settings.useNtfy || !isQuakeNearby(earthquake)) {
            return;
        }

        String Title = "Earthquake Detected";

        sendNotification(Title, createDescription(earthquake), Settings.ntfyNearbyShakingPriorityList);
    }

    private static void sendQuakeCreateInfoEEW(Earthquake earthquake) {
        if (!Settings.useNtfy) {
            return;
        }

        String Title = "";
        int priority = 2;

        if (earthquakeList[currentEarthquake][1].equals("1")) {
            priority = Settings.ntfyLightShakingPriorityList;
            Title = "Light shaking is expected";
        } else if (earthquakeList[currentEarthquake][1].equals("2")) {
            priority = Settings.ntfyStrongShakingPriorityList;
            Title = "Strong shaking is expected";
        }

        sendNotification(Title, createDescription(earthquake), priority);
    }

    private static void sendQuakeUpdateInfo(Earthquake earthquake) {
        if (!Settings.useNtfy || !isQuakeNearby(earthquake)) {
            return;
        }

        String Title = "Revision #%d".formatted(earthquake.getRevisionID());

        sendNotification(Title, createDescription(earthquake), 2);
    }

    private static void sendQuakeUpdateInfoEEW(Earthquake earthquake, int currentEarthquakeUpdate) {
        if (!Settings.useNtfy) {
            return;
        }

        String Title = "";
        int priority = 2; // do not make an audible notification when revising, unless shaking intensity increased

        Title = switch (earthquakeList[currentEarthquakeUpdate][1]) {
            case "0" -> "No shaking is expected";
            case "1" -> "Light shaking is expected";
            case "2" -> "Strong shaking is expected";
            default -> Title;
        };

        // if shaking intensity increased, prioritize the notification
        if (Integer.parseInt(earthquakeList[currentEarthquakeUpdate][2]) < Integer.parseInt(earthquakeList[currentEarthquakeUpdate][1])) {
            priority = Settings.ntfyStrongShakingPriorityList;
        }

        sendNotification(Title, createDescription(earthquake), priority);
    }

    private static void sendQuakeReportInfo(Earthquake earthquake) {
        if (!Settings.useNtfy || !isQuakeNearby(earthquake)) {
            return;
        }

        String Title = "Final quake report";

        sendNotification(Title, createDescription(earthquake), Settings.ntfyNearbyShakingPriorityList);
    }

    private static void sendQuakeRemoveInfo(Earthquake earthquake) {
        if (!Settings.useNtfy || !isQuakeNearby(earthquake)) {
            return;
        }

        String Title = "Cancelled Earthquake";
        String message = "M%.1f %s".formatted(earthquake.getMag(), earthquake.getRegion());

        sendNotification(Title, message, Settings.ntfyNearbyShakingPriorityList);
    }

    private static void sendQuakeRemoveInfoEEW(Earthquake earthquake) {
        if (!Settings.useNtfy) {
            return;
        }

        String Title = "Early Earthquake Warning Cancelled";
        String message = "M%.1f %s".formatted(earthquake.getMag(), earthquake.getRegion());

        sendNotification(Title, message, 3);
    }

    private static void sendNotification(String title, String description, int priority) {

        if (!Settings.useNtfy) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(Settings.ntfy);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("title", title);
                connection.setRequestProperty("priority", String.valueOf(priority));
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = description.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                System.out.println("Ntfy Detected earthquake nearby. Response Code: " + responseCode); // Debugging
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
        String title = "Server Starting";
        try {
            URL url = new URL(Settings.ntfy);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("title", title);
            connection.setRequestProperty("priority", "3");
            connection.setDoOutput(true);

            String message = "This may take a few minutes to complete.";

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = message.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            System.out.println("Start message. Response Code: " + responseCode); // Debugging
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
