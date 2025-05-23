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


import fish.focus.uvms.movement.service.entity.alarm.AlarmReport;

import java.io.Serializable;
import java.util.List;

public class AlarmListResponseDto implements Serializable {

    private static final long serialVersionUID = 1;

    private List<AlarmReport> alarmList;
    private int totalNumberOfPages;
    private int currentPage;

    public List<AlarmReport> getAlarmList() {
        return alarmList;
    }

    public void setAlarmList(List<AlarmReport> alarmList) {
        this.alarmList = alarmList;
    }

    public int getTotalNumberOfPages() {
        return totalNumberOfPages;
    }

    public void setTotalNumberOfPages(int totalNumberOfPages) {
        this.totalNumberOfPages = totalNumberOfPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

}