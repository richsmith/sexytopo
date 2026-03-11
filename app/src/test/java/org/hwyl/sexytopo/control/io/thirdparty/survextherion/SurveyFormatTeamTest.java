package org.hwyl.sexytopo.control.io.thirdparty.survextherion;

import org.hwyl.sexytopo.model.survey.Trip;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class SurveyFormatTeamTest {

    // === Import tests ===

    @Test
    public void testSurvexParseExploTeamLineReturnsFalse() {
        Map<String, List<Trip.Role>> teamMap = new LinkedHashMap<>();
        boolean consumed = SurveyFormat.SURVEX.parseExploTeamLine(
                "explo-team \"Alice\"", teamMap);
        Assert.assertFalse(consumed);
        Assert.assertTrue(teamMap.isEmpty());
    }

    @Test
    public void testTherionParseExploTeamLine() {
        Map<String, List<Trip.Role>> teamMap = new LinkedHashMap<>();
        boolean consumed = SurveyFormat.THERION.parseExploTeamLine(
                "explo-team \"Alice\"", teamMap);
        Assert.assertTrue(consumed);
        Assert.assertTrue(teamMap.containsKey("Alice"));
        List<Trip.Role> roles = teamMap.get("Alice");
        Assert.assertNotNull(roles);
        Assert.assertTrue(roles.contains(Trip.Role.EXPLORATION));
    }

    @Test
    public void testTherionParseExploTeamLineMergesWithExistingRoles() {
        Map<String, List<Trip.Role>> teamMap = new LinkedHashMap<>();
        List<Trip.Role> existingRoles = new ArrayList<>();
        existingRoles.add(Trip.Role.INSTRUMENTS);
        teamMap.put("Alice", existingRoles);

        SurveyFormat.THERION.parseExploTeamLine("explo-team \"Alice\"", teamMap);
        List<Trip.Role> roles = teamMap.get("Alice");
        Assert.assertNotNull(roles);
        Assert.assertEquals(2, roles.size());
        Assert.assertTrue(roles.contains(Trip.Role.INSTRUMENTS));
        Assert.assertTrue(roles.contains(Trip.Role.EXPLORATION));
    }

    @Test
    public void testTherionParseExploTeamLineIgnoresNonMatch() {
        Map<String, List<Trip.Role>> teamMap = new LinkedHashMap<>();
        boolean consumed = SurveyFormat.THERION.parseExploTeamLine(
                "team \"Alice\" notes", teamMap);
        Assert.assertFalse(consumed);
        Assert.assertTrue(teamMap.isEmpty());
    }

    @Test
    public void testTherionParseExploTeamLineNoDuplicate() {
        Map<String, List<Trip.Role>> teamMap = new LinkedHashMap<>();
        List<Trip.Role> existingRoles = new ArrayList<>();
        existingRoles.add(Trip.Role.EXPLORATION);
        teamMap.put("Alice", existingRoles);

        SurveyFormat.THERION.parseExploTeamLine("explo-team \"Alice\"", teamMap);
        List<Trip.Role> roles = teamMap.get("Alice");
        Assert.assertNotNull(roles);
        Assert.assertEquals(1, roles.size());
    }
}
