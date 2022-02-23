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
package fish.focus.uvms.movement.rest.service;

import fish.focus.uvms.rest.security.RequiresFeature;
import fish.focus.uvms.rest.security.UnionVMSFeature;
import fish.focus.uvms.movement.rest.filter.AppError;
import fish.focus.uvms.movement.service.bean.ManualMovementService;
import fish.focus.uvms.movement.service.dto.ManualMovementDto;
import fish.focus.uvms.movement.service.entity.alarm.AlarmItem;
import fish.focus.uvms.movement.service.entity.alarm.AlarmReport;
import fish.focus.uvms.movement.service.validation.MovementSanityValidatorBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/manualMovement")
@Stateless
@Consumes(value = {MediaType.APPLICATION_JSON})
@Produces(value = {MediaType.APPLICATION_JSON})
public class ManualMovementRestResource {

    private final static Logger LOG = LoggerFactory.getLogger(ManualMovementRestResource.class);

    @Inject
    private ManualMovementService service;

    @Inject
    private MovementSanityValidatorBean validationService;

    @Context
    private HttpServletRequest request;

    @POST
    @RequiresFeature(UnionVMSFeature.manageManualMovements)
    public Response create(ManualMovementDto data) {
        LOG.debug("Create manual movement invoked in rest layer");
        try {
            UUID reportId = service.sendManualMovement(data, request.getRemoteUser());
            if(reportId == null){
                return Response.ok().header("MDC", MDC.get("requestId")).build();
            } else {
                AlarmReport report = validationService.getAlarmReportByGuid(reportId);
                String sanityRulesTriggered = report.getAlarmItemList().stream().map(AlarmItem::getRuleName).collect(Collectors.joining("\n"));
                return Response.ok().entity(new AppError(400, sanityRulesTriggered)).header("MDC", MDC.get("requestId")).build();
            }
        } catch (Exception e) {
            LOG.error("[ Error when creating a manual movement. ] {} ", e);
            throw e;
        }
    }

}
