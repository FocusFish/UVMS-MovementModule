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
package eu.europa.ec.fisheries.uvms.movement.service.message;

import eu.europa.ec.fisheries.uvms.commons.message.api.MessageConstants;
import eu.europa.ec.fisheries.uvms.commons.message.impl.AbstractConsumer;
import eu.europa.ec.fisheries.uvms.config.exception.ConfigMessageException;
import eu.europa.ec.fisheries.uvms.config.message.ConfigMessageConsumer;
import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.jms.Destination;
import javax.jms.JMSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@LocalBean
@Stateless
public class MovementConsumerBean extends AbstractConsumer implements ConfigMessageConsumer {

    private static final long CONFIG_TIMEOUT = 600000L;

    private static final Logger LOG = LoggerFactory.getLogger(MovementConsumerBean.class);

    @Resource(mappedName = "java:/" + MessageConstants.QUEUE_MOVEMENT)
    private Destination destination;

    @Override
    public <T> T getConfigMessage(String correlationId, Class<T> type) throws ConfigMessageException {
        try {
            return getMessage(correlationId, type, CONFIG_TIMEOUT);
        } catch (JMSException e) {
            LOG.error("[ Error when getting message ] {}", e.getMessage());
            throw new ConfigMessageException("Error when retrieving message: ");
        } 
    }

    @Override
    public Destination getDestination() {
        return destination;
    }

}