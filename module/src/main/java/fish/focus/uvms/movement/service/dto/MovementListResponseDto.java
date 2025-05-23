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
package fish.focus.uvms.movement.service.dto;

import fish.focus.uvms.movement.model.dto.MovementDto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.math.BigInteger;
import java.util.List;

/**
 *
 **/
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "movementQueryListDto")
public class MovementListResponseDto {

    @XmlElement(required = true)
    private List<MovementDto> movement;
    @XmlElement(required = true)
    private BigInteger totalNumberOfPages;
    @XmlElement(required = true)
    private BigInteger currentPage;

    public List<MovementDto> getMovement() {
        return movement;
    }

    public void setMovement(List<MovementDto> movement) {
        this.movement = movement;
    }

    public BigInteger getTotalNumberOfPages() {
        return totalNumberOfPages;
    }

    public void setTotalNumberOfPages(BigInteger totalNumberOfPages) {
        this.totalNumberOfPages = totalNumberOfPages;
    }

    public BigInteger getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(BigInteger currentPage) {
        this.currentPage = currentPage;
    }

}