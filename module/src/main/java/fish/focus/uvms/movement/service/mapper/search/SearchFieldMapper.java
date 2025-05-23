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
package fish.focus.uvms.movement.service.mapper.search;

import fish.focus.schema.movement.search.v1.ListCriteria;
import fish.focus.schema.movement.search.v1.RangeCriteria;
import fish.focus.schema.movement.search.v1.SearchKey;
import fish.focus.schema.movement.v1.MovementActivityTypeType;
import fish.focus.schema.movement.v1.MovementSourceType;
import fish.focus.schema.movement.v1.MovementTypeType;
import fish.focus.schema.movement.v1.SegmentCategoryType;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class SearchFieldMapper {

    private static final Logger LOG = LoggerFactory.getLogger(SearchFieldMapper.class);

    /**
     * Creates a search SQL based on the search fields
     *
     * @param searchFields
     * @param isDynamic
     * @return
     * @throws ParseException
     */
    public static String createSelectSearchSql(List<SearchValue> searchFields, boolean isDynamic) {
        StringBuilder selectBuffer = new StringBuilder();

        selectBuffer.append(createInitSearchSql(SearchTables.MOVEMENT));
        selectBuffer.append(createInitFromSearchSql(SearchTables.MOVEMENT));

        if (searchFields != null && !searchFields.isEmpty()) {
            selectBuffer.append(createSearchSql(searchFields, isDynamic, true, false));
        }

        selectBuffer
                .append(" ORDER BY ")
                .append(SearchTables.MOVEMENT.getTableAlias())
                .append(".")
                .append(SearchField.DATE.getFieldName())
                .append(" DESC ");
        return selectBuffer.toString();
    }

    /**
     * Creates a search SQL based on the search fields
     *
     * @param searchFields
     * @param isDynamic
     * @return
     * @throws ParseException
     */
    public static String createMinimalSelectSearchSql(List<SearchValue> searchFields, boolean isDynamic) {
        StringBuilder selectBuffer = new StringBuilder();

        selectBuffer.append(createInitSearchSql(SearchTables.MINIMAL_MOVEMENT));
        selectBuffer.append(createInitFromSearchSql(SearchTables.MINIMAL_MOVEMENT));

        if (searchFields != null && !searchFields.isEmpty()) {
            selectBuffer.append(createSearchSql(searchFields, isDynamic, true, true));
        }

        selectBuffer
                .append(" ORDER BY ")
                .append(SearchTables.MINIMAL_MOVEMENT.getTableAlias())
                .append(".")
                .append(SearchField.DATE.getFieldName())
                .append(" DESC ");
        LOG.debug("[ SEARCH SQL: ] " + selectBuffer.toString());
        return selectBuffer.toString();
    }

    /**
     * Helper method that returns the initial select with the table aliases
     *
     * @param tables
     * @return
     */
    public static String createInitSearchSql(SearchTables... tables) {
        StringBuilder selectBuffer = new StringBuilder("SELECT ");
        for (SearchTables table : tables) {
            selectBuffer.append(" ").append(table.getTableAlias()).append(",");
        }
        return (selectBuffer.toString().endsWith(",")) ? selectBuffer.substring(0, selectBuffer.lastIndexOf(",")) : selectBuffer.toString();
    }

    public static String createInitFromSearchSql(SearchTables... tables) {
        StringBuilder selectBuffer = new StringBuilder(" FROM");
        for (SearchTables table : tables) {
            selectBuffer.append(" ").append(table.getTableName()).append(" ").append(table.getTableAlias()).append(",");
        }
        return (selectBuffer.toString().endsWith(",")) ? selectBuffer.substring(0, selectBuffer.lastIndexOf(",")) : selectBuffer.toString();
    }

    /**
     * Creates a JPQL count query based on the search fields. This is used for
     * when paginating lists
     *
     * @param searchFields
     * @param isDynamic
     * @return
     * @throws ParseException
     */
    public static String createCountSearchSql(List<SearchValue> searchFields, boolean isDynamic) {
        StringBuilder countBuffer = new StringBuilder();
        countBuffer.append("SELECT COUNT( ").append(SearchTables.MOVEMENT.getTableAlias()).append(") FROM ")
                .append(SearchTables.MOVEMENT.getTableName())
                .append(" ")
                .append(SearchTables.MOVEMENT.getTableAlias())
                .append(" ");
        if (searchFields != null && !searchFields.isEmpty()) {
            countBuffer.append(createSearchSql(searchFields, isDynamic, false, false));
        }
        LOG.debug("[ COUNT SQL: ] " + countBuffer.toString());
        return countBuffer.toString();
    }

    /**
     * Creates the complete search SQL with joins and sets the values based on
     * the criterias
     *
     * @param criterias
     * @param dynamic
     * @return
     * @throws ParseException
     */
    private static String createSearchSql(List<SearchValue> criterias, boolean dynamic, boolean joinFetch, boolean minimal) {

        String OPERATOR = " OR ";
        if (dynamic) {
            OPERATOR = " AND ";
        }

        StringBuilder builder = new StringBuilder();

        HashMap<SearchField, List<SearchValue>> orderedValues = combineSearchFields(criterias);

        String joinStr = minimal ? buildMinimalJoin(orderedValues, joinFetch) : buildJoin(orderedValues, joinFetch);
        builder.append(joinStr);

        if (!orderedValues.isEmpty()) {

            builder.append(" WHERE ");

            boolean first = true;
            boolean containsSpecialConditions = checkValidSingleAttributes(orderedValues);

            for (Entry<SearchField, List<SearchValue>> criteria : orderedValues.entrySet()) {
                if (!isKeySpecialCondition(criteria.getKey())) {
                    first = createOperator(first, builder, OPERATOR);
                    createCriteria(criteria.getValue(), criteria.getKey(), builder);
                }
            }

            if (containsSpecialConditions) {
                builder.append(buildSpecialConditionSql(orderedValues, first, OPERATOR));
            }
        }
        return builder.toString();
    }

    private static boolean createOperator(boolean first, StringBuilder builder, String OPERATOR) {
        if (first) {
            first = false;
        } else {
            builder.append(OPERATOR);
        }
        return first;
    }

    /**
     * Creates the where condition. If the list has more than one value the
     * condition will be 'IN(value1, value2)' If the list has one value the
     * condition will be '= value'
     *
     * @param criteria
     * @param builder
     * @throws ParseException
     */
    private static void createCriteria(List<SearchValue> criteria, SearchFieldType field, StringBuilder builder) {

        if (criteria.size() == 1) {
            SearchValue searchValue = criteria.get(0);
            if (searchValue.isRange()) {
                if (isKeySpecialCondition(searchValue.getField())) {
                    builder.append(setValueAsType(searchValue, field));
                } else {
                    if (searchValue.getField().equals(SearchField.DATE)) {
                        builder.append(buildTableAliasname(field));
                        builder.append(setValueAsType(searchValue));
                    } else {
                        builder.append(setValueAsType(searchValue));
                    }
                }
            } else {
                if (searchValue.getField().getClazz().isAssignableFrom(Geometry.class)) {
                    builder
                            .append(setValueAsType(searchValue));
                } else {
                    builder
                            .append(buildTableAliasname(field))
                            .append(setValueAsType(searchValue));
                }
            }
        } else if (criteria.size() > 1) {
            builder
                    .append(buildInSqlStatement(criteria, field));
        }
    }

    /**
     * Build special occasion WHERE String
     */
    private static String buildSpecialConditionSql(HashMap<SearchField, List<SearchValue>> orderedValues, boolean first, String operator) {

        StringBuilder builder = new StringBuilder();

        if (orderedValues.containsKey(SearchField.SEGMENT_ID)) {
            first = createOperator(first, builder, operator);
            builder.append(" ( ");
            createCriteria(orderedValues.get(SearchField.SEGMENT_ID), SearchFieldSpecial.TO_SEGMENT_ID, builder);
            builder.append(" OR ");
            createCriteria(orderedValues.get(SearchField.SEGMENT_ID), SearchFieldSpecial.FROM_SEGMENT_ID, builder);
            builder.append(" ) ");
        }
        if (orderedValues.containsKey(SearchField.CATEGORY)) {
            first = createOperator(first, builder, operator);
            builder.append(" ( ");
            createCriteria(orderedValues.get(SearchField.CATEGORY), SearchFieldSpecial.TO_SEGMENT_CATEGORY, builder);
            builder.append(" OR ");
            createCriteria(orderedValues.get(SearchField.CATEGORY), SearchFieldSpecial.FROM_SEGMENT_CATEGORY, builder);
            builder.append(" ) ");
        }
        if (orderedValues.containsKey(SearchField.SEGMENT_SPEED)) {
            first = createOperator(first, builder, operator);
            builder.append(" ( ");
            createCriteria(orderedValues.get(SearchField.SEGMENT_SPEED), SearchFieldSpecial.TO_SEGMENT_SPEED, builder);
            builder.append(" OR ");
            createCriteria(orderedValues.get(SearchField.SEGMENT_SPEED), SearchFieldSpecial.FROM_SEGMENT_SPEED, builder);
            builder.append(" ) ");
        }
        if (orderedValues.containsKey(SearchField.SEGMENT_DURATION)) {
            first = createOperator(first, builder, operator);
            builder.append(" ( ");
            createCriteria(orderedValues.get(SearchField.SEGMENT_DURATION), SearchFieldSpecial.TO_SEGMENT_DURATION, builder);
            builder.append(" OR ");
            createCriteria(orderedValues.get(SearchField.SEGMENT_DURATION), SearchFieldSpecial.FROM_SEGMENT_DURATION, builder);
            builder.append(" ) ");
        }
        if (orderedValues.containsKey(SearchField.SEGMENT_LENGTH)) {
            first = createOperator(first, builder, operator);
            builder.append(" ( ");
            createCriteria(orderedValues.get(SearchField.SEGMENT_LENGTH), SearchFieldSpecial.TO_SEGMENT_LENGTH, builder);
            builder.append(" OR ");
            createCriteria(orderedValues.get(SearchField.SEGMENT_LENGTH), SearchFieldSpecial.FROM_SEGMENT_LENGTH, builder);
            builder.append(" ) ");
        }
        return builder.toString();
    }

    /**
     * Checks if the search key should be handled as a special occasion string
     */
    private static boolean isKeySpecialCondition(SearchField field) {
        return field.equals(SearchField.CATEGORY)
                || field.equals(SearchField.SEGMENT_ID)
                || field.equals(SearchField.SEGMENT_DURATION)
                || field.equals(SearchField.SEGMENT_LENGTH)
                || field.equals(SearchField.SEGMENT_SPEED);
    }

    /**
     * Helper method to check search fields that must be handled specially
     * exists in the search criteria
     */
    private static boolean checkValidSingleAttributes(HashMap<SearchField, List<SearchValue>> orderedValues) {
        return orderedValues.containsKey(SearchField.CATEGORY)
                || orderedValues.containsKey(SearchField.SEGMENT_ID)
                || orderedValues.containsKey(SearchField.SEGMENT_LENGTH)
                || orderedValues.containsKey(SearchField.SEGMENT_SPEED)
                || orderedValues.containsKey(SearchField.SEGMENT_DURATION);
    }

    /**
     * Created the Join statement based on the join type. The resulting String
     * can be:
     * <p>
     * JOIN LEFT JOIN JOIN FETCH ( based on fetch )
     *
     * @param fetch create a JOIN FETCH or plain JOIN
     * @param type
     * @return
     */
    private static String getJoin(boolean fetch, JoinType type) {
        StringBuilder builder = new StringBuilder();
        builder.append(" ").append(type.name()).append(" ");
        builder.append("JOIN ");
        if (fetch) {
            builder.append("FETCH ");
        }
        return builder.toString();
    }

    /**
     * Builds JPA joins based on the search criterias provided. In some cases
     * there is no need for a joind and the JQL query runs faster
     *
     * @param orderedValues
     * @param fetch
     * @return
     */
    // TODO: There are some typos in the return value. Check if these has impact on expected result.
    private static String buildJoin(HashMap<SearchField, List<SearchValue>> orderedValues, boolean fetch) {
        return getJoin(fetch, JoinType.INNER) + SearchTables.MOVEMENT.getTableAlias() + "." + "movementConnect " + SearchTables.MOVEMENT_CONNECT.getTableAlias() + " " +
                getJoin(fetch, JoinType.LEFT) + SearchTables.MOVEMENT.getTableAlias() + "." + "track " + SearchTables.TRACK.getTableAlias() + " ";
    }

    /**
     * Builds JPA joins based on the search criterias provided. In some cases
     * there is no need for a joind and the JQL query runs faster
     *
     * @param orderedValues
     * @param fetch
     * @return
     */
    private static String buildMinimalJoin(HashMap<SearchField, List<SearchValue>> orderedValues, boolean fetch) {
        return getJoin(fetch, JoinType.INNER) + SearchTables.MOVEMENT.getTableAlias() + "." + "movementConnect " + SearchTables.MOVEMENT_CONNECT.getTableAlias() + " ";
    }

    /**
     * Creates at String that sets values based on what class the SearchValue
     * has. A String class returns [ = 'value' ] A Integer returns [ = value ]
     * Date is specificaly handled and can return [ >= 'datavalue' ] or [ <=
     * 'datavalue' ]
     * <p>
     * Also the special occation MIN_SPEED and MAX_SPEED is handled in the same
     * way as dates
     *
     * @param entry
     * @return
     */
    private static String setValueAsType(SearchValue entry) {
        StringBuilder builder = new StringBuilder();

        Class clazz = entry.getField().getClazz();

        if (entry.isRange()) {
            if (clazz.isAssignableFrom(Instant.class)) {
                builder.append(" BETWEEN ").append(":fromDate ").append(" AND ").append(":toDate ");
            } else if (clazz.isAssignableFrom(Double.class) || clazz.isAssignableFrom(Integer.class)) {
                builder.append(" ( ").append(buildTableAliasname(entry.getField()));
                builder.append(" >= ").append(entry.getFromValue());
                builder.append(" AND ");
                builder.append(buildTableAliasname(entry.getField()));
                builder.append(" <= ").append(entry.getToValue()).append(" ) ");
            } else {
                throw new IllegalArgumentException("Error when setting value as type: Only Date, Integer and Double are supported when the entry is a range query ( setValueAsType )");
            }
        } else {
            if (clazz.isEnum()) {
                builder.append(" = ").append(getOrdinalValueFromEnum(entry));
            } else if (clazz.isAssignableFrom(Geometry.class)) {
                builder.append(" WITHIN( ")
                        .append("geometry(").append(buildTableAliasname(entry.getField())).append(")")
                        .append(", ").append(":wkt").append(" ) = true ");
            } else {
                builder.append(" = ").append(buildValueFromClassType(entry));
            }
        }
        return builder.toString();
    }

    /**
     * Creates at String that sets values based on what class the SearchValue
     * has. A String class returns [ = 'value' ] A Integer returns [ = value ]
     * Date is specificaly handled and can return [ >= 'datavalue' ] or [ <=
     * 'datavalue' ]
     * <p>
     * Also the special occation MIN_SPEED and MAX_SPEED is handled in the same
     * way as dates
     *
     * @param entry
     * @return
     * @throws ParseException
     */
    private static String setValueAsType(SearchValue entry, SearchFieldType type) {
        StringBuilder builder = new StringBuilder();

        Class clazz = entry.getField().getClazz();

        if (entry.isRange()) {
            if (clazz.isAssignableFrom(Instant.class)) {
                builder.append(" BETWEEN ").append(":fromDate ").append(" AND ").append(":toDate ");
            } else if (clazz.isAssignableFrom(Double.class) || clazz.isAssignableFrom(Integer.class)) {
                builder
                        .append(" ( ").append(buildTableAliasname(type))
                        .append(" >= ").append(entry.getFromValue())
                        .append(" AND ")
                        .append(buildTableAliasname(type))
                        .append(" <= ").append(entry.getToValue()).append(" ) ");
            } else {
                throw new IllegalArgumentException("Error when setting value as type: Only Date, " +
                        "Integer and Double are supported when the entry is a range query ( setValueAsType )");
            }
        } else {

            if (entry.getField().getClazz().isEnum()) {
                builder.append(" = ").append(getOrdinalValueFromEnum(entry));
            }

            if (clazz.isAssignableFrom(Geometry.class)) {
                builder.append(" WITHIN( ")
                        .append(buildTableAliasname(entry.getField()))
                        .append(", ").append(":wkt").append(" ) = true ");
            }

            builder.append(" = ").append(buildValueFromClassType(entry));
        }

        return builder.toString();

    }

    /**
     * Get the Ordinal value from Enum matching PK id in type table
     *
     * @param value
     * @return
     */
    public static Integer getOrdinalValueFromEnum(SearchValue value) {
        if (value.getField().getClazz().isAssignableFrom(MovementTypeType.class)) {
            return MovementTypeType.fromValue(value.getValue()).ordinal();
        } else if (value.getField().getClazz().isAssignableFrom(MovementActivityTypeType.class)) {
            return MovementActivityTypeType.fromValue(value.getValue()).ordinal();
        } else if (value.getField().getClazz().isAssignableFrom(MovementSourceType.class)) {
            return MovementSourceType.fromValue(value.getValue()).ordinal();
        } else if (value.getField().getClazz().isAssignableFrom(SegmentCategoryType.class)) {
            return SegmentCategoryType.fromValue(value.getValue()).ordinal();
        }
        throw new IllegalArgumentException("Enum type not defined for mapping");
    }

    /**
     * Builds a table alias for the query based on the search field
     * <p>
     * EG [ theTableAlias.theColumnName ]
     *
     * @param field
     * @return
     */
    private static String buildTableAliasname(SearchFieldType field) {
        return field.getSearchTables().getTableAlias() + "." + field.getFieldName();
    }

    /**
     * Returns the representation of the value
     * <p>
     * if Integer [ value ] else [ 'value' ]
     *
     * @param entry
     * @return
     */
    private static String buildValueFromClassType(SearchValue entry) {
        StringBuilder builder = new StringBuilder();
        if (entry.getField().getClazz().isAssignableFrom(Integer.class)) {
            builder.append(entry.getValue());
        } else if (entry.getField().getClazz().isAssignableFrom(Double.class)) {
            builder.append(entry.getValue());
        } else if (entry.getField().getClazz().isEnum()) {
            builder.append(getOrdinalValueFromEnum(entry));
        } else {
            builder.append("'").append(entry.getValue()).append("'");
        }
        return builder.toString();
    }

    /**
     * Builds an IN JPQL representation for lists of values
     * <p>
     * The resulting String = [ mc.in IN ( 'ABC123', 'ABC321' ) ]
     *
     * @param searchValues
     * @param field
     * @return
     */
    private static String buildInSqlStatement(List<SearchValue> searchValues, SearchFieldType field) {
        StringBuilder builder = new StringBuilder();

        builder.append(buildTableAliasname(field));

        builder.append(" IN ( ");
        boolean first = true;
        for (SearchValue searchValue : searchValues) {
            if (first) {
                first = false;
                builder.append(buildValueFromClassType(searchValue));
            } else {
                builder.append(", ").append(buildValueFromClassType(searchValue));
            }
        }
        builder.append(" )");
        return builder.toString();
    }

    /**
     * Takes all the search values and categorizes them in lists to a key
     * according to the SearchField
     *
     * @param searchValues
     * @return
     */
    private static HashMap<SearchField, List<SearchValue>> combineSearchFields(List<SearchValue> searchValues) {
        HashMap<SearchField, List<SearchValue>> values = new HashMap<>();
        for (SearchValue search : searchValues) {
            if (values.containsKey(search.getField())) {
                values.get(search.getField()).add(search);
            } else {
                values.put(search.getField(), new ArrayList<>(Collections.singletonList(search)));
            }
        }
        return values;
    }

    /**
     * Converts List<ListCriteria> to List<SearchValue> so that a JPQL query can
     * be built based on the criteria
     *
     * @param listCriteria
     * @return
     */
    public static List<SearchValue> mapListCriteriaToSearchValue(List<ListCriteria> listCriteria) {

        if (listCriteria == null || listCriteria.isEmpty()) {
            LOG.debug(" Non valid search criteria when mapping ListCriteria to SearchValue, List is null or empty");
            return new ArrayList<>();
        }
        List<SearchValue> searchFields = new ArrayList<>();
        for (ListCriteria criteria : listCriteria) {
            try {
                SearchField field = mapCriteria(criteria.getKey());
                searchFields.add(new SearchValue(field, criteria.getValue()));
            } catch (IllegalArgumentException e) {
                LOG.debug("Error when mapping to search field.. continuing with other criterias. {}", e.getMessage());
            }
        }
        return searchFields;
    }

    /**
     * Converts List<RangeCriteria> to List<SearchValue> so that a JPQL query
     * can be built based on the criteria
     *
     * @param rangeCriteria
     * @return
     */
    public static List<SearchValue> mapRangeCriteriaToSearchField(List<RangeCriteria> rangeCriteria) {

        if (rangeCriteria == null || rangeCriteria.isEmpty()) {
            LOG.debug(" Non valid search criteria when mapping RangeCriteria to SearchValue, List is null or empty");
            return new ArrayList<>();
        }

        List<SearchValue> searchFields = new ArrayList<>();
        for (RangeCriteria criteria : rangeCriteria) {
            switch (criteria.getKey()) {
                case DATE:
                    searchFields.add(new SearchValue(SearchField.DATE, criteria.getFrom(), criteria.getTo()));
                    break;
                case MOVEMENT_SPEED:
                    searchFields.add(new SearchValue(SearchField.MOVEMENT_SPEED, criteria.getFrom(), criteria.getTo()));
                    break;
                case SEGMENT_SPEED:
                    searchFields.add(new SearchValue(SearchField.SEGMENT_SPEED, criteria.getFrom(), criteria.getTo()));
                    break;
                case TRACK_SPEED:
                    searchFields.add(new SearchValue(SearchField.TRACK_SPEED, criteria.getFrom(), criteria.getTo()));
                    break;
                case SEGMENT_DURATION:
                    searchFields.add(new SearchValue(SearchField.SEGMENT_DURATION, criteria.getFrom(), criteria.getTo()));
                    break;
                case SEGMENT_LENGTH:
                    searchFields.add(new SearchValue(SearchField.SEGMENT_LENGTH, criteria.getFrom(), criteria.getTo()));
                    break;
                case TRACK_DURATION:
                    searchFields.add(new SearchValue(SearchField.TRACK_DURATION, criteria.getFrom(), criteria.getTo()));
                    break;
                case TRACK_LENGTH:
                    searchFields.add(new SearchValue(SearchField.TRACK_LENGTH, criteria.getFrom(), criteria.getTo()));
                    break;
                case TRACK_DURATION_AT_SEA:
                    searchFields.add(new SearchValue(SearchField.TRACK_TOTAL_TIME_AT_SEA, criteria.getFrom(), criteria.getTo()));
                    break;
                default:
                    throw new AssertionError(criteria.getKey().name());
            }
        }
        return searchFields;
    }

    /**
     * Maps the Search Key to a SearchField. All SearchKeys that are not a part
     * of Movement are excluded
     *
     * @param key
     * @return
     */
    private static SearchField mapCriteria(SearchKey key) {
        switch (key) {
            case MOVEMENT_ID:
                return SearchField.MOVEMENT_ID;
            case SEGMENT_ID:
                return SearchField.SEGMENT_ID;
            case TRACK_ID:
                return SearchField.TRACK_ID;
            case CONNECT_ID:
                return SearchField.CONNECT_ID;
            case MOVEMENT_TYPE:
                return SearchField.MOVMENT_TYPE;
            case AREA:
                return SearchField.AREA;
            case STATUS:
                return SearchField.STATUS;
            case SOURCE:
                return SearchField.SOURCE;
            case CATEGORY:
                return SearchField.CATEGORY;
            case DATE:
                return SearchField.DATE;
            default:
                throw new IllegalArgumentException("No field found: " + key.name());
        }
    }

    /**
     * The supported JOIN types see method getJoin for more info
     */
    public enum JoinType {
        INNER,
        LEFT
    }
}
