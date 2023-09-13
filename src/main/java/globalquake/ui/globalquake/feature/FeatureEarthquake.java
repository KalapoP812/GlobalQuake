package globalquake.ui.globalquake.feature;

import globalquake.core.earthquake.Earthquake;
import globalquake.geo.GeoUtils;
import globalquake.geo.taup.TauPTravelTimeCalculator;
import globalquake.ui.globe.GlobeRenderer;
import globalquake.ui.globe.Point2D;
import globalquake.ui.globe.RenderProperties;
import globalquake.ui.globe.feature.RenderElement;
import globalquake.ui.globe.feature.RenderEntity;
import globalquake.ui.globe.feature.RenderFeature;
import globalquake.ui.settings.Settings;
import globalquake.utils.Scale;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class FeatureEarthquake extends RenderFeature<Earthquake> {

    private final List<Earthquake> earthquakes;

    public static final DecimalFormat f1d = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.ENGLISH));

    public FeatureEarthquake(List<Earthquake> earthquakes) {
        super(5);
        this.earthquakes = earthquakes;
    }

    @Override
    public Collection<Earthquake> getElements() {
        return earthquakes;
    }

    @Override
    public void createPolygon(GlobeRenderer renderer, RenderEntity<Earthquake> entity, RenderProperties renderProperties) {
        RenderElement elementPWave = entity.getRenderElement(0);
        RenderElement elementSWave = entity.getRenderElement(1);
        RenderElement elementPKPWave = entity.getRenderElement(2);
        RenderElement elementPKIKPWave = entity.getRenderElement(3);
        RenderElement elementCross = entity.getRenderElement(4);

        Earthquake e = entity.getOriginal();

        long age = System.currentTimeMillis() - e.getOrigin();
        double pDist = TauPTravelTimeCalculator.getPWaveTravelAngle(e.getDepth(), age / 1000.0) / 360.0
                * GeoUtils.EARTH_CIRCUMFERENCE;
        double sDist = TauPTravelTimeCalculator.getSWaveTravelAngle(e.getDepth(), age / 1000.0) / 360.0
                * GeoUtils.EARTH_CIRCUMFERENCE;
        double pkpDist = TauPTravelTimeCalculator.getPKPWaveTravelAngle(e.getDepth(), age / 1000.0) / 360.0
                * GeoUtils.EARTH_CIRCUMFERENCE;
        double pkikpDist = TauPTravelTimeCalculator.getPKIKPWaveTravelAngle(e.getDepth(), age / 1000.0) / 360.0
                * GeoUtils.EARTH_CIRCUMFERENCE;

        renderer.createCircle(elementPWave.getPolygon(),
                entity.getOriginal().getLat(),
                entity.getOriginal().getLon(),
                Math.max(0, pDist), 0, GlobeRenderer.QUALITY_HIGH);

        renderer.createCircle(elementSWave.getPolygon(),
                entity.getOriginal().getLat(),
                entity.getOriginal().getLon(),
                Math.max(0, sDist), 0, GlobeRenderer.QUALITY_HIGH);

        renderer.createCircle(elementPKPWave.getPolygon(),
                entity.getOriginal().getLat(),
                entity.getOriginal().getLon(),
                Math.max(0, pkpDist), 0, GlobeRenderer.QUALITY_HIGH);

        renderer.createCircle(elementPKIKPWave.getPolygon(),
                entity.getOriginal().getLat(),
                entity.getOriginal().getLon(),
                Math.max(0, pkikpDist), 0, GlobeRenderer.QUALITY_HIGH);

        renderer.createCross(elementCross.getPolygon(),
                entity.getOriginal().getLat(),
                entity.getOriginal().getLon(), renderer
                        .pxToDeg(16), 45.0);
    }

    @Override
    public boolean needsCreatePolygon(RenderEntity<Earthquake> entity, boolean propertiesChanged) {
        return true;
    }

    @Override
    public boolean needsProject(RenderEntity<Earthquake> entity, boolean propertiesChanged) {
        return true;
    }

    @Override
    public boolean needsUpdateEntities() {
        return true;
    }

    @Override
    public void project(GlobeRenderer renderer, RenderEntity<Earthquake> entity) {
        for (int i = 0; i <= 4; i++) {
            RenderElement elementPWave = entity.getRenderElement(i);
            elementPWave.getShape().reset();
            elementPWave.shouldDraw = renderer.project3D(elementPWave.getShape(), elementPWave.getPolygon(), true);
        }
    }

    @Override
    public void render(GlobeRenderer renderer, Graphics2D graphics, RenderEntity<Earthquake> entity) {
        float thicknessMultiplier = (float) Math.max(0.3, Math.min(1.6, entity.getOriginal().getMag() / 5.0));
        RenderElement elementPWave = entity.getRenderElement(0);
        RenderElement elementSWave = entity.getRenderElement(1);
        RenderElement elementPKPWave = entity.getRenderElement(2);
        RenderElement elementPKIKPWave = entity.getRenderElement(3);

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (elementPWave.shouldDraw) {
            graphics.setColor(Color.BLUE);
            graphics.setStroke(new BasicStroke(4.0f *thicknessMultiplier));
            graphics.draw(elementPWave.getShape());
        }

        if (elementSWave.shouldDraw) {
            graphics.setColor(getColorSWave(entity.getOriginal().getMag()));
            graphics.setStroke(new BasicStroke(4.0f * thicknessMultiplier));
            graphics.draw(elementSWave.getShape());
        }

        if(Settings.displayCoreWaves) {
            if (elementPKPWave.shouldDraw) {
                graphics.setColor(Color.MAGENTA);
                graphics.setStroke(new BasicStroke(4.0f * thicknessMultiplier));
                graphics.draw(elementPKPWave.getShape());
            }

            if (elementPKIKPWave.shouldDraw) {
                graphics.setColor(Color.GREEN);
                graphics.setStroke(new BasicStroke(1.0f));
                graphics.draw(elementPKIKPWave.getShape());
            }
        }

        RenderElement elementCross = entity.getRenderElement(4);
        if (elementCross.shouldDraw && (System.currentTimeMillis() / 500) % 2 == 0) {
            graphics.setColor(getCrossColor(entity.getOriginal().getMag()));
            graphics.setStroke(new BasicStroke(4f));
            graphics.draw(elementCross.getShape());

            var point3D = GlobeRenderer.createVec3D(getCenterCoords(entity));
            var centerPonint = renderer.projectPoint(point3D);

            String str = "M%s".formatted(f1d.format(entity.getOriginal().getMag()));

            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font("Calibri", Font.BOLD, 16));
            graphics.drawString(str, (int) (centerPonint.x - graphics.getFontMetrics().stringWidth(str) / 2), (int) (centerPonint.y - 18));

            str = "%skm".formatted(f1d.format(entity.getOriginal().getDepth()));

            graphics.drawString(str, (int) (centerPonint.x - graphics.getFontMetrics().stringWidth(str) / 2), (int) (centerPonint.y + 29));
        }

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    private Color getColorSWave(double mag) {
        double weight = Math.max(0, Math.min(1, (mag - 2.0) / 4.0));
        return Scale.interpolateColors(Color.yellow, Color.red, weight);
    }

    public static Color getCrossColor(double mag) {
        if (mag < 3) {
            return Color.lightGray;
        }
        if (mag < 4) {
            return Color.green;
        }
        if (mag < 5) {
            return Color.yellow;
        }
        if (mag < 6) {
            return Color.orange;
        }
        if(mag < 7){
            return Color.red;
        }
        return Color.magenta;
    }


    @Override
    public Point2D getCenterCoords(RenderEntity<?> entity) {
        return new Point2D(((Earthquake) (entity.getOriginal())).getLat(), ((Earthquake) (entity.getOriginal())).getLon());
    }
}
