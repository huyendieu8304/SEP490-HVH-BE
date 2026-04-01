package com.sep490.g28.hvh.be.util;


import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

public class GeoUtils {

    private static final int SRID = 4326;

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), SRID);

    /**
     * Create PostGIS Point from lat/lng
     * NOTE: coordinate order in PostGIS = (lng, lat)
     */
    public static Point toPoint(Double lat, Double lng) {
        if (lat == null || lng == null) {
            return null;
        }

        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
        point.setSRID(SRID);
        return point;
    }

    public static Double getLat(Point point) {
        if (point == null) return null;
        return point.getY();
    }

    public static Double getLng(Point point) {
        if (point == null) return null;
        return point.getX();
    }

    public static boolean isValidLatLng(Double lat, Double lng) {
        if (lat == null || lng == null) return false;

        return lat >= -90 && lat <= 90 &&
                lng >= -180 && lng <= 180;
    }

    /**
     * distance between 2 points in meters
     */
    public static double distanceMeters(Point p1, Point p2) {
        if (p1 == null || p2 == null) return 0;

        double earthRadius = 6371000; // meters

        double lat1 = Math.toRadians(getLat(p1));
        double lat2 = Math.toRadians(getLat(p2));
        double deltaLat = lat2 - lat1;
        double deltaLng = Math.toRadians(getLng(p2) - getLng(p1));

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c;
    }
}
