package fish.focus.uvms.movement.service.bean;

import fish.focus.schema.movement.v1.MovementSourceType;
import fish.focus.uvms.config.exception.ConfigServiceException;
import fish.focus.uvms.config.service.ParameterService;
import fish.focus.uvms.movement.service.constant.ParameterKey;
import fish.focus.uvms.movement.service.dao.MovementDao;
import fish.focus.uvms.movement.service.entity.IncomingMovement;
import fish.focus.uvms.movement.service.entity.Movement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Stateless
public class IncomingMovementBean {

    private static final Logger LOG = LoggerFactory.getLogger(IncomingMovementBean.class);

    @EJB
    private ParameterService parameterService;

    @Inject
    private TrackService trackService;

    @Inject
    private MovementDao dao;

    public void processMovement(Movement currentMovement) {
        boolean trackInMovementEnabled = true;
        try {
            if ("false".equalsIgnoreCase(parameterService.getStringValue(ParameterKey.TRACK_IN_MOVEMENT_ENABLED.getKey()))) {
                trackInMovementEnabled = false;
            }
        } catch (ConfigServiceException e) {
            LOG.info("Cannot find parameter {} !", ParameterKey.TRACK_IN_MOVEMENT_ENABLED.getKey());
        }
        if (currentMovement == null) {
            throw new IllegalArgumentException("Movement to process is null!");
        }
        UUID connectId = currentMovement.getMovementConnect().getId();
        Instant timeStamp = currentMovement.getTimestamp();

        Movement latestMovement = currentMovement.getMovementConnect().getLatestMovement();
        if (latestMovement == null) { // First position
            setLatest(currentMovement);
        } else {
            try {
                if (currentMovement.getTimestamp().isAfter(latestMovement.getTimestamp())) {
                    // Normal case (latest position)
                    currentMovement.setPreviousMovement(latestMovement);
                        setLatest(currentMovement);
                    if (trackInMovementEnabled) {
                        trackService.upsertTrack(latestMovement, currentMovement);
                    }
                } else {
                    Movement previousMovement = dao.getPreviousMovement(connectId, timeStamp);
                    if (previousMovement == null) { // Before first position
                        if (trackInMovementEnabled) {
                            Movement firstMovement = dao.getFirstMovement(connectId, currentMovement.getId());
                            trackService.upsertTrack(firstMovement, currentMovement);
                        }
                    } else { // Between two positions
                        currentMovement.setPreviousMovement(previousMovement);
                        if (trackInMovementEnabled) {
                            trackService.upsertTrack(previousMovement, currentMovement);
                        }
                    }
                }
            } catch (EntityNotFoundException e){
                LOG.error("Couldn't get latestMovement for movementConnect, source: {}, uuid: {}, timestamp: {}", currentMovement.getSource(), connectId, timeStamp);
                setLatest(currentMovement);
            }
        }
        updateLatestVMS(currentMovement);
    }
    private static void setLatest(Movement currentMovement) {
        currentMovement.getMovementConnect().setLatestMovement(currentMovement);
        currentMovement.getMovementConnect().setLatestLocation(currentMovement.getLocation());
    }

    private void updateLatestVMS(Movement currentMovement) {
        if (currentMovement.getSource().equals(MovementSourceType.AIS)) {
            return;
        }
        Movement latestVMS = currentMovement.getMovementConnect().getLatestVMS();
        if (latestVMS == null || currentMovement.getTimestamp().isAfter(latestVMS.getTimestamp())) {
            currentMovement.getMovementConnect().setLatestVMS(currentMovement);
        }
    }
    
    public boolean checkAndSetDuplicate(IncomingMovement movement) {
        if(movement.getPositionTime() == null || movement.getAssetGuid() == null){     //if these two are null the check cant complete and one of the other sanity rules will get it
            return false;
        }
        UUID connectId = UUID.fromString(movement.getAssetGuid());
        Instant timeStamp = movement.getPositionTime();
        MovementSourceType source = MovementSourceType.fromValue(movement.getMovementSourceType());

        List<Movement> duplicateMovements = dao.isDateAlreadyInserted(connectId, timeStamp, source);
        if (!duplicateMovements.isEmpty()) {
            // If they have different movement types or different source
            if (!Objects.equals(movement.getMovementType(), duplicateMovements.get(0).getMovementType().value())) {
                Instant newDate = timeStamp.plusSeconds(1);
                movement.setPositionTime(newDate);
            } else {
                LOG.info("Got a duplicate movement for Asset {}. Marking it as such.", movement.getAssetGuid());
                movement.setDuplicate(true);
                return true;
            }
        }
        return false;
    }
}