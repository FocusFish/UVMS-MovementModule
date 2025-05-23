package fish.focus.uvms.movement.service.bean;

import fish.focus.schema.movement.search.v1.*;
import fish.focus.schema.movement.source.v1.GetMovementMapByQueryResponse;
import fish.focus.schema.movement.v1.MovementSegment;
import fish.focus.schema.movement.v1.MovementTrack;
import fish.focus.schema.movement.v1.SegmentCategoryType;
import fish.focus.uvms.commons.date.DateUtils;
import fish.focus.uvms.movement.model.GetMovementListByQueryResponse;
import fish.focus.uvms.movement.service.MovementHelpers;
import fish.focus.uvms.movement.service.TransactionalTests;
import fish.focus.uvms.movement.service.entity.Movement;
import fish.focus.uvms.movement.service.mapper.search.SearchField;
import fish.focus.uvms.movement.service.mapper.search.SearchValue;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import javax.ejb.EJBTransactionRolledbackException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;

/**
 * Transferred from domain layer. Should be integrated with MovementServiceIntTest.
 */

@RunWith(Arquillian.class)
public class MovementDomainModelBeanIntTest extends TransactionalTests {

    private final Random rnd = new Random();

    @EJB
    private MovementMapResponseHelper mapResponseHelper;

    @EJB
    private MovementService movementService;

    @Test
    @OperateOnDeployment("movementservice")
    public void filterSegments() {
        List<SearchValue> searchKeyValuesRange = new ArrayList<>();
        searchKeyValuesRange.add(createSearchValueDurationHelper());
        searchKeyValuesRange.add(createSearchValueLengthHelper());
        searchKeyValuesRange.add(createSearchValueSpeedHelper());

        // just try to satisfy all paths in the filter
        List<MovementSegment> movementSegments = new ArrayList<>();
        movementSegments.add(createMovementSegmentDurationHelper(9, 900, 90));
        movementSegments.add(createMovementSegmentDurationHelper(15, 1500, 150));
        movementSegments.add(createMovementSegmentDurationHelper(21, 2100, 210));

        List<MovementSegment> segments = mapResponseHelper.filterSegments(movementSegments, searchKeyValuesRange);
        assertEquals(1, segments.size());
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void getLatestMovements_0() {
        List<Movement> movements = movementService.getLatestMovements(0);
        assertNotNull(movements);
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void getLatestMovements_5() {
        List<Movement> movements = movementService.getLatestMovements(5);
        assertNotNull(movements);
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void getLatestMovements_5000000() {
        List<Movement> movements = movementService.getLatestMovements(5000000);
        assertNotNull(movements);
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void getLatestMovements_NULL() {
        assertThrows(EJBTransactionRolledbackException.class, () -> movementService.getLatestMovements(null));
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void getLatestMovements_neg5() {
        assertThrows(EJBTransactionRolledbackException.class, () -> movementService.getLatestMovements(-5));
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void getMovementListByQuery_NULL() {
        String expectedMessage = "Movement list query is null";

        Exception exception = assertThrows(EJBTransactionRolledbackException.class, () -> movementService.getList(null));
        assertThat(exception.getMessage(), containsString(expectedMessage));
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void getMovementMapByQuery_NULL() {
        String expectedMessage = "Movement list query is null";

        Exception exception = assertThrows(EJBTransactionRolledbackException.class, () -> movementService.getMapByQuery(null));
        assertThat(exception.getMessage(), containsString(expectedMessage));
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void keepSegment_NULL() {
        String expectedMessage = "MovementSegment or SearchValue list is null";

        Exception exception = assertThrows(EJBTransactionRolledbackException.class, () -> mapResponseHelper.keepSegment(null, null));
        assertThat(exception.getMessage(), containsString(expectedMessage));
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void removeTrackMismatches_NULL() {
        String expectedMessage = "MovementTrack list or Movement list is null";

        Exception exception = assertThrows(EJBTransactionRolledbackException.class, () -> mapResponseHelper.removeTrackMismatches(null, null));
        assertThat(exception.getMessage(), containsString(expectedMessage));
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void testGetMovementListByQuery_WillFailWithNullParameter() {
        String expectedMessage = "Movement list query is null";

        Exception exception = assertThrows(EJBTransactionRolledbackException.class, () -> movementService.getList(null));
        assertThat(exception.getMessage(), containsString(expectedMessage));
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void testGetMovementListByQuery_WillFailNoPaginationSet() {
        String expectedMessage = "Pagination in movementlist query is null";

        MovementQuery input = new MovementQuery();
        input.setExcludeFirstAndLastSegment(true);
        Exception exception = assertThrows(EJBTransactionRolledbackException.class, () -> movementService.getList(input));
        assertThat(exception.getMessage(), containsString(expectedMessage));
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void testGetMovementListByQuery_WillFailNoSearchCriteria() {
        String expectedMessage = "No search criterias in MovementList query";

        MovementQuery input = new MovementQuery();
        input.setExcludeFirstAndLastSegment(true);

        ListPagination listPagination = new ListPagination();
        listPagination.setListSize(new BigInteger("100"));
        listPagination.setPage(new BigInteger("1")); //this can not be 0 or lower....
        input.setPagination(listPagination);

        Exception exception = assertThrows(EJBTransactionRolledbackException.class, () -> movementService.getList(input));
        assertThat(exception.getMessage(), containsString(expectedMessage));
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void testGetMovementListByQuery() {
        GetMovementListByQueryResponse output;
        MovementQuery input = new MovementQuery();
        input.setExcludeFirstAndLastSegment(true);

        ListPagination listPagination = new ListPagination();
        listPagination.setListSize(new BigInteger("100"));
        listPagination.setPage(new BigInteger("1")); //this can not be 0 or lower....
        input.setPagination(listPagination);

        UUID connectID = UUID.randomUUID();
        UUID connectID2 = UUID.randomUUID();
        createAndProcess10MovementsFromVarbergGrena(connectID);
        createAndProcess10MovementsFromVarbergGrena(connectID2);

        ListCriteria listCriteria = new ListCriteria();
        listCriteria.setKey(SearchKey.CONNECT_ID);
        listCriteria.setValue(connectID.toString());
        input.getMovementSearchCriteria().add(listCriteria);

        output = movementService.getList(input);
        assertEquals(10, output.getMovement().size());

        listCriteria = new ListCriteria();
        listCriteria.setKey(SearchKey.CONNECT_ID);
        listCriteria.setValue(connectID2.toString());
        input.getMovementSearchCriteria().add(listCriteria);
        output = movementService.getList(input);
        assertEquals(20, output.getMovement().size());

        input.getMovementSearchCriteria().remove(listCriteria);

        RangeCriteria rangeCriteria = new RangeCriteria();
        rangeCriteria.setKey(RangeKeyType.DATE);
        rangeCriteria.setFrom(DateUtils.dateToEpochMilliseconds(Instant.now()));
        rangeCriteria.setTo(DateUtils.dateToEpochMilliseconds(Instant.now().plusSeconds(1800))); //1 800 000 is the time for 6 of the movements in the list  aka 1 800
        input.getMovementRangeSearchCriteria().add(rangeCriteria);

        output = movementService.getList(input);
        assertEquals(6, output.getMovement().size());
    	
    	/* Apparently speed vary to much to be a valid test
    	rangeCriteria = new RangeCriteria();
    	rangeCriteria.setKey(RangeKeyType.MOVEMENT_SPEED);
    	rangeCriteria.setFrom("45.552000");
    	rangeCriteria.setTo("45.554000");
    	input.getMovementRangeSearchCriteria().add(rangeCriteria);
    	
    	output = movementService.getList(input);
    	assertEquals(2, output.getMovementList().size(),1);
    	*/
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void testGetMovementListByQuery_WillFailEmptyRangeSearchCriteria() {
        MovementQuery input = new MovementQuery();
        input.setExcludeFirstAndLastSegment(true);

        ListPagination listPagination = new ListPagination();
        listPagination.setListSize(new BigInteger("100"));
        listPagination.setPage(new BigInteger("1")); //this can not be 0 or lower....
        input.setPagination(listPagination);

        UUID connectID = UUID.randomUUID();
        UUID connectID2 = UUID.randomUUID();
        createAndProcess10MovementsFromVarbergGrena(connectID);
        createAndProcess10MovementsFromVarbergGrena(connectID2);

        ListCriteria listCriteria = new ListCriteria();
        listCriteria.setKey(SearchKey.CONNECT_ID);
        listCriteria.setValue(connectID.toString());
        input.getMovementSearchCriteria().add(listCriteria);

        input.getMovementRangeSearchCriteria().add(new RangeCriteria());

        assertThrows(EJBTransactionRolledbackException.class, () -> movementService.getList(input));
    }


    @Test
    @OperateOnDeployment("movementservice")
    public void testGetMovementMapByQuery_WillFailWIthNullAsQuery() {
        String expectedMessage = "Movement list query is null";

        Exception exception = assertThrows(EJBTransactionRolledbackException.class, () -> movementService.getMapByQuery(null));
        assertThat(exception.getMessage(), containsString(expectedMessage));
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void testGetMovementMapByQuery_WillFailWIthPaginationNotSupported() {
        String expectedMessage = "Pagination not supported in get movement map by query";

        MovementQuery input = new MovementQuery();
        input.setExcludeFirstAndLastSegment(true);

        ListPagination listPagination = new ListPagination();
        listPagination.setListSize(new BigInteger("100"));
        listPagination.setPage(new BigInteger("1")); //this can not be 0 or lower....
        input.setPagination(listPagination);

        Exception exception = assertThrows(EJBTransactionRolledbackException.class, () -> movementService.getMapByQuery(input));
        assertThat(exception.getMessage(), containsString(expectedMessage));
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void testGetMovementMapByQuery() {
        GetMovementMapByQueryResponse output;

        MovementQuery input = new MovementQuery();
        input.setExcludeFirstAndLastSegment(true);

        UUID connectID = UUID.randomUUID();
        UUID connectID2 = UUID.randomUUID();
        createAndProcess10MovementsFromVarbergGrena(connectID);
        createAndProcess10MovementsFromVarbergGrena(connectID2);

        ListCriteria listCriteria = new ListCriteria();
        listCriteria.setKey(SearchKey.CONNECT_ID);
        listCriteria.setValue(connectID.toString());
        input.getMovementSearchCriteria().add(listCriteria);

        output = movementService.getMapByQuery(input);
        assertEquals(1, output.getMovementMap().size());
        assertEquals(10, output.getMovementMap().get(0).getMovements().size());
        assertEquals(9, output.getMovementMap().get(0).getSegments().size());
        assertEquals(1, output.getMovementMap().get(0).getTracks().size());

        listCriteria = new ListCriteria();
        listCriteria.setKey(SearchKey.CONNECT_ID);
        listCriteria.setValue(connectID2.toString());
        input.getMovementSearchCriteria().add(listCriteria);
        output = movementService.getMapByQuery(input);
        assertEquals(2, output.getMovementMap().size());
        assertEquals(10, output.getMovementMap().get(1).getMovements().size());
        assertEquals(9, output.getMovementMap().get(1).getSegments().size());
        assertEquals(1, output.getMovementMap().get(1).getTracks().size());

        input.getMovementSearchCriteria().remove(listCriteria);

        RangeCriteria rangeCriteria = new RangeCriteria();
        rangeCriteria.setKey(RangeKeyType.DATE);
        rangeCriteria.setFrom(DateUtils.dateToEpochMilliseconds(Instant.now()));
        rangeCriteria.setTo(DateUtils.dateToEpochMilliseconds(Instant.now().plusSeconds(1800))); //1 800 000 is the time for 6 of the movements in the list  aka 1 800
        input.getMovementRangeSearchCriteria().add(rangeCriteria);

        output = movementService.getMapByQuery(input);
        assertEquals(6, output.getMovementMap().get(0).getMovements().size());
        assertEquals(5, output.getMovementMap().get(0).getSegments().size());
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void testGetMinimalMovementMapByQuery_WillFailNoSearchCriteriaSet() {
        MovementQuery input = new MovementQuery();
        input.setExcludeFirstAndLastSegment(true);

        ListPagination listPagination = new ListPagination();
        listPagination.setListSize(new BigInteger("100"));
        listPagination.setPage(new BigInteger("1")); //this can not be 0 or lower....
        input.setPagination(listPagination);
        input.getMovementRangeSearchCriteria().add(new RangeCriteria());

        assertThrows(EJBTransactionRolledbackException.class, () -> movementService.getMapByQuery(input));
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void testRemoveTrackMismatches() {
        UUID connectID = UUID.randomUUID();
        List<Movement> varbergGrena = createAndProcess10MovementsFromVarbergGrena(connectID);
        em.flush();
        List<MovementTrack> input = new ArrayList<>();
        MovementTrack movementTrack = new MovementTrack();
        movementTrack.setId("" + varbergGrena.get(0).getTrack().getId());
        input.add(movementTrack);

        movementTrack = new MovementTrack();
        movementTrack.setId("42");
        input.add(movementTrack);

        movementTrack = new MovementTrack();
        movementTrack.setId("99999999");
        input.add(movementTrack);

        mapResponseHelper.removeTrackMismatches(input, varbergGrena);

        assertEquals(1, input.size());

        try {
            mapResponseHelper.removeTrackMismatches(null, varbergGrena);
            fail("null as input");
        } catch (RuntimeException e) {
            assertTrue(true);
        }
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void testGetLatestMovementsByConnectID() {
        UUID connectID = UUID.randomUUID();
        UUID connectID2 = UUID.randomUUID();
        List<Movement> control = createAndProcess10MovementsFromVarbergGrena(connectID);
        createAndProcess10MovementsFromVarbergGrena(connectID2);

        List<UUID> input = new ArrayList<>();
        input.add(connectID);

        List<Movement> output = movementService.getLatestMovementsByConnectIds(input);

        assertEquals(1, output.size());
        assertEquals(control.get(9).getId(), output.get(0).getId());

        input.add(connectID2);
        output = movementService.getLatestMovementsByConnectIds(input);
        assertEquals(2, output.size());

        output = movementService.getLatestMovementsByConnectIds(null);
        assertTrue(output.isEmpty());
    }

    /******************************************************************************************************************
     *   HELPER FUNCTIONS
     ******************************************************************************************************************/

    private MovementSegment createMovementSegmentDurationHelper(double durationValue, double distanceValue, double speedValue) {
        MovementSegment movementSegment = new MovementSegment();
        movementSegment.setId(String.valueOf(rnd.nextInt(1000)));
        movementSegment.setTrackId("TrackId_42");
        movementSegment.setCategory(SegmentCategoryType.OTHER);
        movementSegment.setDistance(distanceValue);
        movementSegment.setDuration(durationValue);
        movementSegment.setSpeedOverGround(speedValue);
        movementSegment.setCourseOverGround(0.6);
        movementSegment.setWkt("wkt");
        return movementSegment;
    }

    private SearchValue createSearchValueDurationHelper() {
        return new SearchValue(SearchField.SEGMENT_DURATION, "10", "20");
    }

    private SearchValue createSearchValueLengthHelper() {
        return new SearchValue(SearchField.SEGMENT_LENGTH, "1000", "2000");
    }

    private SearchValue createSearchValueSpeedHelper() {
        return new SearchValue(SearchField.SEGMENT_SPEED, "100", "200");
    }

    private List<Movement> createAndProcess10MovementsFromVarbergGrena(UUID connectID) {
        MovementHelpers movementHelpers = new MovementHelpers(movementService);
        List<Movement> varbergGrena = movementHelpers.createVarbergGrenaMovements(1, 10, connectID);
        return varbergGrena;
    }
}
