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
package fish.focus.uvms.movement.service.validation;

import fish.focus.schema.movement.search.v1.GroupListCriteria;
import fish.focus.schema.movement.search.v1.MovementSearchGroup;
import fish.focus.schema.movement.search.v1.SearchKey;
import fish.focus.schema.movement.search.v1.SearchKeyType;

import java.util.Arrays;
import java.util.stream.Collectors;


public class MovementGroupValidatorBean {

    public static final String ALLOWED_FIELD_VALUES = getAllowedValues();

    public static boolean isMovementGroupOk(MovementSearchGroup searchGroup) {
        if (searchGroup.getSearchFields().size() == 0) {
            return false;
        }
        for (GroupListCriteria field : searchGroup.getSearchFields()) {
            if (field.getType().equals(SearchKeyType.MOVEMENT)) {
                if (!containsCorrectMovementField(field.getKey())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check to see if the correct key value is present for MOVEMENT
     *
     * @param key
     * @return
     */
    private static boolean containsCorrectMovementField(String key) {
        for (SearchKey type : SearchKey.values()) {
            if (key.equalsIgnoreCase(type.name())) {
                return true;
            }
        }
        return false;
    }

    private static String getAllowedValues() {
        return Arrays
                .stream(SearchKey.values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }
}
