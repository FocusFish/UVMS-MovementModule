package fish.focus.uvms.movement.rest;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import fish.focus.schema.movement.asset.v1.VesselType;
import fish.focus.schema.movement.search.v1.ListPagination;
import fish.focus.schema.movement.search.v1.MovementQuery;
import fish.focus.schema.movement.search.v1.MovementSearchGroup;
import fish.focus.schema.movement.v1.*;
import fish.focus.uvms.movement.service.entity.Movement;
import fish.focus.uvms.movement.service.entity.MovementConnect;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;

public class MovementTestHelper {

    public static Movement createMovement() {
        return MovementTestHelper.createMovement(56d,11d);
    }

    public static Movement createMovement(Double longitude , Double latitude) {

        Movement movement = new Movement();
        movement.setMovementType(MovementTypeType.POS);
        MovementConnect movementConnect = new MovementConnect();
        movementConnect.setId(UUID.randomUUID());
        movementConnect.setUpdated(Instant.now());
        movementConnect.setUpdatedBy("MovementTestHelper");
        movement.setMovementConnect(movementConnect);

        Coordinate coordinate = new Coordinate(longitude, latitude);
        GeometryFactory factory = new GeometryFactory();
        Point point = factory.createPoint(coordinate);
        point.setSRID(4326);
        movement.setLocation(point);

        movement.setHeading(0f);
        movement.setSpeed(0f);
        movement.setSource(MovementSourceType.NAF);
        movement.setStatus("TEST");
        movement.setTimestamp(Instant.now());

        movement.setUpdatedBy("Test");
        movement.setUpdated(Instant.now());

        return movement;
    }
    
    public static MovementSearchGroup createBasicMovementSearchGroup() {
        MovementSearchGroup movementSearchGroup = new MovementSearchGroup();
        movementSearchGroup.setName("Test Group " + getRandomIntegers(5));
        movementSearchGroup.setUser("TEST");
        return movementSearchGroup;
    }

    public static MovementQuery createMovementQuery() {
        MovementQuery movementQuery = new MovementQuery();
        movementQuery.setExcludeFirstAndLastSegment(true);

        ListPagination listPagination = new ListPagination();
        listPagination.setPage(BigInteger.ONE);
        listPagination.setListSize(BigInteger.valueOf(1000));
        movementQuery.setPagination(listPagination);
        
        return movementQuery;
    }
    
    public static TempMovementType createTempMovementType() {
        TempMovementType tempMovement = new TempMovementType();
        VesselType asset = new VesselType();
        asset.setCfr("CFR" + getRandomIntegers(7));
        asset.setExtMarking("EXT" + getRandomIntegers(2));
        asset.setIrcs("IRCS" + getRandomIntegers(5));
        asset.setFlagState("SWE");
        asset.setName("Ship" + getRandomIntegers(8));
        tempMovement.setAsset(asset);
        MovementPoint movementPoint = new MovementPoint();
        movementPoint.setLongitude(1d);
        movementPoint.setLatitude(2d);
        tempMovement.setPosition(movementPoint);
        tempMovement.setCourse(45d);
        tempMovement.setState(TempMovementStateEnum.DRAFT);
        return tempMovement;
    }
    
    public static String getRandomIntegers(int length) {
        return new Random()
                .ints(0,9)
                .mapToObj(i -> String.valueOf(i))
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }
}
