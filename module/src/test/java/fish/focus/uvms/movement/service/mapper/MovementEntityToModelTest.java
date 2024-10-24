package fish.focus.uvms.movement.service.mapper;

import fish.focus.schema.movement.v1.MovementBaseType;
import fish.focus.schema.movement.v1.MovementSegment;
import fish.focus.schema.movement.v1.MovementType;
import fish.focus.uvms.movement.service.MovementHelpers;
import fish.focus.uvms.movement.service.TransactionalTests;
import fish.focus.uvms.movement.service.bean.IncomingMovementBean;
import fish.focus.uvms.movement.service.bean.MovementService;
import fish.focus.uvms.movement.service.dao.MovementDao;
import fish.focus.uvms.movement.service.entity.Movement;
import fish.focus.uvms.movement.service.entity.Track;
import org.hamcrest.CoreMatchers;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

@RunWith(Arquillian.class)
public class MovementEntityToModelTest extends TransactionalTests {

    @EJB
    private MovementService movementService;

    @EJB
    private MovementDao movementDao;

    @EJB
    private IncomingMovementBean incomingMovementBean;

    @Test
    @OperateOnDeployment("movementservice")
    public void testMovementBaseType() {
        MovementHelpers movementHelpers = new MovementHelpers(movementService);
        UUID connectId = UUID.randomUUID();
        Instant dateStartMovement = Instant.now();
        Instant lesTime = dateStartMovement;
        double lon = 11.641982;
        double lat = 57.632304;
        Movement movement = movementHelpers.createMovement(lon, lat, connectId, "ONE", dateStartMovement);

        MovementBaseType output = MovementEntityToModelMapper.mapToMovementBaseType(movement);

        assertEquals(0.0, output.getReportedSpeed(), 0D);
        assertEquals(0.0, output.getReportedCourse(), 0D);
        assertEquals(movement.getId().toString(), output.getGuid());
        assertEquals(lat, output.getPosition().getLatitude(), 0D);
        assertEquals(lon, output.getPosition().getLongitude(), 0D);
        assertEquals(connectId.toString(), output.getConnectId());
        assertEquals(lesTime.truncatedTo(ChronoUnit.MILLIS),
                output.getLesReportTime().toInstant().truncatedTo(ChronoUnit.MILLIS));
        try {
            output = MovementEntityToModelMapper.mapToMovementBaseType(null);
            fail("null input should result in a nullpointer");
        } catch (NullPointerException e) {
            assertTrue(true);
        }
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void testMapToMovementTypeWithMovementInput() {
        MovementHelpers movementHelpers = new MovementHelpers(movementService);
        UUID connectId = UUID.randomUUID();
        Instant dateStartMovement = Instant.now();
        double lon = 11.641982;
        double lat = 57.632304;
        Movement movement = movementHelpers.createMovement(lon, lat, connectId, "ONE", dateStartMovement);

        MovementType output = MovementEntityToModelMapper.mapToMovementType(movement);

        assertEquals(0.0, output.getReportedSpeed(), 0D);
        assertEquals(0.0, output.getReportedCourse(), 0D);
        assertEquals(movement.getId().toString(), output.getGuid());
        assertEquals(lat, output.getPosition().getLatitude(), 0D);
        assertEquals(lon, output.getPosition().getLongitude(), 0D);
        assertEquals(connectId.toString(), output.getConnectId());
        assertEquals("POINT ( 11.641982 57.632304 )", output.getWkt());
        assertTrue(!output.isDuplicate());

        movement = null;
        output = MovementEntityToModelMapper.mapToMovementType(movement);
        assertNull(output);
    }


    @Test
    @OperateOnDeployment("movementservice")
    public void testMapToMovementTypeWithAListOfMovements() {
        //Most of the method is tested by testMapToMovementType
        MovementHelpers movementHelpers = new MovementHelpers(movementService);
        UUID connectId = UUID.randomUUID();
        Instant dateStartMovement = Instant.now();

        List<Movement> input = movementHelpers.createFishingTourVarberg(1, connectId);
        em.flush();
        List<MovementType> output = MovementEntityToModelMapper.mapToMovementType(input);

        assertEquals(input.size(), output.size());

        input = null;
        try {
            output = MovementEntityToModelMapper.mapToMovementType(input);
            fail("Null as input");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void testMapToMovementSegment() {
        MovementHelpers movementHelpers = new MovementHelpers(movementService);
        UUID connectId = UUID.randomUUID();
        Instant dateStartMovement = Instant.now();
        List<Movement> movementList = movementHelpers.createFishingTourVarberg(1, connectId);
        em.flush();

        List<MovementSegment> output = MovementEntityToModelMapper.mapToMovementSegment(movementList, false);
        assertThat(output.size(), CoreMatchers.is(movementList.size() - 1));

        try {
            MovementEntityToModelMapper.mapToMovementSegment(null, false);
            fail("Null as input");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void testMapToMovementSegmentIncludeFirstAndLast() {
        MovementHelpers movementHelpers = new MovementHelpers(movementService);
        UUID connectId = UUID.randomUUID();
        List<Movement> movementList = movementHelpers.createFishingTourVarberg(1, connectId);
        em.flush();

        List<Movement> movementSublist = movementList.subList(1, movementList.size());
        List<MovementSegment> output = MovementEntityToModelMapper.mapToMovementSegment(movementSublist, false);
        assertThat(output.size(), CoreMatchers.is(movementSublist.size() - 1));
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void testMapToMovementSegmentExcludeFirstAndLast() {
        MovementHelpers movementHelpers = new MovementHelpers(movementService);
        UUID connectId = UUID.randomUUID();
        List<Movement> movementList = movementHelpers.createFishingTourVarberg(1, connectId);
        em.flush();

        List<Movement> movementSublist = movementList.subList(1, movementList.size());
        List<MovementSegment> output = MovementEntityToModelMapper.mapToMovementSegment(movementSublist, true);
        assertThat(output.size(), CoreMatchers.is(movementSublist.size() - 1));
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void testOrderMovementsByConnectId() {
        MovementHelpers movementHelpers = new MovementHelpers(movementService);
        List<UUID> connectId = new ArrayList<>();
        List<Movement> input = new ArrayList<>();
        UUID ID;
        for (int i = 0; i < 20; i++) {
            ID = UUID.randomUUID();
            connectId.add(ID);
            input.add(movementHelpers.createMovement(Math.random() * 90, Math.random() * 90, ID, "ONE", Instant.now().plusMillis((long) (Math.random() * 5000))));
        }

        Map<UUID, List<Movement>> output = MovementEntityToModelMapper.orderMovementsByConnectId(input);

        assertEquals(connectId.size(), output.keySet().size());
        for (UUID s : connectId) {
            assertTrue(output.containsKey(s));
        }

        try {
            output = MovementEntityToModelMapper.orderMovementsByConnectId(null);
            fail("Null as input");
        } catch (NullPointerException e) {
            assertTrue(true);
        }
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void testExtractTracks() {
        MovementHelpers movementHelpers = new MovementHelpers(movementService);
        UUID connectId = UUID.randomUUID();
        ArrayList<Movement> movementList = new ArrayList<>(movementHelpers.createFishingTourVarberg(1, connectId));

        List<Track> output = MovementEntityToModelMapper.extractTracks(movementList);

        assertEquals(1, output.size());
        assertEquals(movementList.get(0).getTrack().getDuration(), output.get(0).getDuration(), 0D);
        assertEquals(movementList.get(0).getTrack().getId(), output.get(0).getId());

        try {
            output = MovementEntityToModelMapper.extractTracks(null);
            fail("Null as invalue");
        } catch (NullPointerException e) {
            assertTrue(true);
        }
    }
}
