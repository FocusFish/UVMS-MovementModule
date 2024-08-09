package fish.focus.uvms.movement.service.validation;

import fish.focus.schema.exchange.plugin.types.v1.PluginType;
import fish.focus.schema.movement.search.v1.ListPagination;
import fish.focus.uvms.movement.service.bean.MovementCreateBean;
import fish.focus.uvms.movement.service.dao.AlarmDAO;
import fish.focus.uvms.movement.service.dto.*;
import fish.focus.uvms.movement.service.entity.IncomingMovement;
import fish.focus.uvms.movement.service.entity.alarm.AlarmItem;
import fish.focus.uvms.movement.service.entity.alarm.AlarmReport;
import fish.focus.uvms.movement.service.mapper.search.AlarmSearchFieldMapper;
import fish.focus.uvms.movement.service.mapper.search.AlarmSearchValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Stateless
public class MovementSanityValidatorBean {

    private static final Logger LOG = LoggerFactory.getLogger(MovementSanityValidatorBean.class);

    @Inject
    private AlarmDAO alarmDAO;

    @Inject
    private MovementCreateBean movementCreate;

    public UUID evaluateSanity(IncomingMovement movement) {
        UUID reportId = null;
        for (SanityRule sanityRule : SanityRule.values()) {
            if (sanityRule.evaluate(movement)) {
                LOG.info("\t==> Executing RULE {}", sanityRule.getRuleName());
                reportId = createAlarmReport(sanityRule.getRuleName(), movement);
            }
        }
        return reportId;
    }

    public UUID createAlarmReport(String ruleName, IncomingMovement movement) {

        LOG.info("Create alarm invoked in validation service, rule: {}", ruleName);

        AlarmReport alarmReport = alarmDAO.getOpenAlarmReportByMovementGuid(movement.getId());
        if (alarmReport == null) {
            alarmReport = new AlarmReport();
            alarmReport.setAssetGuid(movement.getAssetGuid());
            alarmReport.setCreatedDate(Instant.now());
            alarmReport.setPluginType(movement.getPluginType() != null ? PluginType.fromValue(movement.getPluginType()) : null);
            //alarmReport.setRecipient();
            alarmReport.setStatus(AlarmStatusType.OPEN);
            alarmReport.setUpdated(Instant.now());
            alarmReport.setUpdatedBy("UVMS");
            alarmReport.setIncomingMovement(movement);
            alarmReport.setAlarmItemList(new ArrayList<>());
            alarmDAO.save(alarmReport);
        }


        AlarmItem item = new AlarmItem();
        item.setAlarmReport(alarmReport);
        item.setRuleGuid(ruleName); // WTF?
        item.setRuleName(ruleName);
        item.setUpdated(Instant.now());
        item.setUpdatedBy("UVMS");
        alarmDAO.save(item);

        alarmReport.getAlarmItemList().add(item);

        return alarmReport.getId();
    }

    public AlarmListResponseDto getAlarmList(AlarmQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Alarm list query is null");
        }
        if (query.getPagination() == null) {
            throw new IllegalArgumentException("Pagination in alarm list query is null");
        }

        BigInteger page = query.getPagination().getPage();
        BigInteger listSize = query.getPagination().getListSize();

        List<AlarmSearchValue> searchKeyValues = AlarmSearchFieldMapper.mapSearchField(query.getAlarmSearchCriteria());

        String sql = AlarmSearchFieldMapper.createSelectSearchSql(searchKeyValues, query.isDynamic());
        String countSql = AlarmSearchFieldMapper.createCountSearchSql(searchKeyValues, query.isDynamic());

        Long numberMatches = alarmDAO.getAlarmListSearchCount(countSql);
        List<AlarmReport> alarmEntityList = alarmDAO.getAlarmListPaginated(page.intValue(), listSize.intValue(), sql);

        int numberOfPages = (int) (numberMatches / listSize.longValue());
        if (numberMatches % listSize.longValue() != 0) {
            numberOfPages += 1;
        }

        AlarmListResponseDto response = new AlarmListResponseDto();
        response.setTotalNumberOfPages(numberOfPages);
        response.setCurrentPage(query.getPagination().getPage().intValue());
        response.setAlarmList(alarmEntityList);

        return response;
    }

    public AlarmReport updateAlarmStatus(AlarmReport alarm) {
        AlarmReport entity = alarmDAO.getAlarmReportByGuid(alarm.getId());
        if (entity == null) {
            throw new IllegalArgumentException("Alarm is null", null);
        }

        entity.setStatus(alarm.getStatus());
        entity.setUpdatedBy(alarm.getUpdatedBy());
        entity.setUpdated(Instant.now());
        /* TODO: WAT isInactivatePosition()
        if (entity.getIncomingMovement() != null) {
            entity.getIncomingMovement().setActive(!alarm.getIncomingMovement().getActive());
        }
        */

        alarmDAO.merge(entity);

        return entity;
    }

    public IncomingMovement updateIncomingMovement(IncomingMovement movement) {
        if (movement == null || movement.getId() == null) {
            throw new IllegalArgumentException("IncomingMovement or its ID is null");
        }

        if (movement.getAlarmReport() == null) {
            movement.setAlarmReport(alarmDAO.getOpenAlarmReportByMovementGuid(movement.getId()));
        }
        movement.getAlarmReport().setIncomingMovement(movement); //since these two infinetly recurse we make sure that they recurse into each other
        movement.setUpdated(Instant.now());
        alarmDAO.merge(movement);

        return movement;
    }

    public AlarmReport getAlarmReportByGuid(UUID guid) {
        return alarmDAO.getAlarmReportByGuid(guid);
    }

    public String reprocessAlarm(List<String> alarmGuids, String username) {
        AlarmQuery query = mapToOpenAlarmQuery(alarmGuids);
        AlarmListResponseDto alarms = getAlarmList(query);

        for (AlarmReport alarm : alarms.getAlarmList()) {
            // Cannot reprocess without a movement (i.e. "Asset not sending" alarm)
            if (alarm.getIncomingMovement() == null) {
                continue;
            }

            // Mark the alarm as REPROCESSED before reprocessing. That will create a new alarm (if still wrong) with the items remaining.
            alarm.setStatus(AlarmStatusType.REPROCESSED);
            alarm = updateAlarmStatus(alarm);

            IncomingMovement incomingMovement = alarm.getIncomingMovement();
            movementCreate.processIncomingMovement(incomingMovement);
        }

        return "OK";
    }

    private AlarmQuery mapToOpenAlarmQuery(List<String> alarmGuids) {
        AlarmQuery query = new AlarmQuery();
        ListPagination pagination = new ListPagination();
        pagination.setListSize(BigInteger.valueOf(alarmGuids.size()));
        pagination.setPage(BigInteger.valueOf(1));
        query.setPagination(pagination);

        for (String alarmGuid : alarmGuids) {
            AlarmListCriteria criteria = new AlarmListCriteria();
            criteria.setKey(AlarmSearchKey.ALARM_GUID);
            criteria.setValue(alarmGuid);
            query.getAlarmSearchCriteria().add(criteria);
        }

        // We only want open alarms
        AlarmListCriteria openCrit = new AlarmListCriteria();
        openCrit.setKey(AlarmSearchKey.STATUS);
        openCrit.setValue(AlarmStatusType.OPEN.name());
        query.getAlarmSearchCriteria().add(openCrit);
        query.setDynamic(true);
        return query;
    }


    public long getNumberOfOpenAlarmReports() {
        LOG.info("Counting open alarms");
        return alarmDAO.getNumberOfOpenAlarms();
    }

}
