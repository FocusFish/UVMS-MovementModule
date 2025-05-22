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
package fish.focus.uvms.movement.service.bean;

import fish.focus.schema.movement.search.v1.MovementQuery;
import fish.focus.schema.movement.source.v1.GetMovementMapByQueryResponse;
import fish.focus.schema.movement.v1.MovementSourceType;
import fish.focus.schema.movement.v1.MovementType;
import fish.focus.uvms.asset.client.AssetClient;
import fish.focus.uvms.config.exception.ConfigServiceException;
import fish.focus.uvms.config.service.ParameterService;
import fish.focus.uvms.movement.model.GetMovementListByQueryResponse;
import fish.focus.uvms.movement.model.dto.ListResponseDto;
import fish.focus.uvms.movement.model.dto.MovementDto;
import fish.focus.uvms.movement.service.constant.ParameterKey;
import fish.focus.uvms.movement.service.dao.MovementDao;
import fish.focus.uvms.movement.service.dto.CursorPagination;
import fish.focus.uvms.movement.service.entity.Movement;
import fish.focus.uvms.movement.service.entity.MovementConnect;
import fish.focus.uvms.movement.service.event.CreatedMovement;
import fish.focus.uvms.movement.service.mapper.MovementEntityToModelMapper;
import fish.focus.uvms.movement.service.mapper.MovementMapper;
import fish.focus.uvms.movement.service.mapper.MovementResponseMapper;
import fish.focus.uvms.movement.service.mapper.search.SearchFieldMapper;
import fish.focus.uvms.movement.service.mapper.search.SearchValue;
import fish.focus.uvms.movementrules.model.dto.VicinityInfoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Stateless
public class MovementService {

    private static final Logger LOG = LoggerFactory.getLogger(MovementService.class);

    @Inject
    private IncomingMovementBean incomingMovementBean;

    @Inject
    private MovementDao movementDao;

    @Inject
    private MovementMapResponseHelper movementMapResponseHelper;

    @Inject
    private AssetClient assetClient;

    @EJB
    private ParameterService parameterService;

    @Inject
    @CreatedMovement
    private Event<Movement> createdMovementEvent;

    public Movement createAndProcessMovement(Movement movement) {
        createMovement(movement);
        incomingMovementBean.processMovement(movement);
        fireMovementEvent(movement);
        return movement;
    }

    public List<Movement> createMovementBatch(List<Movement> movements) {
        for (Movement movement : movements) {
            createMovement(movement);
            incomingMovementBean.processMovement(movement);
        }
        return movements;

    }

    public Movement createMovement(Movement movement) {
        if (movement.getMovementConnect().getId() == null) {
            throw new IllegalArgumentException("No movementConnect ID");
        }
        try {
            // TODO remove, already called by MovementCreateBean
            MovementConnect moveConnect = getOrCreateMovementConnectByConnectId(movement.getMovementConnect());

            movement.setMovementConnect(moveConnect);
            return movementDao.createMovement(movement);
        } catch (Exception e) {
            throw new EJBException("Could not create movement.", e);
        }
    }

    public MovementConnect getOrCreateMovementConnectByConnectId(MovementConnect connect) {
        MovementConnect movementConnect;

        if (connect == null) {
            return null;
        }
        movementConnect = movementDao.getMovementConnectByConnectId(connect.getId());

        if (movementConnect == null) {
            LOG.info("Creating new MovementConnect");
            return movementDao.createMovementConnect(connect);
        }

        return movementConnect;
    }

    public GetMovementMapByQueryResponse getMapByQuery(MovementQuery query) {
        return movementMapResponseHelper.getMapByQuery(query);
    }


    public GetMovementListByQueryResponse getList(MovementQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Movement list query is null");
        }
        if (query.getPagination() == null || query.getPagination().getListSize() == null || query.getPagination().getPage() == null) {
            throw new IllegalArgumentException("Pagination in movementlist query is null");
        }
        if (query.getMovementSearchCriteria().isEmpty() && query.getMovementRangeSearchCriteria().isEmpty()) {
            throw new IllegalArgumentException("No search criterias in MovementList query");
        }
        try {
            ListResponseDto response = new ListResponseDto();
            List<MovementType> movementList = new ArrayList<>();

            int page = query.getPagination().getPage().intValue();
            int listSize = query.getPagination().getListSize().intValue();

            List<SearchValue> searchKeyValues = new ArrayList<>();
            List<SearchValue> searchKeyValuesList = SearchFieldMapper.mapListCriteriaToSearchValue(query.getMovementSearchCriteria());
            List<SearchValue> searchKeyValuesRange = SearchFieldMapper.mapRangeCriteriaToSearchField(query.getMovementRangeSearchCriteria());

            searchKeyValues.addAll(searchKeyValuesList);
            searchKeyValues.addAll(searchKeyValuesRange);

            String countSql = SearchFieldMapper.createCountSearchSql(searchKeyValues, true);
            String sql = SearchFieldMapper.createSelectSearchSql(searchKeyValues, true);

            Long numberMatches = movementDao.getMovementListSearchCount(countSql, searchKeyValues);
            List<Movement> movementEntityList = movementDao.getMovementListPaginated(page, listSize, sql, searchKeyValues);

            movementEntityList.forEach(movement -> movementList.add(MovementEntityToModelMapper.mapToMovementType(movement)));

            response.setCurrentPage(BigInteger.valueOf(page));
            response.setMovementList(movementList);
            response.setTotalNumberOfPages(BigInteger.valueOf(getNumberOfPages(numberMatches, listSize)));

            return MovementResponseMapper.createMovementListResponse(response);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error when getting movement list by query: ParseException", e);
        }
    }

    public List<MovementDto> getCursorBasedList(CursorPagination cursorPagination) {
        List<Movement> movementEntityList = movementDao.getCursorBasedList(cursorPagination);

        List<MovementDto> movementList = new ArrayList<>();
        for (Movement movement : movementEntityList) {
            movementList.add(MovementMapper.mapToMovementDto(movement));
        }
        return movementList;
    }

    public Movement getById(UUID id) {
        return movementDao.getMovementById(id);
    }


    private void fireMovementEvent(Movement createdMovement) {
        try {
            createdMovementEvent.fire(createdMovement);
        } catch (Exception e) {
            LOG.error("[ Error when firing notification of created temp movement. ] {}", e.getMessage());
        }
    }

    public List<Movement> getLatestMovementsByConnectIds(List<UUID> connectIds) {
        return movementDao.getLatestMovementsByConnectIdList(connectIds);
    }

    public List<Movement> getLatestMovements(Integer numberOfMovements) {
        return movementDao.getLatestMovements(numberOfMovements);
    }

    public Movement getPreviousVMSLastMonth(UUID connectId, Instant timestamp) {
        List<MovementSourceType> sources = Arrays.stream(MovementSourceType.values())
                .filter(source -> !source.equals(MovementSourceType.AIS))
                .collect(Collectors.toList());
        return movementDao.getPreviousMovementBetweenDates(connectId, timestamp.minus(30, ChronoUnit.DAYS), timestamp, sources);
    }

    private int getNumberOfPages(Long numberOfMovements, int listSize) {
        int numberOfPages = (int) (numberOfMovements / listSize);
        if (numberOfMovements % listSize != 0) {
            numberOfPages += 1;
        }
        return numberOfPages;
    }

    public int countNrOfMovementsLastDayForAsset(String asset, Instant positionTime) {
        return (int) movementDao.countNrOfMovementsForAssetBetween(UUID.fromString(asset), positionTime.minus(1,
                ChronoUnit.DAYS), positionTime);
    }

    public List<MovementDto> getLatestMovementsLast8Hours(List<MovementSourceType> sources) {
        return getLatestMovementsAfter(Instant.now().minus(8, ChronoUnit.HOURS), sources);
    }

    public List<MovementDto> getLatestMovementsAfter(Instant date, List<MovementSourceType> sources) {
        return movementDao.getLatestWithLimit(date, sources);
    }

    public List<VicinityInfoDTO> getVicinityOf(Movement movement) {
        try {
            String maxDistance = parameterService.getStringValue(ParameterKey.MAX_DISTANCE.getKey());
            return movementDao.getVicinityOfMovement(movement, Double.parseDouble(maxDistance));
        } catch (ConfigServiceException | NullPointerException | NumberFormatException e) {
            LOG.error("Could not parse maxDistance parameter!");
            return new ArrayList<>();
        }
    }

    public String getAssetList(List<String> assetIds) {
        return assetClient.getAssetList(assetIds);
    }

    public int remapMovementConnectInMovement(String oldMovementConnectId, String newMovementConnectId) {
        if (oldMovementConnectId == null || oldMovementConnectId.isEmpty()) {
            throw new IllegalArgumentException("OldMovementConnectString is null or empty");
        }

        if (newMovementConnectId == null || newMovementConnectId.isEmpty()) {
            throw new IllegalArgumentException("NewMovementConnectString is null or empty");
        }

        MovementConnect oldMovementConnect = movementDao.getMovementConnectByConnectId(UUID.fromString(oldMovementConnectId));
        if (oldMovementConnect == null) { //I really dont understand the how as to why this can be null but according to the log in prod it can
            return 0;
        }
        MovementConnect newMovementConnect = movementDao.getMovementConnectByConnectId(UUID.fromString(newMovementConnectId));

        if (newMovementConnect == null) {
            newMovementConnect = new MovementConnect();
            newMovementConnect.setUpdated(Instant.now());
            newMovementConnect.setUpdatedBy("UVMS");
            newMovementConnect.setId(UUID.fromString(newMovementConnectId));

            newMovementConnect = movementDao.createMovementConnect(newMovementConnect);
        }

        LOG.info("Remapping old positions for {} to {}", oldMovementConnect.getId(), newMovementConnect);

        return movementDao.updateToNewMovementConnect(oldMovementConnect.getId(), newMovementConnect.getId(), 10000);
    }

    public void removeMovementConnect(String movementConnectId) {
        MovementConnect toBeDeleted = movementDao.getMovementConnectByConnectId(UUID.fromString(movementConnectId));
        if (toBeDeleted != null) {
            movementDao.deleteMovementConnect(toBeDeleted);
        }
    }

    public List<MovementDto> getMovementsByMoveIds(List<UUID> moveIds) {
        List<Movement> movementsByMoveIdList = movementDao.getMovementsByMoveIdList(moveIds);
        return MovementMapper.mapToMovementDtoList(movementsByMoveIdList);
    }
}
