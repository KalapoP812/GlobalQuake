package gqserver.push_notification;

import globalquake.core.GlobalQuake;
import globalquake.core.earthquake.data.Earthquake;
import globalquake.core.events.GlobalQuakeEventListener;
import globalquake.core.events.specific.QuakeCreateEvent;
import globalquake.core.events.specific.QuakeRemoveEvent;
import globalquake.core.events.specific.QuakeReportEvent;
import globalquake.core.events.specific.QuakeUpdateEvent;
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

    private static int homeShakingIntensity;
    private static int maxHomeShakingIntensity;
    private static String homeMMI;
    private static String homeShindo;

    public static void init() {
        // for felt earthquakes
        if (Settings.pushoverFeltShaking) {
            GlobalQuake.instance.getEventHandler().registerEventListener(new GlobalQuakeEventListener() {
                @Override
                public void onQuakeCreate(QuakeCreateEvent event) {
                    homeShakingIntensity = determineHomeShakingIntensity(event.earthquake());
                    // if newly detected earthquake is felt, send notification
                    if (homeShakingIntensity > 0) {
                        sendQuakeCreateInfoEEW(event.earthquake());
                        maxHomeShakingIntensity = homeShakingIntensity;
                    }
                }

                @Override
                public void onQuakeUpdate(QuakeUpdateEvent event) {
                    homeShakingIntensity = determineHomeShakingIntensity(event.earthquake());
                    // if current earthquake was not felt but now is, send notification
                    if (homeShakingIntensity > 0 && maxHomeShakingIntensity == 0) {
                        sendQuakeCreateInfoEEW(event.earthquake());
                        maxHomeShakingIntensity = homeShakingIntensity;
                        // if current earthquake was shown as felt but shaking is not expected, send notification
                    } else if (homeShakingIntensity == 0 && maxHomeShakingIntensity > 0) {
                        sendQuakeUpdateInfoEEW(event.earthquake());
                        // otherwise, update the notification
                    } else if (homeShakingIntensity > 0) {
                        sendQuakeUpdateInfoEEW(event.earthquake());
                    }
                }

                @Override
                public void onQuakeReport(QuakeReportEvent event) {
                    // after the earthquake is reported, reset the shaking intensity
                    homeShakingIntensity = 0;
                    maxHomeShakingIntensity = 0;
                }

                @Override
                public void onQuakeRemove(QuakeRemoveEvent event) {
                    sendQuakeRemoveInfoEEW(event.earthquake());
                    homeShakingIntensity = 0;
                    maxHomeShakingIntensity = 0;
                }
            });
        }
        // for nearby earthquakes
        if (Settings.pushoverNearbyShaking) {
            GlobalQuake.instance.getEventHandler().registerEventListener(new GlobalQuakeEventListener() {
                @Override
                public void onQuakeCreate(QuakeCreateEvent event) {
                    isQuakeNearby(event.earthquake());
                    sendQuakeCreateInfo(event.earthquake());
                }

                @Override
                public void onQuakeUpdate(QuakeUpdateEvent event) {
                    isQuakeNearby(event.earthquake());
                    sendQuakeUpdateInfo(event.earthquake());
                }

                @Override
                public void onQuakeReport(QuakeReportEvent event) {
                    isQuakeNearby(event.earthquake());
                    sendQuakeReportInfo(event.earthquake());
                }

                @Override
                public void onQuakeRemove(QuakeRemoveEvent event) {
                    isQuakeNearby(event.earthquake());
                    sendQuakeRemoveInfo(event.earthquake());
                }
            });
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

        if (homeShakingIntensity == 1) {
            priority = Settings.pushoverLightShakingPriorityList;
            customSound = Settings.pushoverSoundFeltLight;
            Title = "Light shaking is expected";
        } else if (homeShakingIntensity == 2) {
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

        sendNotification(Title, createDescription(earthquake), 0, false, null);
    }

    private static void sendQuakeUpdateInfoEEW(Earthquake earthquake) {
        if (!Settings.usePushover) {
            return;
        }

        String Title = "";
        String customSound = "";
        int priority = 2; // do not make an audible notification when revising, unless shaking intensity increased

        if (homeShakingIntensity == 0) {
            Title = "No shaking is expected";
        }
        else if (homeShakingIntensity == 1) {
            Title = "Light shaking is expected";
        } else if (homeShakingIntensity == 2) {
            Title = "Strong shaking is expected";
        }

        // if shaking intensity increased, prioritize the notification
        if (maxHomeShakingIntensity < homeShakingIntensity) {
            priority = Settings.pushoverStrongShakingPriorityList;
            customSound = Settings.pushoverSoundFeltStrong;
            maxHomeShakingIntensity = homeShakingIntensity;
        }

        sendNotification(Title, createDescription(earthquake), priority, false, customSound);
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

        sendNotification(Title, message, 0, false, null);
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
        double _dist = GeoUtils.geologicalDistance(earthquake.getLat(), earthquake.getLon(), -earthquake.getDepth(), Settings.homeLat, Settings.homeLon, 0);
        double pga = GeoUtils.pgaFunction(earthquake.getMag(), _dist, earthquake.getDepth());

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
