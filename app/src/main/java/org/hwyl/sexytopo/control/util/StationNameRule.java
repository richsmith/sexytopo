package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

/**
 * The one rule for what entering a station name does. A leg connects an existing station and
 * creates its new station; on a backward leg the From box holds the new station. A splay has only
 * an existing station.
 */
public final class StationNameRule {

    public enum Action {
        NONE,
        RENAME,
        MOVE,
        CREATE
    }

    public enum Error {
        BLANK,
        RESERVED,
        ALREADY_EXISTS,
        BREAKS_CONTINUITY,
        DOES_NOT_EXIST
    }

    /** What a typed name would do, or why it is refused. */
    public static final class Result {
        public final Action action;
        public final Error error;
        public final String name;

        /** The station moved to, for a move. */
        public final Station target;

        private Result(Action action, Error error, String name, Station target) {
            this.action = action;
            this.error = error;
            this.name = name;
            this.target = target;
        }

        public boolean isValid() {
            return error == null;
        }

        private static Result of(Action action, String name) {
            return new Result(action, null, name, null);
        }

        private static Result moveTo(Station target) {
            return new Result(Action.MOVE, null, target.getName(), target);
        }

        private static Result refused(Error error, String name) {
            return new Result(Action.NONE, error, name, null);
        }
    }

    private StationNameRule() {}

    /**
     * A leg's existing station. A new name renames the station everywhere. Another station's name
     * moves the leg there, unless that station is the leg's new station or beyond it. When adding,
     * leg is null and a move just picks the station to add from.
     */
    public static Result checkExisting(Survey survey, Station station, Leg leg, String name) {
        Error error = checkName(name);
        if (error != null) {
            return Result.refused(error, name);
        }
        if (name.equals(station.getName())) {
            return Result.of(Action.NONE, name);
        }
        Station target = survey.getStationByName(name);
        if (target == null) {
            return Result.of(Action.RENAME, name);
        }
        if (leg != null && SurveyTraversal.isInSubtree(leg.getDestination(), target)) {
            return Result.refused(Error.BREAKS_CONTINUITY, name);
        }
        return Result.moveTo(target);
    }

    /** A splay's station. Another station's name moves this splay only; a new name is refused. */
    public static Result checkSplayStation(Survey survey, Station station, String name) {
        Error error = checkName(name);
        if (error != null) {
            return Result.refused(error, name);
        }
        if (name.equals(station.getName())) {
            return Result.of(Action.NONE, name);
        }
        Station target = survey.getStationByName(name);
        if (target == null) {
            return Result.refused(Error.DOES_NOT_EXIST, name);
        }
        return Result.moveTo(target);
    }

    /**
     * A leg's new station, which must take a new name. When adding, station is null and the name
     * creates it; otherwise the station is renamed everywhere. A name the existing station is being
     * renamed to counts as taken.
     */
    public static Result checkNew(Survey survey, Station station, String name, Result existing) {
        Error error = checkName(name);
        if (error != null) {
            return Result.refused(error, name);
        }
        if (station != null && name.equals(station.getName())) {
            return Result.of(Action.NONE, name);
        }
        boolean taken = existing != null && existing.action == Action.RENAME;
        if (survey.getStationByName(name) != null || (taken && name.equals(existing.name))) {
            return Result.refused(Error.ALREADY_EXISTS, name);
        }
        return Result.of(station == null ? Action.CREATE : Action.RENAME, name);
    }

    private static Error checkName(String name) {
        if (name == null || name.isEmpty()) {
            return Error.BLANK;
        }
        if (Station.isReservedName(name)) {
            return Error.RESERVED;
        }
        return null;
    }
}
