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

import eu.europa.ec.fisheries.schema.movement.v1.MovementBaseType;
import eu.europa.ec.fisheries.schema.movement.v1.MovementType;
import eu.europa.ec.fisheries.uvms.commons.message.api.MessageException;
import eu.europa.ec.fisheries.uvms.movement.message.constants.ModuleQueue;
import eu.europa.ec.fisheries.uvms.movement.message.consumer.MessageConsumer;
import eu.europa.ec.fisheries.uvms.movement.message.exception.MovementMessageException;
import eu.europa.ec.fisheries.uvms.movement.message.producer.MessageProducer;
import eu.europa.ec.fisheries.uvms.movement.service.SpatialService;
import eu.europa.ec.fisheries.uvms.movement.service.exception.ErrorCode;
import eu.europa.ec.fisheries.uvms.movement.service.exception.MovementServiceException;
import eu.europa.ec.fisheries.uvms.movement.service.mapper.MovementMapper;
import eu.europa.ec.fisheries.uvms.spatial.model.exception.SpatialModelMapperException;
import eu.europa.ec.fisheries.uvms.spatial.model.mapper.SpatialModuleRequestMapper;
import eu.europa.ec.fisheries.uvms.spatial.model.mapper.SpatialModuleResponseMapper;
import eu.europa.ec.fisheries.uvms.spatial.model.schemas.AreaType;
import eu.europa.ec.fisheries.uvms.spatial.model.schemas.LocationType;
import eu.europa.ec.fisheries.uvms.spatial.model.schemas.PointType;
import eu.europa.ec.fisheries.uvms.spatial.model.schemas.SpatialEnrichmentRS;
import eu.europa.ec.fisheries.uvms.spatial.model.schemas.UnitType;
import java.util.Arrays;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.jms.JMSException;
import javax.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@LocalBean
@Stateless
public class SpatialServiceBean implements SpatialService {

    final static Logger LOG = LoggerFactory.getLogger(SpatialServiceBean.class);

    @EJB
    private MessageConsumer consumer;

    @EJB
    private MessageProducer producer;

    public MovementType enrichMovementWithSpatialData(MovementBaseType movement) throws MovementServiceException {
        try {
            LOG.debug("Enrich movement with spatial data envoked in SpatialServiceBean");
            PointType point = new PointType();
            point.setCrs(4326); //this magical int is the World Geodetic System 1984, aka EPSG:4326. See: https://en.wikipedia.org/wiki/World_Geodetic_System or http://spatialreference.org/ref/epsg/wgs-84/
            point.setLatitude(movement.getPosition().getLatitude());
            point.setLongitude(movement.getPosition().getLongitude());
            List<LocationType> locationTypes = Arrays.asList(LocationType.PORT);
            List<AreaType> areaTypes = Arrays.asList(AreaType.COUNTRY);
            String spatialRequest = SpatialModuleRequestMapper.mapToCreateSpatialEnrichmentRequest(point, UnitType.NAUTICAL_MILES, locationTypes, areaTypes);
            String spatialMessageId = producer.sendModuleMessage(spatialRequest, ModuleQueue.SPATIAL);
            TextMessage spatialResponse = consumer.getMessage(spatialMessageId, TextMessage.class);
            LOG.debug("Got response from Spatial " + spatialResponse.getText());
            SpatialEnrichmentRS enrichment = SpatialModuleResponseMapper.mapToSpatialEnrichmentRSFromResponse(spatialResponse, spatialMessageId);
            return MovementMapper.enrichAndMapToMovementType(movement, enrichment);
        } catch (JMSException | SpatialModelMapperException | MovementMessageException | MessageException ex) {
            throw new MovementServiceException("FAILED TO GET DATA FROM SPATIAL ", ex, ErrorCode.DATA_RETRIEVING_ERROR);
        }
    }
}
