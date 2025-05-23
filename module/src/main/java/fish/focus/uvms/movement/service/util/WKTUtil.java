/*
﻿Developed with the contribution of the European Commission - Directorate General for Maritime Affairs and Fisheries
© European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can
redistribute it and/or modify it under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or any later version. The IFDM Suite is distributed in
the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. You should have received a
copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */
package fish.focus.uvms.movement.service.util;

import fish.focus.uvms.movement.service.entity.Movement;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;

import java.util.ArrayList;
import java.util.List;

public class WKTUtil {

    private WKTUtil() {
    }

    public static String getWktLineString(List<Geometry> geometries) {
        List<Coordinate> coords = new ArrayList<>();
        for (Geometry geom : geometries) {
            for (Coordinate verti : geom.getCoordinates()) {
                coords.add(verti);
            }
        }
        CoordinateSequence seq = new CoordinateArraySequence(coords.toArray(new Coordinate[0]));
        return WKTWriter.toLineString(seq);
    }

    public static String getWktLineStringFromMovements(Movement previous, Movement current) {
        return WKTWriter.toLineString(GeometryUtil.getLineStringFromMovements(previous, current).getCoordinateSequence());
    }

    public static String getWktLineStringFromMovementList(List<Movement> movements) {
        List<Coordinate> coords = new ArrayList<>();
        for (Movement movement : movements) {
            coords.add(movement.getLocation().getCoordinate());
        }
        CoordinateSequence seq = new CoordinateArraySequence(coords.toArray(new Coordinate[0]));
        return WKTWriter.toLineString(seq);
    }

    public static Geometry getGeometryFromWKTSrring(String wkt) {
        try {
            WKTReader reader = new WKTReader();
            Geometry geom = reader.read(wkt);
            geom.setSRID(GeometryUtil.SRID);
            return geom;
        } catch (ParseException e) {
            throw new IllegalArgumentException("Inputstring " + wkt + " causes a parse exception.", e);
        }
    }

    public static String getWktPointString(Geometry geometry) {
        return WKTWriter.toPoint(geometry.getCoordinate());
    }

    public static String getWktPointFromMovement(Movement movement) {
        return getWktPointString(movement.getLocation());
    }

}