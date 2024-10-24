package fish.focus.uvms.movement.rest.service;

import fish.focus.schema.movement.search.v1.ListPagination;
import fish.focus.uvms.movement.rest.BuildMovementRestDeployment;
import fish.focus.uvms.movement.rest.filter.AppError;
import fish.focus.uvms.movement.service.dao.AlarmDAO;
import fish.focus.uvms.movement.service.dto.*;
import fish.focus.uvms.movement.service.entity.IncomingMovement;
import fish.focus.uvms.movement.service.entity.alarm.AlarmReport;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Arquillian.class)
public class AlarmRestResourceTest extends BuildMovementRestDeployment {

    @Inject
    private AlarmDAO alarmDao;

    private static AlarmReport getBasicAlarmReport() {
        AlarmReport alarmReport = new AlarmReport();
        alarmReport.setAssetGuid(UUID.randomUUID().toString());
        alarmReport.setStatus(AlarmStatusType.OPEN);
        alarmReport.setUpdated(Instant.now());
        alarmReport.setCreatedDate(Instant.now());
        alarmReport.setUpdatedBy("Test user");
        return alarmReport;
    }

    private static AlarmQuery getBasicAlarmQuery() {
        AlarmQuery query = new AlarmQuery();
        query.setDynamic(true);
        ListPagination pagination = new ListPagination();
        pagination.setPage(BigInteger.valueOf(1));
        pagination.setListSize(BigInteger.valueOf(100));
        query.setPagination(pagination);
        return query;
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void getAlarmListTest() {
        AlarmQuery basicAlarmQuery = getBasicAlarmQuery();
        AlarmListCriteria criteria = new AlarmListCriteria();
        criteria.setKey(AlarmSearchKey.RULE_GUID);
        criteria.setValue("TEST_GUID");
        basicAlarmQuery.getAlarmSearchCriteria().add(criteria);

        AlarmListResponseDto alarmList = getWebTarget()
                .path("alarms/list")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .post(Entity.json(basicAlarmQuery), AlarmListResponseDto.class);

        assertThat(alarmList.getAlarmList().size(), is(notNullValue()));

        int prevNumberOfReports = alarmList.getAlarmList().size();

        AlarmReport alarmReport = getBasicAlarmReport();
        alarmDao.save(alarmReport);
        criteria.setKey(AlarmSearchKey.ALARM_GUID);
        criteria.setValue(alarmReport.getId().toString());

        alarmList = getWebTarget()
                .path("alarms/list")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .post(Entity.json(basicAlarmQuery), AlarmListResponseDto.class);

        assertThat(alarmList.getAlarmList().size(), is(prevNumberOfReports + 1));
        assertEquals(alarmReport.getId(), alarmList.getAlarmList().get(0).getId());
        assertEquals(alarmReport.getStatus(), alarmList.getAlarmList().get(0).getStatus());

        alarmDao.removeAlarmReportAfterTests(alarmReport);
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void negativeGetAlarmListTest() {
        Response response = getWebTarget()
                .path("alarms/list")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .post(Entity.json(new AlarmQuery()));

        assertEquals(200, response.getStatus());
        AppError appError = response.readEntity(AppError.class);
        assertEquals(500, appError.code.intValue());
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void updateAlarmStatusTest() {
        AlarmReport alarmReport = getBasicAlarmReport();
        alarmDao.save(alarmReport);

        alarmReport.setStatus(AlarmStatusType.REJECTED);

        AlarmReport output = getWebTarget()
                .path("alarms")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .put(Entity.json(alarmReport), AlarmReport.class);

        assertEquals(alarmReport.getId(), output.getId());
        assertEquals(AlarmStatusType.REJECTED, output.getStatus());

        alarmDao.removeAlarmReportAfterTests(alarmReport);
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void negativeUpdateAlarmStatusTest() {
        Response response = getWebTarget()
                .path("alarms")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .put(Entity.json(new AlarmReport()));

        assertEquals(200, response.getStatus());
        AppError appError = response.readEntity(AppError.class);
        assertEquals(500, appError.code.intValue());
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void getAlarmReportByGuidTest() {
        AlarmReport alarmReport = getBasicAlarmReport();
        alarmDao.save(alarmReport);

        AlarmReport responseAlarmReportType = getWebTarget()
                .path("alarms/" + alarmReport.getId())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(AlarmReport.class);

        assertNotNull(responseAlarmReportType);
        assertEquals(alarmReport.getId(), responseAlarmReportType.getId());

        alarmDao.removeAlarmReportAfterTests(alarmReport);
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void negativeGetAlarmReportByGuidTest() {
        Response response = getWebTarget()
                .path("alarms/" + "test guid")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get();

        assertEquals(200, response.getStatus());
        AppError appError = response.readEntity(AppError.class);
        assertEquals(500, appError.code.intValue());
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void reprocessAlarmTest() {
        Response response = getWebTarget()
                .path("alarms/reprocess")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .post(Entity.json(Collections.singletonList(UUID.randomUUID()))); // Previously "NULL_GUID"

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        AlarmReport alarmReport = getBasicAlarmReport();
        IncomingMovement incomingMovement = new IncomingMovement();
        incomingMovement.setUpdated(Instant.now());
        incomingMovement.setUpdatedBy("Test User");
        incomingMovement.setMobileTerminalActive(true);
        incomingMovement.setAlarmReport(alarmReport);
        incomingMovement.setPluginType("NAF");
        incomingMovement.setComChannelType("Test");
        alarmReport.setIncomingMovement(incomingMovement);
        alarmDao.save(alarmReport);

        response = getWebTarget()
                .path("alarms/reprocess")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .post(Entity.json(Collections.singletonList(alarmReport.getId())));

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        AlarmReport responseAlarmReportType = getWebTarget()
                .path("alarms/" + alarmReport.getId())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(AlarmReport.class);

        assertNotNull(responseAlarmReportType);
        assertEquals(AlarmStatusType.REPROCESSED, responseAlarmReportType.getStatus());
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void changeIncomingMovementTest() {
        AlarmReport alarmReport = getBasicAlarmReport();
        IncomingMovement incomingMovement = new IncomingMovement();
        incomingMovement.setUpdated(Instant.now());
        incomingMovement.setUpdatedBy("Test User");
        incomingMovement.setMobileTerminalActive(true);
        incomingMovement.setAlarmReport(alarmReport);
        incomingMovement.setPluginType("NAF");
        incomingMovement.setComChannelType("Test");
        alarmReport.setIncomingMovement(incomingMovement);
        alarmDao.save(alarmReport);

        assertNotNull(incomingMovement.getId());

        AlarmReport responseAlarmReportType = getWebTarget()
                .path("alarms/" + alarmReport.getId())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(AlarmReport.class);

        assertNotNull(responseAlarmReportType);
        IncomingMovement savedMovement = responseAlarmReportType.getIncomingMovement();

        savedMovement.setUpdatedBy("Another User");

        Response response = getWebTarget()
                .path("alarms/incomingMovement")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .put(Entity.json(savedMovement));

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        responseAlarmReportType = getWebTarget()
                .path("alarms/" + alarmReport.getId())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(AlarmReport.class);

        assertNotNull(responseAlarmReportType);
        assertEquals(AlarmStatusType.OPEN, responseAlarmReportType.getStatus());
        assertEquals("Another User", responseAlarmReportType.getIncomingMovement().getUpdatedBy());

        savedMovement.setAlarmReport(null);
        savedMovement.setUpdatedBy("Yet Another User");

        response = getWebTarget()
                .path("alarms/incomingMovement")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .put(Entity.json(savedMovement));

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        responseAlarmReportType = getWebTarget()
                .path("alarms/" + alarmReport.getId())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(AlarmReport.class);

        assertNotNull(responseAlarmReportType);
        assertEquals(AlarmStatusType.OPEN, responseAlarmReportType.getStatus());
        assertEquals("Yet Another User", responseAlarmReportType.getIncomingMovement().getUpdatedBy());
    }

    @Test
    @OperateOnDeployment("movementservice")
    public void getNumberOfOpenAlarmReportsTest() {
        Integer countOpenBefore = getWebTarget()
                .path("alarms/countopen")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(Integer.class);

        assertNotNull(countOpenBefore);

        AlarmReport alarmReport = getBasicAlarmReport();
        alarmDao.save(alarmReport);

        Integer countOpenAfter = getWebTarget()
                .path("alarms/countopen")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(Integer.class);

        assertEquals((int) countOpenAfter, countOpenBefore + 1);

        AlarmReport alarmReport2 = getBasicAlarmReport();
        alarmDao.save(alarmReport2);

        countOpenAfter = getWebTarget()
                .path("alarms/countopen")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(Integer.class);

        assertEquals((int) countOpenAfter, countOpenBefore + 2);

        alarmDao.removeAlarmReportAfterTests(alarmReport);
        alarmDao.removeAlarmReportAfterTests(alarmReport2);
    }
}
