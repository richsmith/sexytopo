package org.hwyl.sexytopo.control.io.thirdparty.survextherion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hwyl.sexytopo.model.survey.Trip;

public enum SurveyFormat {
    SURVEX {
        @Override
        public char getCommentChar() {
            return ';';
        }

        @Override
        public String getCommandChar() {
            return "*";
        }

        @Override
        public String getSplayStationName() {
            return "..";
        }

        @Override
        public String getExplorationDateKeyword() {
            return "date explored ";
        }

        @Override
        public boolean parseExploTeamLine(String effective, Map<String, List<Trip.Role>> teamMap) {
            return false;
        }
    },

    THERION {
        @Override
        public char getCommentChar() {
            return '#';
        }

        @Override
        public String getCommandChar() {
            return "";
        }

        @Override
        public String getSplayStationName() {
            return "-";
        }

        @Override
        public String getExplorationDateKeyword() {
            return "explo-date ";
        }

        @Override
        public boolean parseExploTeamLine(String effective, Map<String, List<Trip.Role>> teamMap) {
            if (!effective.startsWith("explo-team ")) {
                return false;
            }
            String name = SurvexTherionImporter.extractQuotedValue(effective, "explo-team ");
            if (!name.isEmpty()) {
                List<Trip.Role> roles = teamMap.get(name);
                if (roles == null) {
                    roles = new ArrayList<>();
                    teamMap.put(name, roles);
                }
                if (!roles.contains(Trip.Role.EXPLORATION)) {
                    roles.add(Trip.Role.EXPLORATION);
                }
            }
            return true;
        }
    };

    public abstract char getCommentChar();

    public abstract String getCommandChar();

    /** Splay station name: Survex ".." Therion "-" */
    public abstract String getSplayStationName();

    /** Exploration date keyword: "date explored "- Survex, "explo-date "- Therion */
    public abstract String getExplorationDateKeyword();

    /**
     * Try to parse a team-related line during import. Returns true if the line was consumed (e.g.
     * explo-team in Therion).
     */
    public abstract boolean parseExploTeamLine(
            String effective, Map<String, List<Trip.Role>> teamMap);

    /**
     * Commented-out instrument prefix: ";*instrument insts " for Survex, "#instrument insts " for
     * Therion
     */
    public String getCommentedInstrumentPrefix() {
        return getCommentChar() + getCommandChar() + "instrument insts ";
    }

    /**
     * passage data
     *
     * <ul>
     *   <li>*data passage for Survex
     *   <li>data dimensions for Therion
     */
    public String getDataPassagePrefix() {
        // Survex uses "passage", Therion uses "dimensions"
        if (this == SURVEX) {
            return getCommandChar() + "data passage";
        } else {
            return getCommandChar() + "data dimensions";
        }
    }

    /** Strip command prefix from a line: removes leading "*" for Survex, no-op for Therion */
    public String stripCommandPrefix(String line) {
        String marker = getCommandChar();
        if (!marker.isEmpty() && line.startsWith(marker)) {
            return line.substring(marker.length());
        }
        return line;
    }
}
