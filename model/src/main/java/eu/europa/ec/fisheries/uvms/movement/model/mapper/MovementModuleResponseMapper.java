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
package eu.europa.ec.fisheries.uvms.movement.model.mapper;

import eu.europa.ec.fisheries.schema.movement.common.v1.AcknowledgeType;
import eu.europa.ec.fisheries.schema.movement.common.v1.AcknowledgeTypeType;
import eu.europa.ec.fisheries.schema.movement.common.v1.ExceptionType;
import eu.europa.ec.fisheries.schema.movement.common.v1.SimpleResponse;
import eu.europa.ec.fisheries.schema.movement.module.v1.*;
import eu.europa.ec.fisheries.schema.movement.search.v1.MovementMapResponseType;
import eu.europa.ec.fisheries.schema.movement.v1.MovementType;
import eu.europa.ec.fisheries.uvms.movement.model.exception.*;

import java.util.HashMap;
import java.util.List;
import javax.jms.JMSException;
import javax.jms.TextMessage;

public class MovementModuleResponseMapper {

    private static void validateResponse(TextMessage response, String correlationId) throws ModelMapperException, JMSException, MovementFaultException, MovementDuplicateException {

        if (response == null) {
            throw new ModelMapperException("Error when validating response in ResponseMapper: Reesponse is Null");
        }

        if (response.getJMSCorrelationID() == null) {
            throw new ModelMapperException("No corelationId in response (Null) . Expected was: " + correlationId);
        }

        if (!correlationId.equalsIgnoreCase(response.getJMSCorrelationID())) {
            throw new ModelMapperException("Wrong corelationId in response. Expected was: " + correlationId + "But actual was: " + response.getJMSCorrelationID());
        }

        try {
            ExceptionType movementFault = JAXBMarshaller.unmarshallTextMessage(response, ExceptionType.class);
            if (movementFault.getCode() == 409) {
                throw new MovementDuplicateException(movementFault.getFault());
            }
            throw new MovementFaultException(response.getText(), movementFault);
        } catch (ModelMarshallException e) {
            // All is well
        }
    }
 
    public static String mapTogetMovementListByQueryResponse(List<MovementType> movementList) throws ModelMarshallException {
        GetMovementListByQueryResponse response = new GetMovementListByQueryResponse();
        response.getMovement().addAll(movementList);
        return JAXBMarshaller.marshallJaxBObjectToString(response);
    }

    public static String mapToCreateMovementResponse(MovementType movment) throws ModelMarshallException {
        CreateMovementResponse response = new CreateMovementResponse();
        response.setMovement(movment);
        return JAXBMarshaller.marshallJaxBObjectToString(response);
    }

    public static String mapToMovementMapResponse(List<MovementMapResponseType> responseList) throws ModelMarshallException {
        GetMovementMapByQueryResponse response = new GetMovementMapByQueryResponse();
        response.getMovementMap().addAll(responseList);
        return JAXBMarshaller.marshallJaxBObjectToString(response);
    }

    public static String mapToCreateMovementBatchResponse(SimpleResponse value) throws ModelMarshallException {
        CreateMovementBatchResponse response = new CreateMovementBatchResponse();
        response.setResponse(value);
        return JAXBMarshaller.marshallJaxBObjectToString(response);
    }

    public static HashMap<String, List<MovementType>> mapToMovementMap(List<MovementMapResponseType> movements) throws ModelMarshallException {
        HashMap<String, List<MovementType>> movementMap = new HashMap<>();
        for (MovementMapResponseType movement : movements) {
            movementMap.put(movement.getKey(), movement.getMovements());
        }
        return movementMap;
    }

    public static List<MovementType> mapToMovementListResponse(TextMessage message) throws ModelMarshallException, JMSException, ModelMapperException, MovementFaultException, MovementDuplicateException {
        validateResponse(message, message.getJMSCorrelationID());
        GetMovementListByQueryResponse response = JAXBMarshaller.unmarshallTextMessage(message, GetMovementListByQueryResponse.class);
        return response.getMovement();
    }

    public static List<MovementMapResponseType> mapToMovementMapResponse(TextMessage message) throws ModelMarshallException, ModelMapperException, JMSException, MovementFaultException, MovementDuplicateException {
        validateResponse(message, message.getJMSCorrelationID());
        GetMovementMapByQueryResponse response = JAXBMarshaller.unmarshallTextMessage(message, GetMovementMapByQueryResponse.class);
        return response.getMovementMap();
    }

    public static SimpleResponse mapToSimpleResponseFromCreateMovementBatch(TextMessage message) throws ModelMarshallException, JMSException, ModelMapperException, MovementFaultException, MovementDuplicateException {
        validateResponse(message, message.getJMSCorrelationID());
        CreateMovementBatchResponse response = JAXBMarshaller.unmarshallTextMessage(message, CreateMovementBatchResponse.class);
        return response.getResponse();
    }

    public static CreateMovementResponse mapToCreateMovementResponseFromMovementResponse(TextMessage message) throws ModelMarshallException, JMSException, ModelMapperException, MovementFaultException, MovementDuplicateException {
        validateResponse(message, message.getJMSCorrelationID());
        CreateMovementResponse response = JAXBMarshaller.unmarshallTextMessage(message, CreateMovementResponse.class);
        return response;
    }

    public static String mapTogetMovementListByAreaAndTimeIntervalResponse(List<MovementType> movementList) throws ModelMarshallException {
        GetMovementListByAreaAndTimeIntervalResponse response = new GetMovementListByAreaAndTimeIntervalResponse();
        response.getMovement().addAll(movementList);
        return JAXBMarshaller.marshallJaxBObjectToString(response);
    }

    public static ProcessedMovementAck mapProcessedMovementAck(AcknowledgeTypeType ack, String messageId, String message) {
        ProcessedMovementAck resp = new ProcessedMovementAck();
        AcknowledgeType ackType = new AcknowledgeType();
        ackType.setMessage(message);
        ackType.setMessageId(messageId);
        resp.setResponse(ackType);
        return resp;
    }

}