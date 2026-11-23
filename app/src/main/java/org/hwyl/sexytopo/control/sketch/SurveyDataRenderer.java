package org.hwyl.sexytopo.control.sketch;

import static org.hwyl.sexytopo.control.sketch.ScreenUnits.dpToPixels;
import static org.hwyl.sexytopo.control.sketch.SketchPaints.FADED_ALPHA;
import static org.hwyl.sexytopo.control.sketch.SketchPaints.SOLID_ALPHA;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.control.util.SketchPreferences;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.geometry.Line;
import org.hwyl.sexytopo.model.geometry.Space;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

/**
 * Draws the survey itself: legs and splays, and stations with their names, icons and the active
 * station's highlight.
 */
class SurveyDataRenderer {

    /** Something drawn at each station after the station itself, such as a cross-section's mark. */
    interface StationDecoration {
        void draw(Canvas canvas, Station station, float x, float y, int alpha);
    }

    private static final int STATION_STROKE_WIDTH_DP = 2;
    private static final int STATION_LABEL_OFFSET_DP = 10;
    // how long a name can hang in from off the left of the screen and still be drawn
    private static final int OFF_SCREEN_LABEL_ALLOWANCE_CHARACTERS = 40;

    private final Viewport viewport;
    private final SketchState state;

    private final Paint stationPaint = new Paint();
    private final Paint iconPaint = new Paint();

    private final Paint legPaint = new Paint();
    private final Paint latestLegPaint = new Paint();
    private final Paint splayPaint = new Paint();

    private final Paint fadedLegPaint = new Paint();
    private final Paint fadedLatestLegPaint = new Paint();
    private final Paint fadedSplayPaint = new Paint();

    private final Paint highlightPaint = new Paint();

    private final Paint[] ANTI_ALIAS_PAINTS =
            new Paint[] {
                stationPaint,
                iconPaint,
                legPaint,
                latestLegPaint,
                splayPaint,
                fadedLegPaint,
                fadedLatestLegPaint,
                fadedSplayPaint
            };

    private float stationCrossDiameterPx;
    private float stationLabelOffsetPx;

    // how far off screen a station can be and still have part of it on screen: on the left, its
    // name and icons can reach in; on the other sides only its cross and the edge of its name can
    private float offScreenReachLeft;
    private float offScreenReach;

    private Bitmap commentIcon;
    private Bitmap linkIcon;

    SurveyDataRenderer(Viewport viewport, SketchState state) {
        this.viewport = viewport;
        this.state = state;
    }

    /** Set the paints, sizes and icons up from the theme and current preferences. */
    void initialise(Context context) {

        SketchPaints.applyAntiAliasPreference(ANTI_ALIAS_PAINTS);

        int stationColour = ContextCompat.getColor(context, R.color.station);
        stationPaint.setColor(stationColour);
        float stationStrokeWidthPx = dpToPixels(STATION_STROKE_WIDTH_DP);
        stationPaint.setStrokeWidth(stationStrokeWidthPx);
        int stationLabelFontSizeSp = GeneralPreferences.getStationLabelFontSizeSp();
        float stationLabelFontSizePixels = ScreenUnits.spToPixels(context, stationLabelFontSizeSp);
        stationPaint.setTextSize(stationLabelFontSizePixels);

        iconPaint.setColorFilter(new PorterDuffColorFilter(stationColour, PorterDuff.Mode.SRC_IN));

        SketchPaints.setUpHighlightPaint(context, highlightPaint);

        // active legs/splays
        int legColour = ContextCompat.getColor(context, R.color.leg);
        int latestLegColour = ContextCompat.getColor(context, R.color.legLatest);
        int splayColour = ContextCompat.getColor(context, R.color.splay);
        float legStrokeWidth = dpToPixels(GeneralPreferences.getLegStrokeWidthDp());
        legPaint.setStrokeWidth(legStrokeWidth);
        legPaint.setColor(legColour);

        latestLegPaint.setStrokeWidth(legStrokeWidth);
        latestLegPaint.setColor(latestLegColour);

        float splayStrokeWidth = dpToPixels(GeneralPreferences.getSplayStrokeWidthDp());
        splayPaint.setStrokeWidth(splayStrokeWidth);
        splayPaint.setColor(splayColour);

        // faded legs/splays
        fadedLegPaint.setStrokeWidth(legStrokeWidth);
        fadedLegPaint.setColor(legColour);
        fadedLegPaint.setAlpha(FADED_ALPHA);

        fadedLatestLegPaint.setStrokeWidth(legStrokeWidth);
        fadedLatestLegPaint.setColor(latestLegColour);
        fadedLatestLegPaint.setAlpha(FADED_ALPHA);

        fadedSplayPaint.setStrokeWidth(splayStrokeWidth);
        fadedSplayPaint.setColor(splayColour);
        fadedSplayPaint.setAlpha(FADED_ALPHA);

        stationCrossDiameterPx = dpToPixels(GeneralPreferences.getStationCrossDiameterDp());
        stationLabelOffsetPx = dpToPixels(STATION_LABEL_OFFSET_DP);

        float crossDiameter = Math.round(stationCrossDiameterPx);
        float textSize = stationPaint.getTextSize();
        offScreenReach = crossDiameter + stationLabelOffsetPx + textSize;
        // the cross, a name up to the allowance (a text size per character is a generous width),
        // and two icons, with spacing between each
        offScreenReachLeft =
                crossDiameter * 2
                        + OFF_SCREEN_LABEL_ALLOWANCE_CHARACTERS * textSize
                        + 2 * crossDiameter * 1.5f;

        commentIcon =
                BitmapFactory.decodeResource(context.getResources(), R.drawable.speech_bubble);
        linkIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.link);
    }

    /**
     * Draws the legs, dashing those that isInPlane says point into or out of the page. Which leg is
     * the latest, and which are attached to the active station, is judged against the survey being
     * shown, so a linked survey's legs are never highlighted.
     */
    void drawLegs(Canvas canvas, Space<Coord2D> space, int baseAlpha, Predicate<Leg> isInPlane) {

        boolean highlightLatestLeg = GeneralPreferences.isHighlightLatestLegModeOn();

        boolean showSplays = SketchPreferences.Toggle.SHOW_SPLAYS.isOn();
        boolean fadingNonActive = SketchPreferences.Toggle.FADE_NON_ACTIVE.isOn();

        Survey survey = state.getSurvey();
        List<Leg> activeLegs =
                fadingNonActive
                        ? survey.getActiveStation().getOnwardLegs()
                        : Collections.emptyList();
        Leg latestLeg = highlightLatestLeg ? survey.getMostRecentLeg() : null;

        float viewOffsetX = viewport.getOffsetX();
        float viewOffsetY = viewport.getOffsetY();
        float viewScale = viewport.getScale();

        for (Map.Entry<Leg, Line<Coord2D>> entry : space.getLegMap().entrySet()) {
            Leg leg = entry.getKey();

            if (!showSplays && !leg.hasDestination()) {
                continue;
            }

            Line<Coord2D> line = entry.getValue();

            // inlined viewport.toView, to save allocating two Coord2Ds for every leg
            Coord2D lineStart = line.getStart(), lineEnd = line.getEnd();
            float startX = (lineStart.x - viewOffsetX) * viewScale;
            float startY = (lineStart.y - viewOffsetY) * viewScale;
            float endX = (lineEnd.x - viewOffsetX) * viewScale;
            float endY = (lineEnd.y - viewOffsetY) * viewScale;

            // a straight line can only cross the view if the rectangle around it overlaps it
            if (!viewport.isRectOnCanvas(
                    Math.min(startX, endX),
                    Math.min(startY, endY),
                    Math.max(startX, endX),
                    Math.max(startY, endY))) {
                continue;
            }

            boolean fade =
                    baseAlpha == FADED_ALPHA || (fadingNonActive && !activeLegs.contains(leg));

            Paint paint;
            if (leg == latestLeg) {
                paint = fade ? fadedLatestLegPaint : latestLegPaint;
            } else if (!leg.hasDestination()) {
                paint = fade ? fadedSplayPaint : splayPaint;
            } else {
                paint = fade ? fadedLegPaint : legPaint;
            }

            if (isInPlane.test(leg)) {
                canvas.drawLine(startX, startY, endX, endY, paint);
            } else {
                CanvasShapes.drawDashedLine(
                        canvas, new Coord2D(startX, startY), new Coord2D(endX, endY), paint);
            }
        }
    }

    /**
     * Draws the stations, each followed by the decoration given (if any) at the same alpha. With
     * Fade Non-Active on, only the active station is drawn solid. Stations too far off screen for
     * any of their cross, name or icons to show aren't drawn, but the decoration is still called
     * for them, as it can reach further than this knows.
     */
    void drawStations(
            Canvas canvas,
            Survey survey,
            Space<Coord2D> space,
            int baseAlpha,
            StationDecoration decoration) {

        boolean fadingNonActive = SketchPreferences.Toggle.FADE_NON_ACTIVE.isOn();
        boolean showStationLabels = SketchPreferences.Toggle.SHOW_STATION_LABELS.isOn();

        if (fadingNonActive) {
            baseAlpha = FADED_ALPHA;
        }

        Station activeStation = survey.getActiveStation();

        for (Map.Entry<Station, Coord2D> entry : space.getStationMap().entrySet()) {
            Station station = entry.getKey();

            int alpha = (fadingNonActive && station == activeStation) ? SOLID_ALPHA : baseAlpha;

            Coord2D translatedStation = viewport.toView(entry.getValue());

            int x = (int) (translatedStation.x);
            int y = (int) (translatedStation.y);

            if (couldStationBeVisible(x, y)) {
                drawStation(canvas, survey, station, x, y, alpha, showStationLabels);
            }

            if (decoration != null) {
                decoration.draw(canvas, station, x, y, alpha);
            }
        }
    }

    private static String getLabel(Survey survey, Station station) {
        String name = station.getName();
        if (station == survey.getOrigin()) {
            name = name + " (" + survey.getName() + ")";
        }
        return name;
    }

    /** Whether any part of a station drawn at this point on the view could be on screen. */
    private boolean couldStationBeVisible(int x, int y) {
        return viewport.isRectOnCanvas(
                x - offScreenReach, y - offScreenReach, x + offScreenReachLeft, y + offScreenReach);
    }

    private void drawStation(
            Canvas canvas,
            Survey survey,
            Station station,
            int x,
            int y,
            int alpha,
            boolean showLabel) {

        int stationCrossDiameterIntPx = Math.round(stationCrossDiameterPx);
        drawStationCross(canvas, x, y, alpha);

        if (station == survey.getActiveStation()) {
            highlightActiveStation(canvas, x, y);
        }

        int spacing = stationCrossDiameterIntPx / 2;
        int nextX = x + stationCrossDiameterIntPx;

        if (showLabel) {
            String label = getLabel(survey, station);
            canvas.drawText(label, nextX, y + stationLabelOffsetPx, stationPaint);
            nextX += Math.round(stationPaint.measureText(label)) + spacing;
        }

        List<Bitmap> icons = new ArrayList<>();
        if (station.hasComment()) {
            icons.add(commentIcon);
        }
        if (survey.hasLinkedSurveys(station)) {
            icons.add(linkIcon);
        }

        for (Bitmap icon : icons) {
            int yTop = y - stationCrossDiameterIntPx / 2;
            Rect rect =
                    new Rect(
                            nextX,
                            yTop,
                            nextX + stationCrossDiameterIntPx,
                            yTop + stationCrossDiameterIntPx);
            canvas.drawBitmap(icon, null, rect, iconPaint);
            nextX += stationCrossDiameterIntPx + spacing;
        }
    }

    /** Draws a station's cross, centred on a point on the view. */
    void drawStationCross(Canvas canvas, float x, float y, int alpha) {
        stationPaint.setAlpha(alpha);
        float halfCross = Math.round(stationCrossDiameterPx) / 2f;
        canvas.drawLine(x, y - halfCross, x, y + halfCross, stationPaint);
        canvas.drawLine(x - halfCross, y, x + halfCross, y, stationPaint);
    }

    /** Draws text in the style of a station's name, starting at a point on the view. */
    void drawStationLabel(Canvas canvas, String text, float x, float y, int alpha) {
        stationPaint.setAlpha(alpha);
        canvas.drawText(text, x, y, stationPaint);
    }

    private void highlightActiveStation(Canvas canvas, float x, float y) {
        CanvasShapes.drawHighlightBrackets(
                canvas, x, y, stationCrossDiameterPx * 1.1f, highlightPaint);
    }
}
