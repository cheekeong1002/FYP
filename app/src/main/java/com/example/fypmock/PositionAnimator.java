package com.example.fypmock;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Build;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import es.situm.sdk.model.location.Coordinate;
import es.situm.sdk.model.location.Location;

public class PositionAnimator {

    private static final String TAG = PositionAnimator.class.getSimpleName();
    private static final int DURATION_POSITION_ANIMATION = 500;
    private static final int DURATION_BEARING_ANIMATION = 200;
    private static final int DURATION_RADIUS_ANIMATION = 500;

    private static final double DISTANCE_CHANGE_TO_ANIMATE = 0.2;
    private static final int BEARING_CHANGE_TO_ANIMATE = 1;
    private static final double GROUND_OVERLAY_DIAMETER_CHANGE_TO_ANIMATE = 0.2;

    @Nullable
    private Location lastLocation;
    private LatLng lastLatLng;
    private LatLng destinationLatLng;

    private float lastBearing;
    private float destinationBearing;
    private float lastGroundOverlayDimensions;
    private float destinationGroundOverlayDimensions;

    private ValueAnimator locationAnimator = new ValueAnimator();
    private ValueAnimator locationBearingAnimator = new ValueAnimator();
    private ValueAnimator groundOverlayAnimator = new ValueAnimator();

    //only 1 thread can run this method at a time (synchronized method)
    synchronized void animate(final Marker marker, final GroundOverlay groundOverlay, final Location location) {
        Coordinate toCoordinate = location.getCoordinate();
        final LatLng toLatLng = new LatLng(toCoordinate.getLatitude(), toCoordinate.getLongitude());
        final float toBearing = (float) location.getBearing().degrees();
        final float groundOverlayDiameter = location.getAccuracy() * 2;

        //enter if this is first location obtained
        if (lastLocation == null) {
            marker.setRotation(toBearing);
            marker.setPosition(toLatLng);

            groundOverlay.setPosition(toLatLng);
            groundOverlay.setDimensions(groundOverlayDiameter);

            lastLocation = location;
            lastLatLng = toLatLng;
            lastBearing = toBearing;
            lastGroundOverlayDimensions = groundOverlayDiameter;
            return;
        }

        animatePosition(marker, groundOverlay, location);
        animateBearing(marker, location);
        animateGroundOverlay(groundOverlay, groundOverlayDiameter);
    }

    private void animatePosition(final Marker marker, final GroundOverlay groundOverlay, Location toLocation){
        Coordinate toCoordinate = toLocation.getCoordinate();
        final LatLng toLatLng = new LatLng(toCoordinate.getLatitude(), toCoordinate.getLongitude());

        if ( destinationLatLng != null) {
            float[] results = new float[1];
            android.location.Location.distanceBetween(toCoordinate.getLatitude(), toCoordinate.getLongitude(),
                    destinationLatLng.latitude, destinationLatLng.longitude, results);
            float distance = results[0];

            //skip animate position if change in distance is less than 0.2m
            if (distance < DISTANCE_CHANGE_TO_ANIMATE) {
                return;
            }
        }

        //skip animate position if there is no change in position
        if (destinationLatLng == toLatLng) {
            return;
        }

        //stop animation in its tracks
        locationAnimator.cancel();

        //skip animate if there is no change in location of user or current location is first location
        if (lastLocation != toLocation) {
            locationAnimator = new ObjectAnimator();

            //set listener to receive callbacks on every animation frame
            locationAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                LatLng startLatLng = lastLatLng;

                @Override
                public synchronized void onAnimationUpdate(ValueAnimator animation) {
                    //get value that is used for animating
                    float t = animation.getAnimatedFraction();
                    //calculate interpolation between the start and end location based on "t"
                    lastLatLng = interpolateLatLng(t, startLatLng, toLatLng);

                    marker.setPosition(lastLatLng);
                    groundOverlay.setPosition(lastLatLng);
                }
            });
            locationAnimator.setFloatValues(0, 1); //set of values that animation will animate between over time
            locationAnimator.setDuration(DURATION_POSITION_ANIMATION);
            locationAnimator.start();
            destinationLatLng = toLatLng;
        }
    }

    private LatLng interpolateLatLng(float fraction, LatLng a, LatLng b) {
        double lat = (b.latitude - a.latitude) * fraction + a.latitude;
        double lng = (b.longitude - a.longitude) * fraction + a.longitude;
        return new LatLng(lat, lng);
    }

    private float normalizeAngle(float degrees) {
        degrees = degrees % 360;
        return (degrees + 360) % 360;
    }

    private void animateBearing(final Marker marker, Location location) {
        float degrees = (float) location.getBearing().degrees();

        //Normalize angle so that angle is always positive
        degrees = normalizeAngle(degrees);
        final float toBearing = degrees;

        //skip animate bearing if there is no change in bearing
        if (destinationBearing == toBearing) {
            return;
        }

        locationBearingAnimator.cancel();

        lastBearing =  normalizeAngle(lastBearing);

        //always rotate in shortest way
        if (lastBearing - toBearing > 180) {
            lastBearing -= 360;
        } else if (toBearing - lastBearing > 180) {
            lastBearing += 360;
        }

        float diffBearing = Math.abs(toBearing - lastBearing);
        //skip animate if change in bearing is less 1 degree
        if (diffBearing < BEARING_CHANGE_TO_ANIMATE) {
            return;
        }

        //skip animate if there is no change in bearing degree
        if (lastBearing != toBearing) {

            locationBearingAnimator = new ObjectAnimator();
            locationBearingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public synchronized void onAnimationUpdate(ValueAnimator animation) {
                    //get most recent value calculated by ValueAnimator
                    lastBearing = (Float) animation.getAnimatedValue();
                    marker.setRotation(lastBearing);
                }
            });
            //set of values that animation will animate between over time
            locationBearingAnimator.setFloatValues(lastBearing, toBearing);
            locationBearingAnimator.setDuration(DURATION_BEARING_ANIMATION);
            locationBearingAnimator.start();
            destinationBearing = toBearing;
        }
    }

    private synchronized void animateGroundOverlay(final GroundOverlay groundOverlay, float toGroundOverlayDiameter) {
        //skip animate if there is no change in diameter or current location is first location
        if (destinationGroundOverlayDimensions == toGroundOverlayDiameter) {
            return;
        }

        groundOverlayAnimator.cancel();

        float diffDimensions = Math.abs(toGroundOverlayDiameter - lastGroundOverlayDimensions);
        //skip animate if change in diameter is less than 0.2m
        if (diffDimensions < GROUND_OVERLAY_DIAMETER_CHANGE_TO_ANIMATE) {
            return;
        }
        //skip animate if there is no change in diameter
        if (lastGroundOverlayDimensions != toGroundOverlayDiameter) {

            groundOverlayAnimator = new ObjectAnimator();
            groundOverlayAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                @Override
                public synchronized void onAnimationUpdate(ValueAnimator animation) {
                    //get most recent value calculated by ValueAnimator
                    lastGroundOverlayDimensions = (Float) animation.getAnimatedValue();
                    groundOverlay.setDimensions(lastGroundOverlayDimensions);
                }
            });
            //set of values that animation will animate between over time
            groundOverlayAnimator.setFloatValues(lastGroundOverlayDimensions, toGroundOverlayDiameter);
            groundOverlayAnimator.setDuration(DURATION_RADIUS_ANIMATION);
            groundOverlayAnimator.start();
            destinationGroundOverlayDimensions = toGroundOverlayDiameter;
        }
    }

    synchronized void clear() {
        lastLocation = null;
    }
}
