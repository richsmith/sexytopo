package org.hwyl.sexytopo.control.sketch;

import static org.hwyl.sexytopo.control.sketch.SketchPaints.FADED_ALPHA;
import static org.hwyl.sexytopo.control.sketch.SketchPaints.SOLID_ALPHA;
import static org.hwyl.sexytopo.control.sketch.SketchPaints.dpToPixels;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
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
                latestLegPaint,
                splayPaint,
                fadedLegPaint,
                fadedLatestLegPaint,
                fadedSplayPaint
            };

    private float stationCrossDiameterPx;
    private float stationLabelOffsetPx;

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
        float stationLabelFontSizePixels = SketchPaints.spToPixels(context, stationLabelFontSizeSp);
        stationPaint.setTextSize(stationLabelFontSizePixels);

        iconPaint.setColorFilter(new PorterDuffColorFilter(stationColour, PorterDuff.Mode.SRC_IN));

        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setStrokeWidth(stationStrokeWidthPx * 1.25f);
        int activeStationHighlightColor =
                ContextCompat.getColor(context, R.color.activeStationHighlight);
        highlightPaint.setColor(activeStationHighlightColor);

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

        commentIcon =
                BitmapFactory.decodeResource(context.getResources(), R.drawable.speech_bubble);
        linkIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.link);
    }

    /**
     * Draws the legs, dashing those that isInPlane says point into or out of the page. Which leg is
     * the latest, and which are attached to the active station, is judged against the survey being
     * shown, even when drawing a linked one.
     */
    void drawLegs(Canvas canvas, Space<Coord2D> space, int baseAlpha, Predicate<Leg> isInPlane) {

        boolean highlightLatestLeg = GeneralPreferences.isHighlightLatestLegModeOn();

        boolean showSplays = SketchPreferences.Toggle.SHOW_SPLAYS.isOn();
        boolean fadingNonActive = SketchPreferences.Toggle.FADE_NON_ACTIVE.isOn();

        Map<Leg, Line<Coord2D>> legMap = space.getLegMap();

        for (Leg leg : legMap.keySet()) {

            if (!showSplays && !leg.hasDestination()) {
                continue;
            }

            Line<Coord2D> line = legMap.get(leg);

            //noinspection ConstantConditions
            Coord2D start = viewport.toView(line.getStart());
            Coord2D end = viewport.toView(line.getEnd());

            if (!viewport.isLineOnCanvas(start, end)) {
                continue;
            }

            boolean fade =
                    baseAlpha == FADED_ALPHA || (fadingNonActive && !isAttachedToActive(leg));

            Paint paint;
            if (highlightLatestLeg && state.getSurvey().getMostRecentLeg() == leg) {
                paint = fade ? fadedLatestLegPaint : latestLegPaint;
            } else if (!leg.hasDestination()) {
                paint = fade ? fadedSplayPaint : splayPaint;
            } else {
                paint = fade ? fadedLegPaint : legPaint;
            }

            if (isInPlane.test(leg)) {
                canvas.drawLine(start.x, start.y, end.x, end.y, paint);
            } else {
                CanvasShapes.drawDashedLine(canvas, start, end, paint);
            }
        }
    }

    private boolean isAttachedToActive(Leg leg) {
        return state.getSurvey().getActiveStation().getOnwardLegs().contains(leg);
    }

    /**
     * Draws the stations, each followed by the decoration given (if any) at the same alpha. With
     * Fade Non-Active on, only the active station is drawn solid.
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

        int alpha = baseAlpha;
        stationPaint.setAlpha(alpha);

        for (Map.Entry<Station, Coord2D> entry : space.getStationMap().entrySet()) {
            Station station = entry.getKey();

            if (fadingNonActive && (station == survey.getActiveStation())) {
                alpha = SOLID_ALPHA;
                // setting alpha is measured as a relatively expensive call, so we change this as
                // little as possible
                stationPaint.setAlpha(alpha);
            }

            Coord2D translatedStation = viewport.toView(entry.getValue());

            int x = (int) (translatedStation.x);
            int y = (int) (translatedStation.y);

            int stationCrossDiameterIntPx = Math.round(stationCrossDiameterPx);
            drawStationCross(canvas, x, y, alpha);

            if (station == survey.getActiveStation()) {
                highlightActiveStation(canvas, x, y);
            }

            int spacing = stationCrossDiameterIntPx / 2;
            int nextX = x + stationCrossDiameterIntPx;

            if (showStationLabels) {
                String name = station.getName();
                if (station == survey.getOrigin()) {
                    name = name + " (" + survey.getName() + ")";
                }
                canvas.drawText(name, nextX, y + stationLabelOffsetPx, stationPaint);
                nextX += Math.round(stationPaint.measureText(name)) + spacing;
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

            if (decoration != null) {
                decoration.draw(canvas, station, x, y, alpha);
            }

            if (fadingNonActive && (station == survey.getActiveStation())) {
                alpha = baseAlpha;
                stationPaint.setAlpha(alpha);
            }
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

        float diameter = stationCrossDiameterPx * 1.1f;
        float gap = diameter / 3f;
        float topY = y - (diameter / 2);
        float bottomY = y + (diameter / 2);
        float leftX = x - (diameter / 2);
        float rightX = x + (diameter / 2);

        float innerLeft = leftX + ((diameter - gap) / 2);
        float innerRight = innerLeft + gap;
        float innerTop = topY + ((diameter - gap) / 2);
        float innerBottom = innerTop + gap;

        // top-left corner
        Path topLeft = new Path();
        topLeft.moveTo(innerLeft, topY);
        topLeft.lineTo(leftX, topY);
        topLeft.lineTo(leftX, innerTop);
        canvas.drawPath(topLeft, highlightPaint);

        // top-right corner
        Path topRight = new Path();
        topRight.moveTo(innerRight, topY);
        topRight.lineTo(rightX, topY);
        topRight.lineTo(rightX, innerTop);
        canvas.drawPath(topRight, highlightPaint);

        // bottom-left corner
        Path bottomLeft = new Path();
        bottomLeft.moveTo(leftX, innerBottom);
        bottomLeft.lineTo(leftX, bottomY);
        bottomLeft.lineTo(innerLeft, bottomY);
        canvas.drawPath(bottomLeft, highlightPaint);

        // bottom-right corner
        Path bottomRight = new Path();
        bottomRight.moveTo(rightX, innerBottom);
        bottomRight.lineTo(rightX, bottomY);
        bottomRight.lineTo(innerRight, bottomY);
        canvas.drawPath(bottomRight, highlightPaint);
    }
}
