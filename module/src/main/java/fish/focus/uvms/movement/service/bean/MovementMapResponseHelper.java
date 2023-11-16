package fish.focus.uvms.movement.service.bean;

import fish.focus.schema.movement.search.v1.ListCriteria;
import fish.focus.schema.movement.search.v1.MovementMapResponseType;
import fish.focus.schema.movement.search.v1.MovementQuery;
import fish.focus.schema.movement.search.v1.SearchKey;
import fish.focus.schema.movement.source.v1.GetMovementMapByQueryResponse;
import fish.focus.schema.movement.v1.MovementSegment;
import fish.focus.schema.movement.v1.MovementTrack;
import fish.focus.schema.movement.v1.MovementType;
import fish.focus.uvms.config.exception.ConfigServiceException;
import fish.focus.uvms.config.service.ParameterService;
import fish.focus.uvms.movement.service.constant.ParameterKey;
import fish.focus.uvms.movement.service.dao.MovementDao;
import fish.focus.uvms.movement.service.entity.Movement;
import fish.focus.uvms.movement.service.entity.Track;
import fish.focus.uvms.movement.service.mapper.MovementEntityToModelMapper;
import fish.focus.uvms.movement.service.mapper.MovementResponseMapper;
import fish.focus.uvms.movement.service.mapper.search.SearchField;
import fish.focus.uvms.movement.service.mapper.search.SearchFieldMapper;
import fish.focus.uvms.movement.service.mapper.search.SearchValue;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@Stateless
public class MovementMapResponseHelper {
    private static final Logger LOG = LoggerFactory.getLogger(MovementMapResponseHelper.class);

    @EJB
    private ParameterService parameterService;

    @Inject
    private MovementDao movementDao;

    private boolean trackInMovementEnabled = true;

    public GetMovementMapByQueryResponse getMapByQuery(MovementQuery query) {

        validateQuery(query);

        boolean getLatestReports = query.getMovementSearchCriteria()
                .stream()
                .anyMatch(criteria -> criteria.getKey().equals(SearchKey.NR_OF_LATEST_REPORTS));

        int numberOfLatestReports = getNumberOfLatestReports(query, getLatestReports);

        try {
            List<MovementMapResponseType> mapResponse = new ArrayList<>();

            List<SearchValue> searchKeys = new ArrayList<>();
            List<SearchValue> searchKeyValuesList = SearchFieldMapper.mapListCriteriaToSearchValue(query.getMovementSearchCriteria());
            List<SearchValue> searchKeyValuesRange = SearchFieldMapper.mapRangeCriteriaToSearchField(query.getMovementRangeSearchCriteria());

            searchKeys.addAll(searchKeyValuesList);
            searchKeys.addAll(searchKeyValuesRange);

            String sql = SearchFieldMapper.createSelectSearchSql(searchKeys, true);
            List<Movement> movementEntityList = new ArrayList<>();

            if ( numberOfLatestReports > 0) {
                List<SearchValue> connectedIdsFromSearchKeyValues = getConnectedIdsFromSearchKeyValues(searchKeyValuesList);
                if(!connectedIdsFromSearchKeyValues.isEmpty() && connectedIdsFromSearchKeyValues.size()>1) {
                    getMovementsByConnectedIds(numberOfLatestReports, searchKeys, movementEntityList, connectedIdsFromSearchKeyValues);
                }else{
                    movementEntityList = movementDao.getMovementList(sql, searchKeys, numberOfLatestReports);
                }
            } else {
                movementEntityList = movementDao.getMovementList(sql, searchKeys);
            }

            Map<UUID, List<Movement>> orderMovementsByConnectId = MovementEntityToModelMapper.orderMovementsByConnectId(movementEntityList);

            for (Map.Entry<UUID, List<Movement>> entries : orderMovementsByConnectId.entrySet()) {

                MovementMapResponseType responseType = new MovementMapResponseType();

                responseType.setKey(entries.getKey().toString());

                List<MovementSegment> segmentList = MovementEntityToModelMapper.mapToMovementSegment(entries.getValue(), query.isExcludeFirstAndLastSegment());
                List<MovementSegment> filteredSegments = filterSegments(segmentList, searchKeyValuesRange);

                responseType.getSegments().addAll(filteredSegments);

                List<MovementType> mapToMovementType = MovementEntityToModelMapper.mapToMovementType(entries.getValue());
                responseType.getMovements().addAll(mapToMovementType);

                try {
                    if ("false".equalsIgnoreCase(parameterService.getStringValue(ParameterKey.TRACK_IN_MOVEMENT_ENABLED.getKey()))) {
                        trackInMovementEnabled = false;
                    }
                } catch (ConfigServiceException e) {
                    LOG.info("Cannot find parameter {} ", ParameterKey.TRACK_IN_MOVEMENT_ENABLED.getKey());
                }
                if (trackInMovementEnabled) {
                    List<Track> tracks = MovementEntityToModelMapper.extractTracks(entries.getValue());
                    List<MovementTrack> extractTracks = new ArrayList<>();
                    for (Track track : tracks) {
                        List<Geometry> points = movementDao.getPointsFromTrack(track, 500); //Maybe will increase performance 4 times compared to 2000
                        extractTracks.add(MovementEntityToModelMapper.mapToMovementTrack(track, points));
                    }

                    // In the rare event of segments that are attached to two different tracks, the track that is not
                    //connected to the any relevant Movement should be removed from the search result.
                    removeTrackMismatches(extractTracks, entries.getValue());
                    responseType.getTracks().addAll(extractTracks);
                }

                mapResponse.add(responseType);

            }
            return MovementResponseMapper.createMovementMapResponse(mapResponse);
        } catch (Exception  ex) {
            throw new RuntimeException("Error when getting movement map by query", ex);
        }
    }

    private void validateQuery(MovementQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Movement list query is null");
        }
        if (query.getMovementSearchCriteria() == null) {
            throw new IllegalArgumentException("No search criterias in MovementList query");
        }
        if (query.getPagination() != null) {
            throw new IllegalArgumentException("Pagination not supported in get movement map by query");
        }
    }

    private int getNumberOfLatestReports(MovementQuery query, boolean getLatestReports) {
        int numberOfLatestReports = 0;

        if (getLatestReports) {
            Optional<String> first = query.getMovementSearchCriteria()
                    .stream()
                    .filter(criteria -> criteria.getKey().equals(SearchKey.NR_OF_LATEST_REPORTS))
                    .map(ListCriteria::getValue)
                    .findFirst();
            if (first.isPresent()) {
                numberOfLatestReports = Integer.parseInt(first.get());
            } else {
                throw new IllegalArgumentException(SearchKey.NR_OF_LATEST_REPORTS.name()
                        + " is in the query but no value could be found!, VALUE = null");
            }
        }
        return numberOfLatestReports;
    }

    private void getMovementsByConnectedIds(Integer numberOfLatestReports, List<SearchValue> searchKeys,
                                            List<Movement> movementEntityList, List<SearchValue> connectedIdsFromSearchKeyValues) {

        String sql;
        List<SearchValue> searchValuesWithoutConnectedIds = removeConnectedIdsFromSearchKeyValues(searchKeys);
        for (SearchValue connectedId : connectedIdsFromSearchKeyValues) {
            List<SearchValue> searchValues = new ArrayList<>(searchValuesWithoutConnectedIds);
            searchValues.add(connectedId);
            sql = SearchFieldMapper.createSelectSearchSql(searchValues, true);
            movementEntityList.addAll(movementDao.getMovementList(sql, searchKeys, numberOfLatestReports));
        }
    }

    private List<SearchValue> removeConnectedIdsFromSearchKeyValues(List<SearchValue> searchKeyValues){
        return searchKeyValues
                .stream()
                .filter(searchValue -> !(SearchField.CONNECT_ID.getFieldName().equals(searchValue.getField().getFieldName())))
                .collect(Collectors.toList());
    }

    private List<SearchValue> getConnectedIdsFromSearchKeyValues(List<SearchValue> searchKeyValues){
        return searchKeyValues.stream()
                .filter(searchValue -> SearchField.CONNECT_ID.getFieldName().equals(searchValue.getField().getFieldName()))
                .collect(Collectors.toList());
    }

    /**
     * This method removes track mismatches. These can occur during movement creation but are easier to remove on
     * read than write.
     *
     * In the rare event of segments that are attached to two different tracks, the track that is not
     connected to the any relevant Movement should be removed from the input list.
     * @param tracks list of tracks to purge
     * @param movements list of movements to look for correct tracks in
     */
    public void removeTrackMismatches(List<MovementTrack> tracks, List<Movement> movements) {
        if(tracks == null || movements == null) {
            throw new IllegalArgumentException("MovementTrack list or Movement list is null");
        }
        Set<String> trackIds = movements.stream()
                .filter(movement -> movement.getTrack() != null)
                .map(movement -> movement.getTrack().getId().toString())
                .collect(Collectors.toSet());

        Set<MovementTrack> tracksToSave = tracks.stream()
                .filter(track -> trackIds.contains(track.getId()))
                .collect(Collectors.toSet());

        tracks.removeIf(track -> !tracksToSave.contains(track));
    }

    public List<MovementSegment> filterSegments(List<MovementSegment> movementSegments, List<SearchValue> searchKeyValuesRange) {
        Set<MovementSegment> segments = new HashSet<>();
        if (movementSegments != null) {
            segments = movementSegments.stream()
                    .filter(segment -> keepSegment(segment, searchKeyValuesRange))
                    .collect(Collectors.toSet());
        }
        return new ArrayList<>(segments);
    }

    public boolean keepSegment(MovementSegment segment, List<SearchValue> searchKeyValuesRange) {

        if (segment == null || searchKeyValuesRange == null) {
            throw new IllegalArgumentException("MovementSegment or SearchValue list is null");
        }

        for (SearchValue searchValue : searchKeyValuesRange) {

            if (searchValue.isRange() && searchValue.getField().equals(SearchField.SEGMENT_DURATION)) {
                if (segment.getDuration() < Double.valueOf(searchValue.getFromValue())) {
                    return false;
                }
                if (segment.getDuration() > Double.valueOf(searchValue.getToValue())) {
                    return false;
                }
            }

            if (searchValue.isRange() && searchValue.getField().equals(SearchField.SEGMENT_LENGTH)) {
                if (segment.getDistance() < Double.valueOf(searchValue.getFromValue())) {
                    return false;
                }
                if (segment.getDistance() > Double.valueOf(searchValue.getToValue())) {
                    return false;
                }
            }

            if (searchValue.isRange() && searchValue.getField().equals(SearchField.SEGMENT_SPEED)) {
                if (segment.getSpeedOverGround() < Double.valueOf(searchValue.getFromValue())) {
                    return false;
                }
                if (segment.getSpeedOverGround() > Double.valueOf(searchValue.getToValue())) {
                    return false;
                }
            }
        }
        return true;
    }
}
