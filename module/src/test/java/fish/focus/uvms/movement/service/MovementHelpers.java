package fish.focus.uvms.movement.service;

import com.peertopark.java.geocalc.Coordinate;
import com.peertopark.java.geocalc.DegreeCoordinate;
import com.peertopark.java.geocalc.EarthCalc;
import com.peertopark.java.geocalc.Point;
import fish.focus.uvms.movement.service.bean.MovementService;
import fish.focus.uvms.movement.service.entity.Movement;

import java.time.Instant;
import java.util.*;

public class MovementHelpers {

    private final MovementService movementService;

    private Random rnd = new Random();

    public MovementHelpers(MovementService movementBatchModelBean) {
        this.movementService = movementBatchModelBean;
    }

    public static String getRandomIntegers(int length) {
        return new Random()
                .ints(0, 9)
                .mapToObj(i -> String.valueOf(i))
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }

    /******************************************************************************************************************
     *  helpers
     *****************************************************************************************************************/

    public Movement createMovement(double longitude, double latitude, UUID connectId, String userName,
                                   Instant positionTime) {

        Movement movement = MockData.createMovement(longitude, latitude, connectId, 0, userName);
        movement.setTimestamp(positionTime);
        movement.setLesReportTime(positionTime);
        movement = movementService.createMovement(movement);
        return movement;


    }

    // create l coordinates for well known routes. Collections.shuffle(route);

    private Movement createMovement(LatLong latlong, UUID connectId, String userName, Instant positionTime) {

        Movement movement = MockData.createMovement(latlong, connectId, userName);
        movement.setTimestamp(positionTime);
        return movementService.createAndProcessMovement(movement);

    }

    /**
     * @param order           1 = as created  first EXIT_PORT then GAP  all in time_order
     *                        2 = reversed
     *                        3 = randomly ordered
     * @param numberPositions
     * @param connectId
     * @return
     */
    public List<Movement> createVarbergGrenaMovements(int order, int numberPositions, UUID connectId) {
        List<LatLong> positions = createRuttVarbergGrena(numberPositions);
        return getMovements(order, connectId, positions);
    }

    public List<Movement> createFishingTourVarberg(int order, UUID connectId) {
        List<LatLong> positions = createRuttSmallFishingTourFromVarberg();
        return getMovements(order, connectId, positions);
    }

    private List<Movement> getMovements(int order, UUID connectId, List<LatLong> positions) {
        List<Movement> createdRoute = new ArrayList<>();
        String userName = "TEST";

        Instant timeStamp = Instant.now();
        int loopCount = 0;

        long timeDelta = 300000;

        switch (order) {
            case 2:
                Collections.reverse(positions);
                timeDelta = -300000;
                break;
            case 3:
                Collections.shuffle(positions);
                break;
        }

        for (LatLong position : positions) {
            loopCount++;
            Movement movement = createMovement(position, connectId, userName + "_" + String.valueOf(loopCount), timeStamp);

            timeStamp = timeStamp.plusMillis(timeDelta);
            createdRoute.add(movement);
        }
        return createdRoute;
    }

    private List<LatLong> calculateReportedDataForRoute(List<LatLong> route) {

        LatLong previousPosition = null;
        LatLong currentPosition = null;
        int i = 0;
        int n = route.size();
        while (i < n) {
            currentPosition = route.get(i);
            if (i == 0) {
                previousPosition = route.get(i);
                i++;
                continue;
            }

            double bearing = bearing(previousPosition, currentPosition);
            double distance = distance(previousPosition, currentPosition);
            route.get(i - 1).bearing = bearing;
            route.get(i - 1).distance = distance;
            route.get(i - 1).speed = calcSpeed(previousPosition, currentPosition);

            if (i < n) {
                previousPosition = currentPosition;
            }
            i++;
        }
        double bearing = bearing(previousPosition, currentPosition);
        route.get(i - 1).distance = distance(previousPosition, currentPosition);
        route.get(i - 1).bearing = bearing;
        //double speed = calcSpeed(previousPosition, currentPosition);
        route.get(i - 1).speed = 0d;
        return route;
    }

    private List<LatLong> createRuttVarbergGrena(int numberPositions) {

        int movementTimeDeltaInMillis = 30000;
        List<LatLong> rutt = new ArrayList<>();
        Instant ts = Instant.now();

        double latitude = 57.110;
        double longitude = 12.244;

        double END_LATITUDE = 56.408;
        double END_LONGITUDE = 10.926;

        while (true) {
            if (latitude >= END_LATITUDE)
                latitude = latitude - 0.003;
            if (longitude >= END_LONGITUDE)
                longitude = longitude - 0.003;
            if (latitude < END_LATITUDE && longitude < END_LONGITUDE)
                break;
            rutt.add(new LatLong(latitude, longitude, ts.plusMillis(movementTimeDeltaInMillis)));
        }

        // now when we have a route we must calculate heading and speed
        rutt = calculateReportedDataForRoute(rutt);

        if (numberPositions == -1) {
            return rutt;
        } else {
            return rutt.subList(0, numberPositions);
        }
    }

    private List<LatLong> createRuttSmallFishingTourFromVarberg() {

        int movementTimeDeltaInMillis = 30000;
        List<LatLong> rutt = new ArrayList<>();
        Instant ts = Instant.now();

        double randomFactorLat = rnd.nextDouble();
        double randomFactorLong = rnd.nextDouble();

        double latitude = 57.110 + randomFactorLat;
        double longitude = 12.244 + randomFactorLong;

        // these will never be reached but still good to have to steer on
        double END_LATITUDE = 56.408;
        double END_LONGITUDE = 10.926;

        // leave the harbour
        for (int i = 0; i < 25; i++) {

            if (latitude >= END_LATITUDE)
                latitude = latitude - 0.004;
            if (longitude >= END_LONGITUDE)
                longitude = longitude - 0.004;
            if (latitude < END_LATITUDE && longitude < END_LONGITUDE)
                break;
            rutt.add(new LatLong(latitude, longitude, ts.plusMillis(movementTimeDeltaInMillis)));
        }
        // do some fishing
        for (int i = 0; i < 15; i++) {
            latitude = latitude - 0.001;
            longitude = longitude - 0.002;
            rutt.add(new LatLong(latitude, longitude, ts.plusMillis(movementTimeDeltaInMillis)));
        }
        // go home
        int n = rutt.size();
        List<LatLong> ruttHome = new ArrayList<>();
        for (int i = n - 1; i > 0; i--) {
            LatLong wrk = rutt.get(i);
            ruttHome.add(new LatLong(wrk.latitude + 0.001, wrk.longitude, ts.plusMillis(movementTimeDeltaInMillis)));
        }

        rutt.addAll(ruttHome);
        rutt = calculateReportedDataForRoute(rutt);
        return rutt;
    }

    private Double bearing(LatLong src, LatLong dst) {

        Coordinate latFrom = new DegreeCoordinate(src.latitude);
        Coordinate lngFrom = new DegreeCoordinate(src.longitude);
        Point from = new Point(latFrom, lngFrom);

        Coordinate latTo = new DegreeCoordinate(dst.latitude);
        Coordinate lngTo = new DegreeCoordinate(dst.longitude);
        Point to = new Point(latTo, lngTo);

        return EarthCalc.getBearing(from, to);
    }

    private Double distance(LatLong src, LatLong dst) {

        Coordinate latFrom = new DegreeCoordinate(src.latitude);
        Coordinate lngFrom = new DegreeCoordinate(src.longitude);
        Point from = new Point(latFrom, lngFrom);

        Coordinate latTo = new DegreeCoordinate(dst.latitude);
        Coordinate lngTo = new DegreeCoordinate(dst.longitude);
        Point to = new Point(latTo, lngTo);

        return EarthCalc.getDistance(from, to);
    }

    private double calcSpeed(LatLong src, LatLong dst) {

        try {
            if (src.positionTime == null)
                return 0;
            if (dst.positionTime == null)
                return 0;

            // distance to next
            double distanceM = src.distance;

            double durationms = (double) Math.abs(dst.positionTime.toEpochMilli() - src.positionTime.toEpochMilli());
            double durationSecs = durationms / 1000;
            double speedMeterPerSecond = (distanceM / durationSecs);
            double speedMPerHour = speedMeterPerSecond * 3600;
            return speedMPerHour / 1000;
        } catch (RuntimeException e) {
            return 0.0;
        }
    }
}
