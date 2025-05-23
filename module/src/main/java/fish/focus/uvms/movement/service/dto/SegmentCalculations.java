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

/**
 *
 **/
public class SegmentCalculations {

    private double avgSpeed;
    private double durationBetweenPoints;
    private double distanceBetweenPoints;
    private double course;

    public double getAvgSpeed() {
        return avgSpeed;
    }

    public void setAvgSpeed(double avgSpeed) {
        this.avgSpeed = avgSpeed;
    }

    public double getDurationBetweenPoints() {
        return durationBetweenPoints;
    }

    public void setDurationBetweenPoints(double durationBetweenPoints) {
        this.durationBetweenPoints = durationBetweenPoints;
    }

    public double getDistanceBetweenPoints() {
        return distanceBetweenPoints;
    }

    public void setDistanceBetweenPoints(double distanceBetweenPoints) {
        this.distanceBetweenPoints = distanceBetweenPoints;
    }

    public double getCourse() {
        return course;
    }

    public void setCourse(double course) {
        this.course = course;
    }

}
