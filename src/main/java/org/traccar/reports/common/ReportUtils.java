/*
 * Copyright 2016 - 2023 Anton Tananaev (anton@traccar.org)
 * Copyright 2016 - 2017 Andrey Kunitsyn (andrey@traccar.org)
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
package org.traccar.reports.common;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.jxls.area.Area;
import org.jxls.builder.xls.XlsCommentAreaBuilder;
import org.jxls.common.CellRef;
import org.jxls.formula.StandardFormulaProcessor;
import org.jxls.transform.Transformer;
import org.jxls.transform.poi.PoiTransformer;
import org.jxls.util.TransformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.api.security.PermissionsService;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.geocoder.Geocoder;
import org.traccar.helper.UnitsConverter;
import org.traccar.helper.model.AttributeUtil;
import org.traccar.helper.model.PositionUtil;
import org.traccar.helper.model.UserUtil;
import org.traccar.model.BaseModel;
import org.traccar.model.Device;
import org.traccar.model.Driver;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.reports.model.BaseReportItem;
import org.traccar.reports.model.GeofenceReportItem;
import org.traccar.reports.model.StopReportItem;
import org.traccar.reports.model.TripReportItem;
import org.traccar.session.state.MotionProcessor;
import org.traccar.session.state.MotionState;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class ReportUtils {
    private final Config config;
    private final Storage storage;
    private final PermissionsService permissionsService;
    private final VelocityEngine velocityEngine;
    private final Geocoder geocoder;

    @Inject
    public ReportUtils(
            Config config, Storage storage, PermissionsService permissionsService,
            VelocityEngine velocityEngine, @Nullable Geocoder geocoder) {
        this.config = config;
        this.storage = storage;
        this.permissionsService = permissionsService;
        this.velocityEngine = velocityEngine;
        this.geocoder = geocoder;
    }

    public <T extends BaseModel> T getObject(
            long userId, Class<T> clazz, long objectId) throws StorageException, SecurityException {
        return storage.getObject(clazz, new Request(
                new Columns.All(),
                new Condition.And(
                        new Condition.Equals("id", objectId),
                        new Condition.Permission(User.class, userId, clazz))));
    }

    public void checkPeriodLimit(Date from, Date to) {
        long limit = config.getLong(Keys.REPORT_PERIOD_LIMIT) * 1000;
        if (limit > 0 && to.getTime() - from.getTime() > limit) {
            throw new IllegalArgumentException("Time period exceeds the limit");
        }
    }

    public double calculateFuel(Position first, Position last) {
        if (first.hasAttribute(Position.KEY_FUEL_USED) && last.hasAttribute(Position.KEY_FUEL_USED)) {
            return last.getDouble(Position.KEY_FUEL_USED) - first.getDouble(Position.KEY_FUEL_USED);
        } else if (first.hasAttribute(Position.KEY_FUEL_LEVEL) && last.hasAttribute(Position.KEY_FUEL_LEVEL)) {
            return first.getDouble(Position.KEY_FUEL_LEVEL) - last.getDouble(Position.KEY_FUEL_LEVEL);
        }
        return 0;
    }

    public String findDriver(Position firstPosition, Position lastPosition) {
        if (firstPosition.hasAttribute(Position.KEY_DRIVER_UNIQUE_ID)) {
            return firstPosition.getString(Position.KEY_DRIVER_UNIQUE_ID);
        } else if (lastPosition.hasAttribute(Position.KEY_DRIVER_UNIQUE_ID)) {
            return lastPosition.getString(Position.KEY_DRIVER_UNIQUE_ID);
        }
        return null;
    }

    public String findDriverName(String driverUniqueId) throws StorageException {
        if (driverUniqueId != null) {
            Driver driver = storage.getObject(Driver.class, new Request(
                    new Columns.All(),
                    new Condition.Equals("uniqueId", driverUniqueId)));
            if (driver != null) {
                return driver.getName();
            }
        }
        return null;
    }

    public org.jxls.common.Context initializeContext(long userId) throws StorageException {
        var server = permissionsService.getServer();
        var user = permissionsService.getUser(userId);
        var context = PoiTransformer.createInitialContext();
        context.putVar("distanceUnit", UserUtil.getDistanceUnit(server, user));
        context.putVar("speedUnit", UserUtil.getSpeedUnit(server, user));
        context.putVar("volumeUnit", UserUtil.getVolumeUnit(server, user));
        context.putVar("webUrl", velocityEngine.getProperty("web.url"));
        context.putVar("dateTool", new DateTool());
        context.putVar("numberTool", new NumberTool());
        context.putVar("timezone", UserUtil.getTimezone(server, user));
        context.putVar("locale", Locale.getDefault());
        context.putVar("bracketsRegex", "[\\{\\}\"]");
        return context;
    }

    public void processTemplateWithSheets(
            InputStream templateStream, OutputStream targetStream, org.jxls.common.Context context) throws IOException {

        Transformer transformer = TransformerFactory.createTransformer(templateStream, targetStream);
        List<Area> xlsAreas = new XlsCommentAreaBuilder(transformer).build();
        for (Area xlsArea : xlsAreas) {
            xlsArea.applyAt(new CellRef(xlsArea.getStartCellRef().getCellName()), context);
            xlsArea.setFormulaProcessor(new StandardFormulaProcessor());
            xlsArea.processFormulas();
        }
        transformer.deleteSheet(xlsAreas.get(0).getStartCellRef().getSheetName());
        transformer.write();
    }

    private TripReportItem calculateTrip(
            Device device, Position startTrip, Position endTrip, double maxSpeed,
            boolean ignoreOdometer) throws StorageException {

        TripReportItem trip = new TripReportItem();

        long tripDuration = endTrip.getFixTime().getTime() - startTrip.getFixTime().getTime();
        long deviceId = startTrip.getDeviceId();
        trip.setDeviceId(deviceId);
        trip.setDeviceName(device.getName());

        trip.setStartPositionId(startTrip.getId());
        trip.setStartLat(startTrip.getLatitude());
        trip.setStartLon(startTrip.getLongitude());
        trip.setStartTime(startTrip.getFixTime());
        String startAddress = startTrip.getAddress();
        if (startAddress == null && geocoder != null && config.getBoolean(Keys.GEOCODER_ON_REQUEST)) {
            startAddress = geocoder.getAddress(startTrip.getLatitude(), startTrip.getLongitude(), null);
        }
        trip.setStartAddress(startAddress);

        trip.setEndPositionId(endTrip.getId());
        trip.setEndLat(endTrip.getLatitude());
        trip.setEndLon(endTrip.getLongitude());
        trip.setEndTime(endTrip.getFixTime());
        String endAddress = endTrip.getAddress();
        if (endAddress == null && geocoder != null && config.getBoolean(Keys.GEOCODER_ON_REQUEST)) {
            endAddress = geocoder.getAddress(endTrip.getLatitude(), endTrip.getLongitude(), null);
        }
        trip.setEndAddress(endAddress);

        trip.setDistance(PositionUtil.calculateDistance(startTrip, endTrip, !ignoreOdometer));
        trip.setDuration(tripDuration);
        if (tripDuration > 0) {
            trip.setAverageSpeed(UnitsConverter.knotsFromMps(trip.getDistance() * 1000 / tripDuration));
        }
        trip.setMaxSpeed(maxSpeed);
        trip.setSpentFuel(calculateFuel(startTrip, endTrip));

        trip.setDriverUniqueId(findDriver(startTrip, endTrip));
        trip.setDriverName(findDriverName(trip.getDriverUniqueId()));

        if (!ignoreOdometer
                && startTrip.getDouble(Position.KEY_ODOMETER) != 0
                && endTrip.getDouble(Position.KEY_ODOMETER) != 0) {
            trip.setStartOdometer(startTrip.getDouble(Position.KEY_ODOMETER));
            trip.setEndOdometer(endTrip.getDouble(Position.KEY_ODOMETER));
        } else {
            trip.setStartOdometer(startTrip.getDouble(Position.KEY_TOTAL_DISTANCE));
            trip.setEndOdometer(endTrip.getDouble(Position.KEY_TOTAL_DISTANCE));
        }

        return trip;
    }

    private StopReportItem calculateStop(
            Device device, Position startStop, Position endStop, boolean ignoreOdometer) {

        StopReportItem stop = new StopReportItem();

        long deviceId = startStop.getDeviceId();
        stop.setDeviceId(deviceId);
        stop.setDeviceName(device.getName());

        stop.setPositionId(startStop.getId());
        stop.setLatitude(startStop.getLatitude());
        stop.setLongitude(startStop.getLongitude());
        stop.setStartTime(startStop.getFixTime());
        String address = startStop.getAddress();
        if (address == null && geocoder != null && config.getBoolean(Keys.GEOCODER_ON_REQUEST)) {
            address = geocoder.getAddress(stop.getLatitude(), stop.getLongitude(), null);
        }
        stop.setAddress(address);

        stop.setEndTime(endStop.getFixTime());

        long stopDuration = endStop.getFixTime().getTime() - startStop.getFixTime().getTime();
        stop.setDuration(stopDuration);
        stop.setSpentFuel(calculateFuel(startStop, endStop));

        if (startStop.hasAttribute(Position.KEY_HOURS) && endStop.hasAttribute(Position.KEY_HOURS)) {
            stop.setEngineHours(endStop.getLong(Position.KEY_HOURS) - startStop.getLong(Position.KEY_HOURS));
        }

        if (!ignoreOdometer
                && startStop.getDouble(Position.KEY_ODOMETER) != 0
                && endStop.getDouble(Position.KEY_ODOMETER) != 0) {
            stop.setStartOdometer(startStop.getDouble(Position.KEY_ODOMETER));
            stop.setEndOdometer(endStop.getDouble(Position.KEY_ODOMETER));
        } else {
            stop.setStartOdometer(startStop.getDouble(Position.KEY_TOTAL_DISTANCE));
            stop.setEndOdometer(endStop.getDouble(Position.KEY_TOTAL_DISTANCE));
        }

        return stop;

    }

    @SuppressWarnings("unchecked")
    private <T extends BaseReportItem> T calculateTripOrStop(
            Device device, Position startPosition, Position endPosition, double maxSpeed,
            boolean ignoreOdometer, Class<T> reportClass) throws StorageException {

        if (reportClass.equals(TripReportItem.class)) {
            return (T) calculateTrip(device, startPosition, endPosition, maxSpeed, ignoreOdometer);
        } else {
            return (T) calculateStop(device, startPosition, endPosition, ignoreOdometer);
        }
    }

    private boolean isMoving(List<Position> positions, int index, TripsConfig tripsConfig) {
        if (tripsConfig.getMinimalNoDataDuration() > 0) {
            boolean beforeGap = index < positions.size() - 1
                    && positions.get(index + 1).getFixTime().getTime() - positions.get(index).getFixTime().getTime()
                    >= tripsConfig.getMinimalNoDataDuration();
            boolean afterGap = index > 0
                    && positions.get(index).getFixTime().getTime() - positions.get(index - 1).getFixTime().getTime()
                    >= tripsConfig.getMinimalNoDataDuration();
            if (beforeGap || afterGap) {
                return false;
            }
        }
        return positions.get(index).getBoolean(Position.KEY_MOTION);
    }

    public <T extends BaseReportItem> List<T> detectTripsAndStops(
            Device device, Date from, Date to, Class<T> reportClass) throws StorageException {

        long threshold = config.getLong(Keys.REPORT_FAST_THRESHOLD);
        if (Duration.between(from.toInstant(), to.toInstant()).toSeconds() > threshold) {
            return fastTripsAndStops(device, from, to, reportClass);
        } else {
            return slowTripsAndStops(device, from, to, reportClass);
        }
    }

    public <T extends BaseReportItem> List<T> slowTripsAndStops(
            Device device, Date from, Date to, Class<T> reportClass) throws StorageException {

        List<T> result = new ArrayList<>();
        TripsConfig tripsConfig = new TripsConfig(
                new AttributeUtil.StorageProvider(config, storage, permissionsService, device));
        boolean ignoreOdometer = tripsConfig.getIgnoreOdometer();

        var positions = PositionUtil.getPositions(storage, device.getId(), from, to);
        if (!positions.isEmpty()) {
            boolean trips = reportClass.equals(TripReportItem.class);

            MotionState motionState = new MotionState();
            boolean initialValue = isMoving(positions, 0, tripsConfig);
            motionState.setMotionStreak(initialValue);
            motionState.setMotionState(initialValue);

            boolean detected = trips == motionState.getMotionState();
            double maxSpeed = 0;
            int startEventIndex = detected ? 0 : -1;
            int startNoEventIndex = -1;
            for (int i = 0; i < positions.size(); i++) {
                boolean motion = isMoving(positions, i, tripsConfig);
                if (motionState.getMotionState() != motion) {
                    if (motion == trips) {
                        if (!detected) {
                            startEventIndex = i;
                            maxSpeed = positions.get(i).getSpeed();
                        }
                        startNoEventIndex = -1;
                    } else {
                        startNoEventIndex = i;
                    }
                } else {
                    maxSpeed = Math.max(maxSpeed, positions.get(i).getSpeed());
                }

                MotionProcessor.updateState(motionState, positions.get(i), motion, tripsConfig);
                if (motionState.getEvent() != null) {
                    if (motion == trips) {
                        detected = true;
                        startNoEventIndex = -1;
                    } else if (startEventIndex >= 0 && startNoEventIndex >= 0) {
                        result.add(calculateTripOrStop(
                                device, positions.get(startEventIndex), positions.get(startNoEventIndex),
                                maxSpeed, ignoreOdometer, reportClass));
                        detected = false;
                        startEventIndex = -1;
                        startNoEventIndex = -1;
                    }
                }
            }
            if (detected & startEventIndex >= 0 && startEventIndex < positions.size() - 1) {
                int endIndex = startNoEventIndex >= 0 ? startNoEventIndex : positions.size() - 1;
                result.add(calculateTripOrStop(
                        device, positions.get(startEventIndex), positions.get(endIndex),
                        maxSpeed, ignoreOdometer, reportClass));
            }
        }

        return result;
    }

    public <T extends BaseReportItem> List<T> fastTripsAndStops(
            Device device, Date from, Date to, Class<T> reportClass) throws StorageException {

        List<T> result = new ArrayList<>();
        TripsConfig tripsConfig = new TripsConfig(
                new AttributeUtil.StorageProvider(config, storage, permissionsService, device));
        boolean ignoreOdometer = tripsConfig.getIgnoreOdometer();
        boolean trips = reportClass.equals(TripReportItem.class);
        Set<String> filter = Set.of(Event.TYPE_DEVICE_MOVING, Event.TYPE_DEVICE_STOPPED);

        var events = storage.getObjects(Event.class, new Request(
                new Columns.All(),
                new Condition.And(
                        new Condition.Equals("deviceId", device.getId()),
                        new Condition.Between("eventTime", "from", from, "to", to)),
                new Order("eventTime")));
        var filteredEvents = events.stream()
                .filter(event -> filter.contains(event.getType()))
                .collect(Collectors.toList());

        Event startEvent = null;
        for (Event event : filteredEvents) {
            boolean motion = event.getType().equals(Event.TYPE_DEVICE_MOVING);
            if (motion == trips) {
                startEvent = event;
            } else if (startEvent != null) {
                Position startPosition = storage.getObject(Position.class, new Request(
                        new Columns.All(), new Condition.Equals("id", startEvent.getPositionId())));
                Position endPosition = storage.getObject(Position.class, new Request(
                        new Columns.All(), new Condition.Equals("id", event.getPositionId())));
                if (startPosition != null && endPosition != null) {
                    result.add(calculateTripOrStop(
                            device, startPosition, endPosition, 0, ignoreOdometer, reportClass));
                }
                startEvent = null;
            }
        }

        return result;
    }

    public List<GeofenceReportItem> getGeofenceReport(long geofenceId, Collection<Long> deviceIds, Date from, Date to) {
        List<GeofenceReportItem> result = new ArrayList<>();

        try {
            List<Condition> conditions = new ArrayList<>();
            conditions.add(new Condition.Equals("geofenceId", geofenceId));
            conditions.add(new Condition.Between("eventTime", "from", from, "to", to));

            var events = storage.getObjects(Event.class, new Request(
                    new Columns.All(),
                    Condition.merge(conditions),
                    new Order("eventTime")));

            for (Event event : events) {
                if (shouldProcessEvent(event, deviceIds)) {
                    processEvent(event, result, geofenceId);
                }
            }

            return result;

        } catch (StorageException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean shouldProcessEvent(Event event, Collection<Long> deviceIds) {
        return deviceIds.isEmpty() || deviceIds.contains(event.getDeviceId());
    }

    private void processEvent(Event event, List<GeofenceReportItem> result, long geofenceId) {
        if (event.getType().equals(Event.TYPE_GEOFENCE_ENTER)) {
            result.add(createGeofenceReportItem(geofenceId, event.getDeviceId(), event.getEventTime(), null));
        } else if (event.getType().equals(Event.TYPE_GEOFENCE_EXIT)) {
            updateExitTime(result, geofenceId, event);
        }
    }

    private GeofenceReportItem createGeofenceReportItem(long geofenceId, long deviceId, Date enterTime, Date exitTime) {
        GeofenceReportItem item = new GeofenceReportItem();
        item.setGeofenceId(geofenceId);
        item.setDeviceId(deviceId);
        item.setEnterTime(enterTime);
        item.setExitTime(exitTime);
        return item;
    }

    private void updateExitTime(List<GeofenceReportItem> result, long geofenceId, Event event) {
        for (GeofenceReportItem item : result) {
            if (item.getDeviceId() == event.getDeviceId() && item.getExitTime() == null) {
                item.setExitTime(event.getEventTime());
                Duration duration = Duration.between(item.getEnterTime().toInstant(), item.getExitTime().toInstant());
                item.setDuration(duration.toSeconds());
                return;
            }
        }

        // If no matching enter event was found, add a new exit-only record
        result.add(createGeofenceReportItem(geofenceId, event.getDeviceId(), null, event.getEventTime()));
    }

}
