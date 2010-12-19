/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.hydrologis.geopaparazzi.compass;

import java.text.DecimalFormat;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.drawable.BitmapDrawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.chart.ChartDrawer;
import eu.hydrologis.geopaparazzi.gps.GpsLocation;
import eu.hydrologis.geopaparazzi.util.ApplicationManager;
import eu.hydrologis.geopaparazzi.util.ApplicationManagerListener;
import eu.hydrologis.geopaparazzi.util.Constants;

/**
 * The view taking care of drawing and updating the compass. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class CompassView extends View implements ApplicationManagerListener {
    /*
     * static variables, needed to be initialized only once for all
     */
    private static Paint mPaint = new Paint();
    private static Path mPath = null;
    private static Picture picture = null;
    private static DecimalFormat formatter = new DecimalFormat("0.00000"); //$NON-NLS-1$
    private static String validPointsString;
    private static String distanceString;
    private static int compassWidth;
    private static int compassHeight;
    private static int compassCX;
    private static int compassCY;
    private static float textSizeNormal;
    private static Bitmap compassBitmap;
    private static String timeString;
    private static String lonString;
    private static String latString;
    private static String altimString;
    private static String azimString;
    private static ChartDrawer chartDrawer;

    /**
     * The current azimuth angle, with 0 = North, -90 = West, 90 = East
     */
    private float normalAzimuth = -1f;
    private float pictureAzimuth = -1f;
    private GpsLocation loc;

    private int horizontalAxisColor = Color.DKGRAY;
    private int horizontalAxisAlpha = 0;
    private int horizontalLabelsColor = Color.BLACK;
    private int horizontalLabelsAlpha = 255;
    private int chartColor = Color.RED;
    private int chartAlpha = 255;
    private int chartPointColor = Color.RED;
    private int chartPointAlpha = 255;
    private int backgroundColor = Color.LTGRAY;
    private int backgroundAlpha = 100;

    private ApplicationManager applicationManager;
    private final TextView compassInfoView;
    private int satellitesNum = 0;
    private static String satellitesString;
    private int maxSatellites = 0;

    public CompassView( Context context, TextView compassInfoView, ApplicationManager applicationManager, GpsLocation startLoc ) {
        super(context);
        this.compassInfoView = compassInfoView;
        this.applicationManager = applicationManager;
        this.loc = startLoc;

        /*
         * the following block initializes a bunch of variables that 
         * can be kept static, since they are unique to the 
         * lifetime of the application.
         */
        if (mPath == null) {
            mPath = new Path();
            int[] needle = getResources().getIntArray(R.array.compassneedle_coords);
            for( int i = 0; i < needle.length; i = i + 2 ) {
                if (i == 0) {
                    mPath.moveTo(needle[i], needle[i + 1]);
                } else {
                    mPath.lineTo(needle[i], needle[i + 1]);
                }
            }
            mPath.close();

            timeString = getResources().getString(R.string.utctime);
            lonString = getResources().getString(R.string.lon);
            latString = getResources().getString(R.string.lat);
            altimString = getResources().getString(R.string.altim);
            azimString = getResources().getString(R.string.azimuth);
            validPointsString = getResources().getString(R.string.log_points);
            distanceString = getResources().getString(R.string.log_distance);
            satellitesString = getResources().getString(R.string.satellite_num);

            chartDrawer = new ChartDrawer("", ChartDrawer.LINE); //$NON-NLS-1$

            String textSizeMediumStr = getResources().getString(R.string.text_normal);
            textSizeNormal = Float.parseFloat(textSizeMediumStr);

            BitmapDrawable compassDrawable = (BitmapDrawable) getResources().getDrawable(R.drawable.compass);
            compassWidth = compassDrawable.getIntrinsicWidth();
            compassHeight = compassDrawable.getIntrinsicHeight();
            compassCX = compassWidth / 2;
            if (compassCX % 2 != 0) {
                compassCX--;
            }
            compassCY = compassHeight / 2;

            compassDrawable.setBounds(0, 0, compassWidth, compassHeight);
            compassBitmap = compassDrawable.getBitmap();
        }

    }

    protected void onDraw( Canvas canvas ) {

        // if (azimuth != -1) {
        Paint paint = mPaint;
        paint.setAntiAlias(true);

        if (picture == null) {
            picture = new Picture();
            Canvas recCanvas = picture.beginRecording(compassWidth, compassHeight);
            recCanvas.drawColor(Color.WHITE);
            recCanvas.drawBitmap(compassBitmap, 0, 0, paint);
            paint.setColor(Constants.COMPASS_NEEDLE_COLOR);
            paint.setAlpha(Constants.COMPASS_NEEDLE_ALPHA + 50);
            recCanvas.drawCircle(compassCX, compassCY, 4, paint);

            picture.endRecording();
        }
        picture.draw(canvas);
        paint.setColor(Constants.COMPASS_TEXT_COLOR);
        paint.setAlpha(255);
        paint.setTextSize(textSizeNormal);

        StringBuilder sb = new StringBuilder();

        boolean hasValue = true;
        if (loc == null) {
            // Log.d("COMPASSVIEW", "Location from gps is null!");
            hasValue = false;
            sb.append(getContext().getString(R.string.nogps_data));
            sb.append("\n");
        } else {
            sb.append(timeString);
            sb.append(" ").append(loc.getTimeString()); //$NON-NLS-1$
            sb.append("\n");
            sb.append(latString);
            sb.append(" ").append(formatter.format(loc.getLatitude())); //$NON-NLS-1$
            sb.append("\n");
            sb.append(lonString);
            sb.append(" ").append(formatter.format(loc.getLongitude())); //$NON-NLS-1$
            sb.append("\n");
            sb.append(altimString);
            sb.append(" ").append((int) loc.getAltitude()); //$NON-NLS-1$
            sb.append("\n");
            sb.append(azimString);
            sb.append(" ").append((int) (360 - normalAzimuth)); //$NON-NLS-1$
            sb.append("\n");
        }

        // TODO
        // if (satellitesNum == 0) {
        // sb.append("\n");
        // }
        // sb.append(satellitesString);
        //        sb.append(" ").append(satellitesNum); //$NON-NLS-1$
        //        sb.append("/").append(maxSatellites); //$NON-NLS-1$
        // sb.append("\n");

        compassInfoView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSizeNormal);
        if (hasValue) {
            compassInfoView.setTextColor(getResources().getColor(R.color.black));
        } else {
            // paint.setTextSize(textSizeLarge);
            compassInfoView.setTextColor(getResources().getColor(R.color.red));
        }

        if (applicationManager.isGpsLogging()) {
            sb.append(validPointsString);
            sb.append(" ").append(applicationManager.getCurrentRunningGpsLogPointsNum());
            sb.append("\n");
            sb.append(distanceString);
            sb.append(" ").append(applicationManager.getCurrentRunningGpsLogDistance());
            sb.append("\n");
        }
        compassInfoView.setText(sb.toString());

        drawChart(canvas);

        // draw needle
        if (normalAzimuth != -1) {
            canvas.translate(compassCX, compassCY);
            paint.setColor(Color.RED);
            paint.setAlpha(150);
            paint.setStyle(Paint.Style.FILL);
            canvas.rotate(-(float) normalAzimuth);
            canvas.drawPath(mPath, paint);
        }
    }

    private void drawChart( Canvas canvas ) {
        if (applicationManager.isGpsLogging()) {

            List<Float> last100Elevations = applicationManager.getLast100Elevations();
            int last100size = last100Elevations.size();
            // Log.v("COMPASSVIEW", "Size of last 100 list: " + last100size);
            float[] values = new float[last100size];
            float min = Float.POSITIVE_INFINITY;
            float max = Float.NEGATIVE_INFINITY;
            for( int i = 0; i < last100size; i++ ) {
                values[i] = last100Elevations.get(i);
                if (values[i] > max) {
                    max = values[i];
                }
                if (values[i] < min) {
                    min = values[i];
                }
            }
            if (values.length == 0) {
                values = new float[10];
                min = 0;
                max = 1000;
                for( int i = 0; i < values.length; i++ ) {
                    if (i < 5) {
                        values[i] = i * 100;
                    } else {
                        values[i] = i * 50;
                    }
                }
            }

            float border = 4;
            int canvasWidth = canvas.getWidth();
            chartDrawer.setProperties(horizontalAxisColor, horizontalAxisAlpha, horizontalLabelsColor, horizontalLabelsAlpha,
                    chartColor, chartAlpha, chartPointColor, chartPointAlpha, backgroundColor, backgroundAlpha);
            float y = border;
            float w = canvasWidth - 2 * border;
            float h = compassHeight - 3 * border;
            chartDrawer.drawCart(canvas, border, y, w, h, max, min, new String[]{"", ""}, //$NON-NLS-1$//$NON-NLS-2$
                    new String[]{"", ""}, null, values, 2); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    public void onLocationChanged( GpsLocation loc ) {
        if (loc != null) {
            this.loc = loc;
        }
        invalidate();
    }

    public void onSensorChanged( double normalAzimuth, double pictureAzimuth ) {
        this.normalAzimuth = (float) normalAzimuth;
        invalidate();
    }

    public void onSatellitesStatusChanged( int num, int max ) {
        this.satellitesNum = num;
        this.maxSatellites = max;
    }

}