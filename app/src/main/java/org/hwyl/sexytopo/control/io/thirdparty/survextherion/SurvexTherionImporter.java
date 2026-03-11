package org.hwyl.sexytopo.control.io.thirdparty.survextherion;

import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Shared importer for Survex and Therion data.
 */
public class SurvexTherionImporter {

    public static final Pattern COMMENT_INSTRUCTION_REGEX = Pattern.compile("([{].*?[}])");


    /**
     * Parse centreline data from Survex/Therion format.
     *
     * Handles:
     * - Forward and backward legs (detects based on station order)
     * - Promoted legs in inline{} format: {from: d1 a1 i1, d2 a2 i2, ...}
     * - Promoted legs in commented new lines format (below main leg)
     * - Both Survex (;) and Therion (#) comment styles
     *
     * @param text The centreline data text
     * @param survey The survey to populate
     * @throws Exception if parsing fails
     */
    public static void parseCentreline(String text, Survey survey) throws Exception {

        Map<String, Station> nameToStation = new HashMap<>();

        String[] lines = text.split("\n");
        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex];

            String trimmed = line.trim();
            if (trimmed.equals("") || trimmed.startsWith("*")) {
                continue;
            }

            // Skip pure comment lines (but not lines with data and comments)
            if (trimmed.startsWith(";") || trimmed.startsWith("#")) {
                // Don't skip if it looks like commented new lines promoted leg
                String[] parts = trimmed.substring(1).trim().split("\\s+");
                if (parts.length < 5) {
                    continue; // Just a comment, skip it
                }
                // Might be commented new lines promoted leg, let it fall through
                continue;
            }

            try {
                // Extract comment - support both ; (Survex) and # (Therion)
                String comment = extractCommentFromLine(line);

                String[] fields = line.trim().split("\\s+");
                
                // Check for commented new lines promoted legs in subsequent lines
                List<Leg> commentedNewLineLegs = parseCommentedNewLinePromotedLegs(
                    lines, lineIndex, fields[0], fields[1]);
                
                addLegToSurvey(survey, nameToStation, fields, comment, commentedNewLineLegs);

            } catch (Exception exception) {
                throw new Exception("Error importing this line: " + line);
            }
        }
    }


    /**
     * Parse passage data section to extract station comments.
     * 
     * Supports:
     * - Therion: "data passage station ignoreall"
     * - Survex:  "*data passage station ignoreall"
     * 
     * @param text The full file text (may contain multiple sections)
     * @param format the file format being parsed (SURVEX or THERION)
     * @return Map of station name to passage comment
     */
    public static Map<String, String> parsePassageData(String text, SurveyFormat format) {
        String dataPassagePrefix = format.getDataPassagePrefix();
        String dataCommandPrefix = format.getCommandChar() + "data ";
        Map<String, String> passageComments = new HashMap<>();
        
        String[] lines = text.split("\n");
        boolean inPassageBlock = false;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            if (trimmed.isEmpty()) {
                continue;
            }
            
            if (trimmed.startsWith(dataPassagePrefix)) {
                inPassageBlock = true;
                continue;
            }
            
            // Exit passage block on any other data command
            if (inPassageBlock && trimmed.startsWith(dataCommandPrefix)
                && !trimmed.startsWith(dataPassagePrefix)) {
                inPassageBlock = false;
                continue;
            }
            
            // If in passage block, parse station and comment
            if (inPassageBlock) {
                // Skip comment lines and command lines
                if (trimmed.startsWith(";") || trimmed.startsWith("#") || 
                    trimmed.startsWith("*") || trimmed.startsWith("extend")) {
                    continue;
                }
                
                // Parse: <station> <comment text>
                // Station name is first token, rest is comment
                String[] parts = trimmed.split("\\s+", 2);
                if (parts.length == 2) {
                    String stationName = parts[0];
                    String comment = parts[1].trim();
                    passageComments.put(stationName, comment);
                }
            }
        }
        
        return passageComments;
    }


    /**
     * Merge passage data comments with existing station comments.
     *
     * If a station has both a passage comment and a leg-line comment,
     *
     * Format: <passage comment> :: <leg-line comment>
     *
     * @param survey The survey with stations
     * @param passageComments Map of station name to passage comment
     */
    public static void mergePassageComments(Survey survey, Map<String, String> passageComments) {
        for (Map.Entry<String, String> entry : passageComments.entrySet()) {
            String stationName = entry.getKey();
            String passageComment = entry.getValue();
            
            try {
                Station station = survey.getStationByName(stationName);
                if (station != null) {
                    String existingComment = station.getComment();
                    
                    if (existingComment == null || existingComment.trim().isEmpty()) {
                        // No existing comment, just use passage comment
                        station.setComment(passageComment);
                    } else {
                        // Both exist, combine with " :: " separator
                        // Format: passage comment :: leg-line comment
                        String combinedComment = passageComment + " :: " + existingComment;
                        station.setComment(combinedComment);
                    }
                }
            } catch (Exception e) {
                Log.e("Failed to merge passage comment for station " + stationName + ": " + e);
            }
        }
    }


    public static Trip parseMetadata(String text, SurveyFormat format) {
        char commentChar = format.getCommentChar();

        Date surveyDate = null;
        Date explorationDate = null;
        String instrument = null;
        Map<String, List<Trip.Role>> teamMap = new java.util.LinkedHashMap<>();
        StringBuilder tripComments = new StringBuilder();
        boolean foundAnyMetadata = false;
        boolean readingTripComments = false;

        String[] lines = text.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                readingTripComments = false;
                continue;
            }

            // Strip command prefix for uniform data handling
            String effective = format.stripCommandPrefix(trimmed);

            // Survey date: "date yyyy.MM.dd" (but not "date explored")
            if (effective.startsWith("date ") && !effective.startsWith("date explored")) {
                String dateStr = effective.substring(5).trim();
                surveyDate = parseDate(dateStr);
                if (surveyDate != null) foundAnyMetadata = true;
                continue;
            }

            // Instrument: "instrument inst \"name\""
            // Commented out instrument are ignored
            if (effective.startsWith("instrument inst ")) {
                instrument = extractQuotedValue(effective, "instrument inst ");
                foundAnyMetadata = true;
                continue;
            }
            // Commented-out instrument line — explicitly clear
            if (trimmed.startsWith(format.getCommentedInstrumentPrefix())) {
                instrument = null;
                foundAnyMetadata = true;
                continue;
            }

            // Team: "team \"Name\" role1 role2"
            if (effective.startsWith("team ")) {
                parseTeamLine(effective, teamMap);
                foundAnyMetadata = true;
                continue;
            }

            // Exploration date
            String exploDateKeyword = format.getExplorationDateKeyword();
            if (effective.startsWith(exploDateKeyword)) {
                String dateStr = effective.substring(exploDateKeyword.length()).trim();
                explorationDate = parseDate(dateStr);
                foundAnyMetadata = true;
                continue;
            }
            // Commented exploration date placeholder
            String commentedExploDate = commentChar + format.getCommandChar() + exploDateKeyword;
            if (trimmed.startsWith(commentedExploDate)) {
                foundAnyMetadata = true;
                continue;
            }

            // Explo-team (Therion only) / format-specific team parsing
            if (format.parseExploTeamLine(effective, teamMap)) {
                foundAnyMetadata = true;
                continue;
            }

            // Trip comments block: "#Comment from SexyTopo trip information"
            String commentPrefix = String.valueOf(commentChar);
            if (trimmed.startsWith(commentPrefix + "Comment from SexyTopo trip information")) {
                readingTripComments = true;
                continue;
            }
            if (readingTripComments && trimmed.startsWith(commentPrefix)) {
                if (tripComments.length() > 0) tripComments.append("\n");
                tripComments.append(trimmed.substring(1));
                continue;
            }

            readingTripComments = false;
        }

        if (!foundAnyMetadata) {
            return null;
        }

        Trip trip = new Trip();

        if (surveyDate != null) {
            trip.setDate(surveyDate);
        }

        trip.setInstrument(instrument);

        // Build team list
        List<Trip.TeamEntry> teamEntries = new ArrayList<>();
        for (Map.Entry<String, List<Trip.Role>> entry : teamMap.entrySet()) {
            teamEntries.add(new Trip.TeamEntry(entry.getKey(), entry.getValue()));
        }
        trip.setTeam(teamEntries);

        // Exploration date
        if (explorationDate != null) {
            if (surveyDate != null && isSameDay(surveyDate, explorationDate)) {
                trip.setExplorationDateSameAsSurvey(true);
            } else {
                trip.setExplorationDateSameAsSurvey(false);
                trip.setExplorationDate(explorationDate);
            }
        } else {
            trip.setExplorationDateSameAsSurvey(false);
        }

        if (tripComments.length() > 0) {
            trip.setComments(tripComments.toString());
        }

        return trip;
    }


    private static void parseTeamLine(String line, Map<String, List<Trip.Role>> teamMap) {
        // line: team "Name" role1 role2
        String afterTeam = line.substring(5).trim();
        String name = null;
        String rolesStr = "";

        if (afterTeam.startsWith("\"")) {
            int closeQuote = afterTeam.indexOf('"', 1);
            if (closeQuote > 0) {
                name = afterTeam.substring(1, closeQuote);
                rolesStr = afterTeam.substring(closeQuote + 1).trim();
            }
        }

        if (name == null || name.isEmpty()) return;

        List<Trip.Role> roles = teamMap.get(name);
        if (roles == null) {
            roles = new ArrayList<>();
            teamMap.put(name, roles);
        }

        if (!rolesStr.isEmpty()) {
            for (String roleStr : rolesStr.split("\\s+")) {
                Trip.Role role = parseRole(roleStr);
                if (role != null && !roles.contains(role)) {
                    roles.add(role);
                }
            }
        }
    }


    private static Trip.Role parseRole(String roleStr) {
        switch (roleStr.toLowerCase()) {
            case "notes":
                return Trip.Role.BOOK;
            case "instruments":
                return Trip.Role.INSTRUMENTS;
            case "explorer":
                return Trip.Role.EXPLORATION;
            case "dog":
            case "assistant":
                return Trip.Role.DOG;
            default:
                return null;
        }
    }


    static String extractQuotedValue(String line, String prefix) {
        String rest = line.substring(prefix.length()).trim();
        if (rest.startsWith("\"") && rest.endsWith("\"")) {
            return rest.substring(1, rest.length() - 1);
        }
        return rest;
    }


    @SuppressWarnings("deprecation")
    private static boolean isSameDay(Date d1, Date d2) {
        return d1.getYear() == d2.getYear()
            && d1.getMonth() == d2.getMonth()
            && d1.getDate() == d2.getDate();
    }


    private static Date parseDate(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(SurvexTherionUtil.TRIP_DATE_PATTERN);
            return sdf.parse(dateStr);
        } catch (Exception e) {
            Log.e("Failed to parse date: " + dateStr);
            return null;
        }
    }


    private static void addLegToSurvey(
            Survey survey, Map<String, Station> nameToStation, String[] fields, 
            String comment, List<Leg> commentedNewLineLegs) {

        String fromName = fields[0];
        String toName = fields[1];
        float distance = Float.parseFloat(fields[2]);
        float azimuth = Float.parseFloat(fields[3]);
        float inclination = Float.parseFloat(fields[4]);

        boolean isSplay = toName.equals(SexyTopoConstants.BLANK_STATION_NAME);

        // Detect if this is a backward leg BEFORE creating new stations
        boolean isBackward = isBackwardLeg(fromName, toName, nameToStation);

        Station from = nameToStation.get(fromName);
        if (from == null) {
            from = new Station(fromName);
            nameToStation.put(fromName, from);
        }

        Station to = Survey.NULL_STATION;
        if (!isSplay) {
            to = nameToStation.get(toName);
            if (to == null) {
                to = new Station(toName);
                nameToStation.put(toName, to);
            }
        }

        // Set origin from first non-splay leg
        // For backward legs the logical root is the TO station.
        if (!isSplay && !nameToStation.containsValue(survey.getOrigin())) {
            survey.setOrigin(isBackward ? to : from);
        }

        // Extract promoted legs from inline{} comment
        String commentInstructions = extractCommentInstructions(comment);
        Leg[] promotedFrom = parseInlinePromotedLegs(commentInstructions);

        // Strip the instruction from the comment so it doesn't end up on the station
        if (!commentInstructions.isEmpty()) {
            comment = comment.replace(commentInstructions, "").trim();
        }
        
        // If no inline{} promoted legs, use commented new lines if available
        if (promotedFrom.length == 0 && !commentedNewLineLegs.isEmpty()) {
            promotedFrom = commentedNewLineLegs.toArray(new Leg[0]);
        }

        Leg leg;
        Station legFrom;

        if (isBackward) {
            // This leg was shot backwards (from new station to existing station)
            // Store with swapped stations and wasShotBackwards = true
            legFrom = to;
            
            // Create leg with original measurements and wasShotBackwards = true
            if (from == Survey.NULL_STATION) {
                leg = new Leg(distance, azimuth, inclination, true);
            } else {
                leg = new Leg(distance, azimuth, inclination, from, promotedFrom, true);
            }
            
            // Station comment goes on the TO station (the new one in the file)
            if (!comment.isEmpty() && from != Survey.NULL_STATION) {
                from.setComment(comment);
            }
        } else {
            // Forward leg
            legFrom = from;
            
            if (to == Survey.NULL_STATION) {
                leg = new Leg(distance, azimuth, inclination);
            } else {
                leg = new Leg(distance, azimuth, inclination, to, promotedFrom);
            }
            
            // Station comment goes on the TO station
            if (!comment.isEmpty() && to != Survey.NULL_STATION) {
                to.setComment(comment);
            }
        }

        legFrom.addOnwardLeg(leg);
        survey.addLegRecord(leg);
        survey.setActiveStation(legFrom);
    }


    /**
     * Detect if a leg was shot backwards.
     * 
     * A leg is backward if the FROM station is new (not seen before).
     * Detection is based on POSITION (from/to), not station names/numbers.
     */
    private static boolean isBackwardLeg(String fromName, String toName, 
                                        Map<String, Station> seenStations) {
        boolean fromIsNew = !seenStations.containsKey(fromName);
        boolean toIsNew = !seenStations.containsKey(toName);
        
        if (fromIsNew && toIsNew) {
            // Both new - shouldn't happen in valid survey, assume forward
            return false;
        }
        
        if (!fromIsNew && !toIsNew) {
            // Both exist - this is a loop/connection, assume forward
            return false;
        }
        
        // Backward if FROM position has the new station
        return fromIsNew;
    }


    /**
     * Parse promoted legs from commented new lines format.
     *
     * These are shots on commented lines that come AFTER the main leg line.
     * 
     * Example:
     * 1  2  5.541  253.93  4.67
     * #1 2  5.542  73.95   -4.64
     * #1 2  5.541  73.93   -4.69
     */
    private static List<Leg> parseCommentedNewLinePromotedLegs(
            String[] lines, int startIndex, String expectedFrom, String expectedTo) {
        
        List<Leg> shots = new ArrayList<>();
        
        // Look at subsequent lines
        for (int i = startIndex + 1; i < lines.length; i++) {
            String line = lines[i].trim();
            
            // Stop if a non-comment line or empty line
            if (line.isEmpty() || (!line.startsWith("#") && !line.startsWith(";"))) {
                break;
            }
            
            // Remove the comment character and parse
            String content = line.substring(1).trim();
            String[] fields = content.split("\\s+");
            
            // Check if this is a matching shot
            if (fields.length >= 5 && 
                fields[0].equals(expectedFrom) && 
                fields[1].equals(expectedTo)) {
                
                try {
                    float distance = Float.parseFloat(fields[2]);
                    float azimuth = Float.parseFloat(fields[3]);
                    float inclination = Float.parseFloat(fields[4]);
                    shots.add(new Leg(distance, azimuth, inclination));
                } catch (NumberFormatException e) {
                    Log.e("Failed to parse commented new line promoted leg: " + line);
                }
            } else {
                // Different leg, stop looking
                break;
            }
        }
        
        return shots;
    }


    private static String extractCommentInstructions(String comment) {
        Matcher matcher = COMMENT_INSTRUCTION_REGEX.matcher(comment);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "";
        }
    }


    private static String extractCommentFromLine(String line) {
        String comment = "";
        
        // Try semicolon first (Survex style)
        if (line.contains(";")) {
            comment = line.substring(line.indexOf(";") + 1).trim();
        } 
        // Try hash (Therion style)
        else if (line.contains("#")) {
            comment = line.substring(line.indexOf("#") + 1).trim();
        }
        
        return comment;
    }


    /**
     * Parse promoted legs from inline{} format.
     * 
     * Example: {from: 5.542 73.95 -4.64, 5.541 73.93 -4.69, 5.541 73.92 -4.67}
     */
    private static Leg[] parseInlinePromotedLegs(String commentInstructions) {
        try {
            return parse(commentInstructions);
        } catch (Exception exception) {
            Log.e(exception);
            return new Leg[]{};
        }
    }


    private static Leg[] parse(String instructions) throws Exception {

        if (instructions.equals("")) {
            return new Leg[]{};
        }

        instructions = instructions
                .replaceFirst("\\{", "")
                .replaceFirst("\\}", "")
                .trim();

        String[] components = instructions.split(":", 2);
        if (components.length < 2) {
            throw new Exception("Cannot parse instructions");
        }
        String tag = components[0];
        String content = components[1];
        List<Leg> legs;
        if (tag.equals("from")) {
            legs = parseFrom(content);
        } else if (tag.equals("backsight")) {
            legs = parseFrom(content);
        } else {
            throw new Exception("Unknown tag: " + tag);
        }

        return legs.toArray(new Leg[]{});
    }


    private static List<Leg> parseFrom(String text) throws Exception {
        List<Leg> legs = new ArrayList<>();
        String[] shotTexts = text.trim().split(",");
        for (String shotText : shotTexts) {
            Leg leg = parseLeg(shotText);
            legs.add(leg);
        }
        return legs;
    }


    private static Leg parseLeg(String text) throws Exception {
        String[] fields = text.trim().split("\\s+");
        if (fields.length != 3) {
            throw new Exception("Expected 3 fields but received " + fields.length);
        }
        float distance = Float.parseFloat(fields[0]);
        float azimuth = Float.parseFloat(fields[1]);
        float inclination = Float.parseFloat(fields[2]);

        return new Leg(distance, azimuth, inclination);
    }

}
