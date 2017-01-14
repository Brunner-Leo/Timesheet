/*
 * Copyright 2016 Adrian Schnedlitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.catrobat.jira.timesheet.services.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.service.ServiceException;
import net.java.ao.schema.NotNull;
import org.catrobat.jira.timesheet.activeobjects.Timesheet;
import org.catrobat.jira.timesheet.activeobjects.TimesheetEntry;
import org.catrobat.jira.timesheet.services.TimesheetService;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class TimesheetServiceImpl implements TimesheetService {

    private final ActiveObjects ao;

    public TimesheetServiceImpl(ActiveObjects ao) {
        this.ao = ao;
    }


    @Override
    public Timesheet editTimesheet(String userKey, int targetHoursPractice, int targetHoursTheory,
            int targetHours, int targetHoursCompleted, int targetHoursRemoved,
            String lectures, String reason, Date latestEntryDate,
            boolean isActive, boolean isOffline, boolean isMasterThesisTimesheet, boolean isEnabled) throws ServiceException {
        Timesheet[] found = ao.find(Timesheet.class, "USER_KEY = ?", userKey);

        if (found.length > 2) {
            throw new ServiceException("Found more than two Timesheets with the same UserKey.");
        } else if (found.length == 0) {
            throw new ServiceException("Access denied. No 'Timesheet' found for this user.");
        }

        for (Timesheet aFound : found) {
            if (isMasterThesisTimesheet == aFound.getIsMasterThesisTimesheet()) {
                Timesheet sheet = aFound;

                sheet.setUserKey(userKey);
                sheet.setTargetHoursPractice(targetHoursPractice);
                sheet.setTargetHoursTheory(targetHoursTheory);
                sheet.setTargetHours(targetHours);
                sheet.setTargetHoursCompleted(targetHoursCompleted);
                sheet.setTargetHoursRemoved(targetHoursRemoved);
                sheet.setLectures(lectures);
                sheet.setReason(reason);
                sheet.setLatestEntryBeginDate(latestEntryDate);
                sheet.setIsActive(isActive);
                sheet.setIsOffline(isOffline);
                sheet.setIsEnabled(isEnabled);
                sheet.save();
                return sheet;
            }
        }

        return null;
    }

    @NotNull
    @Override
    public Timesheet add(String userKey, int targetHoursPractice, int targetHoursTheory,
            int targetHours, int targetHoursCompleted, int targetHoursRemoved,
            String lectures, String reason,
            boolean isActive, boolean isOffline, boolean isMasterThesisTimesheet, boolean isEnabled) {
        Timesheet sheet = ao.create(Timesheet.class);
        sheet.setUserKey(userKey);
        sheet.setTargetHoursPractice(targetHoursPractice);
        sheet.setTargetHoursTheory(targetHoursTheory);
        sheet.setTargetHours(targetHours);
        sheet.setTargetHoursCompleted(targetHoursCompleted);
        sheet.setTargetHoursRemoved(targetHoursRemoved);
        sheet.setLectures(lectures);
        sheet.setReason(reason);
        sheet.setLatestEntryBeginDate(new Date());
        sheet.setIsActive(isActive);
        sheet.setIsEnabled(isEnabled);
        sheet.setIsOffline(isOffline);
        sheet.setIsMasterThesisTimesheet(isMasterThesisTimesheet);
        sheet.save();
        return sheet;
    }

    @Override
    public void remove(Timesheet timesheet) throws ServiceException {
        int id = timesheet.getID();
        Timesheet[] found = ao.find(Timesheet.class, "ID = ?", id);

        if (found.length > 1) {
            throw new ServiceException("Found more than one timesheet with the same id.");
        }

        for (TimesheetEntry entry : found[0].getEntries()) {
            ao.delete(entry);
        }

        ao.delete(found[0]);
    }

    @NotNull
    @Override
    public List<Timesheet> all() {
        return newArrayList(ao.find(Timesheet.class));
    }

    @Nullable
    @Override
    public Timesheet updateTimesheetEnableState(int timesheetID, Boolean isEnabled) throws ServiceException {
        Timesheet[] found = ao.find(Timesheet.class, "ID = ?", timesheetID);
        if ((found.length == 1)) {
            Timesheet sheet = found[0];
            sheet.setIsEnabled(isEnabled);
            sheet.save();
            return sheet;
        } else {
            throw new ServiceException("Access denied. No 'Timesheet' found for this user.");
        }
    }

    @Override
    public Timesheet getTimesheetByUser(String userKey, Boolean isMasterThesisTimesheet) throws ServiceException {
        Timesheet[] found = ao.find(Timesheet.class, "USER_KEY = ?", userKey);
        if (found.length > 2) {
            throw new ServiceException("Found more than two Timesheets with the same UserKey.");
        }

        if (isMasterThesisTimesheet) {
            for (int i = 0; i < found.length; i++) {
                if (found[i].getIsMasterThesisTimesheet()) {
                    return found[i];
                }
            }
        } else {
            for (int i = 0; i < found.length; i++) {
                if (!found[i].getIsMasterThesisTimesheet()) {
                    return found[i];
                }
            }
        }

        System.out.println("found.length = " + found.length);
        throw new ServiceException("No Timesheet found. Maybe user does not have one.");
    }

    @Override
    public Boolean userHasTimesheet(String userKey, Boolean isMasterThesisTimesheet) throws ServiceException {
        Timesheet[] found = ao.find(Timesheet.class, "USER_KEY = ?", userKey);

        if (found.length > 2) {
            if (found != null) {
                ao.delete(found);
            }
            throw new ServiceException("Found more than two Timesheets with the same UserKey. All timesheets will be deleted.");
        }

        if (isMasterThesisTimesheet) {
            for (int i = 0; i < found.length; i++) {
                if (found[i].getIsMasterThesisTimesheet()) {
                    return true;
                }
            }
        } else {
            for (int i = 0; i < found.length; i++) {
                if (!found[i].getIsMasterThesisTimesheet()) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public Timesheet getAdministratorTimesheet(String userKey) throws ServiceException {
        Timesheet[] found = ao.find(Timesheet.class, "USER_KEY = ?", userKey);

        if (found.length > 1) {
            throw new ServiceException("Found more than two Timesheets with the same UserKey.");
        } else if (found.length == 0) {
            return null;
        } else {
            return found[0];
        }
    }

    @Override
    public Timesheet getTimesheetImport(String userKey) {
        Timesheet[] found = ao.find(Timesheet.class, "USER_KEY = ?", userKey);

        return found.length > 0 ? found[0] : null;
    }

    @Override
    public Timesheet getTimesheetByID(int id) throws ServiceException {
        Timesheet[] found = ao.find(Timesheet.class, "ID = ?", id);

        if (found.length > 1) {
            throw new ServiceException("Multiple Timesheets with the same ID.");
        }
        return (found.length > 0) ? found[0] : null;
    }
}
