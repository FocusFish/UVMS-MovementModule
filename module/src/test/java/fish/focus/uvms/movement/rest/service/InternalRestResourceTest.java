package fish.focus.uvms.movement.rest.service;

import fish.focus.schema.movement.search.v1.ListCriteria;
import fish.focus.schema.movement.search.v1.ListPagination;
import fish.focus.schema.movement.search.v1.MovementQuery;
import fish.focus.schema.movement.search.v1.SearchKey;
import fish.focus.schema.movement.v1.MovementType;
import fish.focus.uvms.commons.date.DateUtils;
import fish.focus.uvms.movement.rest.BuildMovementRestDeployment;
import fish.focus.uvms.movement.rest.MovementTestHelper;
import fish.focus.uvms.movement.model.GetMovementListByQueryResponse;
import fish.focus.uvms.movement.model.GetMovementMapByQueryResponse;
import fish.focus.uvms.movement.model.dto.MovementDto;
import fish.focus.uvms.movement.model.dto.MovementsForConnectIdsBetweenDatesRequest;
import fish.focus.uvms.movement.service.bean.MovementService;
import fish.focus.uvms.movement.service.dao.MovementDao;
import fish.focus.uvms.movement.service.entity.Movement;
import fish.focus.uvms.movement.service.util.JsonBConfiguratorMovement;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.Assert.*;

@RunWith(Arquillian.class)
public class InternalRestResourceTest extends BuildMovementRestDeployment {

    @Inject
    private MovementService movementService;

    @Inject
    private MovementDao movementDao;

    private Jsonb jsonb;

    @Before
    public void init(){
        jsonb = new JsonBConfiguratorMovement().getContext(null);
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void getMovementListByQuery() {
        Movement movementBaseType = MovementTestHelper.createMovement();
        Movement createdMovement = movementService.createAndProcessMovement(movementBaseType);

        assertNotNull(createdMovement.getId());

        MovementQuery query = createMovementQuery(null);

        String response = getWebTarget()
                .path("internal/list")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getTokenInternalRest())
                .post(Entity.json(query), String.class);
        assertNotNull(response);

        GetMovementListByQueryResponse movList =
                jsonb.fromJson(response, GetMovementListByQueryResponse.class);
        assertNotNull(movList);
        assertTrue(movList.getMovement().size() > 0);
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void getLatestMovementsByConnectIds() {
        Movement movementBaseType = MovementTestHelper.createMovement();
        Movement createdMovement = movementService.createAndProcessMovement(movementBaseType);

        assertNotNull(createdMovement.getId());

        UUID movConnectId = createdMovement.getMovementConnect().getId();

        String response = getWebTarget()
                .path("internal/latest")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getTokenInternalRest())
                .post(Entity.json(Collections.singletonList(movConnectId)), String.class);
        assertNotNull(response);

        List<MovementType> movements = Arrays.asList(jsonb.fromJson(response, MovementType[].class));

        assertNotNull(movements);
        assertEquals(1, movements.size());
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void countMovementsInTheLastDayForAssetTest() {
        Movement movementBaseType = MovementTestHelper.createMovement();
        Movement createdMovement = movementService.createAndProcessMovement(movementBaseType);
        Instant in60Seconds = Instant.now().plusSeconds(60);

        long response = getWebTarget()
                .path("internal/countMovementsInDateAndTheDayBeforeForAsset/")
                .path(createdMovement.getMovementConnect().getId().toString())
                .queryParam("after", DateUtils.dateToEpochMilliseconds(in60Seconds)) //yyyy-MM-dd HH:mm:ss Z
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getTokenInternalRest())
                .get(long.class);

        assertEquals(1, response);
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void createAndGetMovementById() {
        Movement movementBaseType = MovementTestHelper.createMovement();
        Movement createdMovement = movementService.createAndProcessMovement(movementBaseType);

        MovementDto response = getWebTarget()
                .path("internal/getMovement/")
                .path(createdMovement.getId().toString())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getTokenInternalRest())
                .get(MovementDto.class);

        assertNotNull(response);
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void getMovementMapByQuery() throws IOException {
        Movement movementBaseType = MovementTestHelper.createMovement();
        Movement createdMovement = movementService.createAndProcessMovement(movementBaseType);

        MovementQuery query = createMovementQuery(createdMovement);

        String response = getWebTarget()
                .path("internal/movementMapByQuery")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getTokenInternalRest())
                .post(Entity.json(query), String.class);
        assertNotNull(response);

        GetMovementMapByQueryResponse movMap = jsonb.fromJson(response, GetMovementMapByQueryResponse.class);
        assertNotNull(movMap);
        assertEquals(1, movMap.getMovementMap().size());
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void remapMovementConnectInMovementTest() {
        Movement movementBaseType1 = MovementTestHelper.createMovement();
        Movement movementBaseType2 = MovementTestHelper.createMovement();
        movementBaseType2.setMovementConnect(movementBaseType1.getMovementConnect());
        Movement movementBaseType3 = MovementTestHelper.createMovement();
        Movement movementBaseType4 = MovementTestHelper.createMovement();
        movementBaseType4.setMovementConnect(movementBaseType3.getMovementConnect());
        Movement createdMovement1 = movementService.createAndProcessMovement(movementBaseType1);
        Movement createdMovement2 = movementService.createAndProcessMovement(movementBaseType2);
        Movement createdMovement3 = movementService.createAndProcessMovement(movementBaseType3);
        Movement createdMovement4 = movementService.createAndProcessMovement(movementBaseType4);

        Response remapResponse = getWebTarget()
                .path("internal/remapMovementConnectInMovement")
                .queryParam("MovementConnectFrom", createdMovement1.getMovementConnect().getId().toString())
                .queryParam("MovementConnectTo", createdMovement3.getMovementConnect().getId().toString())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getTokenInternalRest())
                .put(Entity.json(""), Response.class);
        assertEquals(200, remapResponse.getStatus());
        int nrOfChanges = remapResponse.readEntity(Integer.class);
        assertEquals(2, nrOfChanges);

        MovementQuery movementQuery = createMovementQuery(null);
        movementQuery.getMovementSearchCriteria().clear();
        ListCriteria criteria = new ListCriteria();
        criteria.setKey(SearchKey.CONNECT_ID);
        criteria.setValue(createdMovement3.getMovementConnect().getId().toString());
        movementQuery.getMovementSearchCriteria().add(criteria);

        String response = getWebTarget()
                .path("internal/list")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getTokenInternalRest())
                .post(Entity.json(movementQuery), String.class);
        assertNotNull(response);

        GetMovementListByQueryResponse movList =
                jsonb.fromJson(response, GetMovementListByQueryResponse.class);
        assertEquals(4, movList.getMovement().size());
        assertTrue(movList.getMovement().stream().anyMatch(m -> m.getGuid().equals(createdMovement1.getId().toString())));
        assertTrue(movList.getMovement().stream().anyMatch(m -> m.getGuid().equals(createdMovement2.getId().toString())));
        assertTrue(movList.getMovement().stream().anyMatch(m -> m.getGuid().equals(createdMovement3.getId().toString())));
        assertTrue(movList.getMovement().stream().anyMatch(m -> m.getGuid().equals(createdMovement4.getId().toString())));
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void remapMovementConnectInMovementAndDeleteOldMovementConnectTest() {
        Movement movementBaseType1 = MovementTestHelper.createMovement();
        Movement movementBaseType2 = MovementTestHelper.createMovement();
        movementBaseType2.setMovementConnect(movementBaseType1.getMovementConnect());
        Movement movementBaseType3 = MovementTestHelper.createMovement();
        Movement movementBaseType4 = MovementTestHelper.createMovement();
        movementBaseType4.setMovementConnect(movementBaseType3.getMovementConnect());
        Movement createdMovement1 = movementService.createAndProcessMovement(movementBaseType1);
        movementService.createAndProcessMovement(movementBaseType2);
        Movement createdMovement3 = movementService.createAndProcessMovement(movementBaseType3);
        movementService.createAndProcessMovement(movementBaseType4);

        Response remapResponse = getWebTarget()
                .path("internal/remapMovementConnectInMovement")
                .queryParam("MovementConnectFrom", createdMovement1.getMovementConnect().getId().toString())
                .queryParam("MovementConnectTo", createdMovement3.getMovementConnect().getId().toString())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getTokenInternalRest())
                .put(Entity.json(""), Response.class);
        assertEquals(200, remapResponse.getStatus());
        int nrOfChanges = remapResponse.readEntity(Integer.class);
        assertEquals(2, nrOfChanges);

        Response deleteResponse = getWebTarget()
                .path("internal/removeMovementConnect")
                .queryParam("MovementConnectId", createdMovement1.getMovementConnect().getId().toString())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getTokenInternalRest())
                .delete(Response.class);
        assertEquals(200, deleteResponse.getStatus());

        assertNull(movementDao.getMovementConnectByConnectId(createdMovement1.getMovementConnect().getId()));
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void remapNonExistantAssetTest() {
        Response remapResponse = getWebTarget()
                .path("internal/remapMovementConnectInMovement")
                .queryParam("MovementConnectFrom", UUID.randomUUID().toString())
                .queryParam("MovementConnectTo", UUID.randomUUID().toString())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getTokenInternalRest())
                .put(Entity.json(""), Response.class);
        assertEquals(200, remapResponse.getStatus());
        int nrOfChanges = remapResponse.readEntity(Integer.class);
        assertEquals(0, nrOfChanges);
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void removeNonExistantMCTest() {
        Response deleteResponse = getWebTarget()
                .path("internal/removeMovementConnect")
                .queryParam("MovementConnectId", UUID.randomUUID().toString())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getTokenInternalRest())
                .delete(Response.class);
        assertEquals(200, deleteResponse.getStatus());
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void movementsForConnectIdsBetweenDates() {
        Movement movementBaseType = MovementTestHelper.createMovement();
        Movement createdMovement = movementService.createAndProcessMovement(movementBaseType);

        assertNotNull(createdMovement.getId());

        List<String> connectIds = new ArrayList<>();
        connectIds.add(movementBaseType.getMovementConnect().getId().toString());

        Instant now = Instant.now();
        Instant dayBefore = now.minus(1, ChronoUnit.DAYS);

        MovementsForConnectIdsBetweenDatesRequest request = new MovementsForConnectIdsBetweenDatesRequest(connectIds, dayBefore, now);

        Response response = getWebTarget()
                .path("internal/movementsForConnectIdsBetweenDates")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getTokenInternalRest())
                .post(Entity.entity(request, MediaType.APPLICATION_JSON_TYPE), Response.class);

        List<MovementDto> movementExtendedList = response.readEntity(new GenericType<List<MovementDto>>() {});

        assertEquals(1, movementExtendedList.size());
    }

    private MovementQuery createMovementQuery(Movement createdMovement) {
        MovementQuery query = new MovementQuery();
        if(createdMovement != null) {
            ListCriteria criteria = new ListCriteria();
            criteria.setKey(SearchKey.MOVEMENT_ID);
            criteria.setValue(createdMovement.getId().toString());
            query.getMovementSearchCriteria().add(criteria);
        } else {
            ListCriteria criteria = new ListCriteria();
            criteria.setKey(SearchKey.STATUS);
            criteria.setValue("TEST");
            query.getMovementSearchCriteria().add(criteria);

            ListPagination pagination = new ListPagination();
            pagination.setPage(BigInteger.ONE);
            pagination.setListSize(BigInteger.valueOf(10));
            query.setPagination(pagination);
        }
        return query;
    }
}
