package fish.focus.uvms.movement.service.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fish.focus.uvms.movement.service.dto.AlarmStatusType;
import fish.focus.uvms.movement.service.entity.alarm.AlarmReport;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.UUID;

@Stateless
public class AlarmDAO {

    private static final Logger LOG = LoggerFactory.getLogger(AlarmDAO.class);

    @PersistenceContext
    private EntityManager em;

    public AlarmReport getOpenAlarmReportByMovementGuid(UUID guid) {
        try {
            TypedQuery<AlarmReport> query = em.createNamedQuery(AlarmReport.FIND_ALARM_REPORT_BY_MOVEMENT_GUID_AND_STATUS, AlarmReport.class);
            query.setParameter("movementGuid", guid);
            query.setParameter("status", AlarmStatusType.OPEN);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public void save(Object entity) {
        em.persist(entity);
    }

    public void merge(Object entity) {
        em.merge(entity);
    }

    public AlarmReport getAlarmReportByGuid(UUID guid) {
            return em.find(AlarmReport.class, guid);
    }

    public long getNumberOfOpenAlarms() {
        TypedQuery<Long> query = em.createNamedQuery(AlarmReport.COUNT_ALARMS_BY_STATUS, Long.class);
        query.setParameter("status", AlarmStatusType.OPEN);
        return query.getSingleResult();
    }

    public Long getAlarmListSearchCount(String countSql) {
        LOG.debug("ALARM SQL QUERY IN LIST COUNT: {}", countSql);

        TypedQuery<Long> query = em.createQuery(countSql, Long.class);
        return query.getSingleResult();
    }

    public List<AlarmReport> getAlarmListPaginated(Integer page, Integer listSize, String sql) {

        LOG.debug("ALARM SQL QUERY IN LIST PAGINATED: {}", sql);

        TypedQuery<AlarmReport> query = em.createQuery(sql, AlarmReport.class);
        query.setFirstResult(listSize * (page - 1));
        query.setMaxResults(listSize);
        return query.getResultList();
    }

    public void removeAlarmReportAfterTests(AlarmReport alarmReport) {
        em.remove(em.contains(alarmReport) ? alarmReport : em.merge(alarmReport));
    }

}