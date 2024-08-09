package fish.focus.uvms.movement.service.util;

import fish.focus.uvms.movement.service.MovementHelpers;
import fish.focus.uvms.movement.service.TransactionalTests;
import fish.focus.uvms.movement.service.bean.IncomingMovementBean;
import fish.focus.uvms.movement.service.bean.MovementService;
import fish.focus.uvms.movement.service.dao.MovementDao;
import fish.focus.uvms.movement.service.entity.Movement;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.locationtech.jts.geom.Geometry;

import javax.ejb.EJB;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(Arquillian.class)
public class WKTUtilTest extends TransactionalTests {

    @EJB
    private MovementService movementService;

    @EJB
    private MovementDao movementDao;

    @EJB
    private IncomingMovementBean incomingMovementBean;

    @Test
    @OperateOnDeployment("movementservice")
    public void testGetWKTLineString() {
        MovementHelpers movementHelpers = new MovementHelpers(movementService);
        UUID connectId = UUID.randomUUID();
        Instant dateStartMovement = Instant.now();

        List<Movement> varbergGrena = movementHelpers.createVarbergGrenaMovements(1, 10, connectId);
        List<Geometry> input = new LinkedList<>();
        String correct = "LINESTRING (";
        for (Movement mov : varbergGrena) {
            input.add(mov.getLocation());
            correct = correct.concat(mov.getLocation().getX() + " " + mov.getLocation().getY() + ", ");
        }
        correct = correct.substring(0, correct.lastIndexOf(','));
        correct = correct.concat(")");
        String output = WKTUtil.getWktLineString(input);
        assertEquals(correct, output);

        try {
            input.get(((int) Math.random() * 10)).getCoordinate().setCoordinate(null);
            output = WKTUtil.getWktLineString(input);
            fail("A random null in the input should generate an exception");
        } catch (NullPointerException e) {
            assertTrue(true);
        }

        try {
            output = WKTUtil.getWktLineString(null);
            fail("Null as input should generate an exception");
        } catch (NullPointerException e) {
            assertTrue(true);
        }
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void testGetWktLineStringFromMovementList() {
        MovementHelpers movementHelpers = new MovementHelpers(movementService);
        UUID connectId = UUID.randomUUID();
        Instant dateStartMovement = Instant.now();

        List<Movement> input = movementHelpers.createVarbergGrenaMovements(1, 10, connectId);

        String output = WKTUtil.getWktLineStringFromMovementList(input);
        String correct = "LINESTRING (12.241 57.107, 12.238 57.104, 12.235 57.101, 12.232 57.098, 12.229 57.095, 12.225999999999999 57.092,"
                + " 12.222999999999999 57.089, 12.219999999999999 57.086, 12.216999999999999 57.083, 12.213999999999999 57.08)";
        assertEquals(correct, output);

        try {
            input.get(((int) Math.random() * 10)).setLocation(null);
            output = WKTUtil.getWktLineStringFromMovementList(input);
            fail("A random null in the input should generate an exception");
        } catch (NullPointerException e) {
            assertTrue(true);
        }

        try {
            output = WKTUtil.getWktLineStringFromMovementList(null);
            fail("Null as input should generate an exception");
        } catch (NullPointerException e) {
            assertTrue(true);
        }
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void testGetWktLineStringFromMovementGeometryList() {
        MovementHelpers movementHelpers = new MovementHelpers(movementService);
        UUID connectId = UUID.randomUUID();
        Instant dateStartMovement = Instant.now();

        List<Movement> input = movementHelpers.createVarbergGrenaMovements(1, 10, connectId);

        List<Geometry> geometries = new ArrayList<>();
        for (Movement each : input) {
            geometries.add(each.getLocation());
        }

        String output = WKTUtil.getWktLineString(geometries);
        String correct = "LINESTRING (12.241 57.107, 12.238 57.104, 12.235 57.101, 12.232 57.098, 12.229 57.095, 12.225999999999999 57.092,"
                + " 12.222999999999999 57.089, 12.219999999999999 57.086, 12.216999999999999 57.083, 12.213999999999999 57.08)";
        assertEquals(correct, output);

        try {
            input.get(((int) Math.random() * 10)).setLocation(null);
            output = WKTUtil.getWktLineStringFromMovementList(input);
            fail("A random null in the input should generate an exception");
        } catch (NullPointerException e) {
            assertTrue(true);
        }

        try {
            output = WKTUtil.getWktLineStringFromMovementList(null);
            fail("Null as input should generate an exception");
        } catch (NullPointerException e) {
            assertTrue(true);
        }
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void testGetGeometryFromWKTSrring() {
        String input = "LINESTRING (12.241 57.107, 12.238 57.104, 12.235 57.101, 12.232 57.098, 12.229 57.095, 12.225999999999999 57.092,"
                + " 12.222999999999999 57.089, 12.219999999999999 57.086, 12.216999999999999 57.083, 12.213999999999999 57.08)";
        Geometry output = WKTUtil.getGeometryFromWKTSrring(input);
        List<Geometry> geoList = new LinkedList<>();
        geoList.add(output);
        String stringOutput = WKTUtil.getWktLineString(geoList);

        assertEquals(input, stringOutput);

        try {
            input = "LINESTRING ()";
            output = WKTUtil.getGeometryFromWKTSrring(input);
            fail("zero arguments");
        } catch (Exception e) {
            assertTrue(true);
        }

        try {
            input = "LINESTRING (12.241 57.107)";
            output = WKTUtil.getGeometryFromWKTSrring(input);
            fail("Only one point should cause an exception");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            input = "LINESTRING (12.241)";
            output = WKTUtil.getGeometryFromWKTSrring(input);
            fail("Only half a point should cause an exception");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            input = "LI,NESTR,ING (12.241 57.107)";
            output = WKTUtil.getGeometryFromWKTSrring(input);
            fail("random commas should cause an exception");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            output = WKTUtil.getGeometryFromWKTSrring(null);
            fail("null as input");
        } catch (NullPointerException e) {
            assertTrue(true);
        }
    }
}
