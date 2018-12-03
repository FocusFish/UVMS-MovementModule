/*
﻿Developed with the contribution of the European Commission - Directorate General for Maritime Affairs and Fisheries
© European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can
redistribute it and/or modify it under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or any later version. The IFDM Suite is distributed in
the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. You should have received a
copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */
package eu.europa.ec.fisheries.uvms.movement.service.bean;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import eu.europa.ec.fisheries.uvms.audit.model.mapper.AuditLogMapper;
import eu.europa.ec.fisheries.uvms.movement.model.constants.AuditObjectTypeEnum;
import eu.europa.ec.fisheries.uvms.movement.model.constants.AuditOperationEnum;
import eu.europa.ec.fisheries.uvms.movement.service.message.MovementMessageProducerBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import eu.europa.ec.fisheries.schema.movement.v1.MovementTypeType;
import eu.europa.ec.fisheries.uvms.audit.model.exception.AuditModelMarshallException;
import eu.europa.ec.fisheries.uvms.movement.service.entity.Movement;
import eu.europa.ec.fisheries.uvms.movement.service.entity.temp.DraftMovement;
import eu.europa.ec.fisheries.uvms.movement.service.message.ModuleQueue;
import eu.europa.ec.fisheries.uvms.movement.service.message.AuditModuleRequestMapper;

@Stateless
public class AuditService {

    private static final Logger LOG = LoggerFactory.getLogger(AuditService.class);

    @Inject
    private MovementMessageProducerBean producer;

    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void sendMovementCreatedAudit(Movement movement, String username) {
        try {
            String auditData;
            if (MovementTypeType.MAN.equals(movement.getMovementType())) {
                auditData = AuditModuleRequestMapper.mapAuditLogManualMovementCreated(movement.getGuid(), username);
            } else {
                auditData = AuditModuleRequestMapper.mapAuditLogMovementCreated(movement.getGuid(), username);
            }
            producer.sendModuleMessage(auditData, ModuleQueue.AUDIT);
        } catch (AuditModelMarshallException e) {
            LOG.error("Failed to send audit log message! Movement with guid {} was created ", movement.getGuid(), e);
        }
    }
    
    // TODO what should be audited during batch?
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void sendMovementBatchCreatedAudit(String guid, String username) {
        try {
            String auditData = AuditModuleRequestMapper.mapAuditLogMovementBatchCreated(guid, username);
            producer.sendModuleMessage(auditData, ModuleQueue.AUDIT);
        } catch (AuditModelMarshallException e) {
            LOG.error("Failed to send audit log message! Movement batch with guid {} was created ", guid, e);
        }
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void sendTempMovementCreatedAudit(DraftMovement draftMovement, String username) {
        try {
            String auditRequest = AuditModuleRequestMapper.mapAuditLogTempMovementCreated(draftMovement.getId().toString(),
                    username);
            producer.sendModuleMessage(auditRequest, ModuleQueue.AUDIT);
        } catch (AuditModelMarshallException e) {
            LOG.error("Failed to send audit log message! DraftMovement with guid {} was created ", draftMovement.getId().toString(), e);
        }
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void sendAuditMessage(AuditObjectTypeEnum type, AuditOperationEnum operation, String affectedObject, String comment, String username) {
        try {
            String message = AuditLogMapper.mapToAuditLog(type.getValue(), operation.getValue(), affectedObject, comment, username);
            producer.sendModuleMessage(message, ModuleQueue.AUDIT);
        } catch (AuditModelMarshallException e) {
            LOG.error("[ERROR] Error when sending message to Audit. ] {}", e.getMessage());
        }
    }

}
