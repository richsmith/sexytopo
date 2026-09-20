package org.hwyl.sexytopo.control.graph;

import android.view.Menu;
import android.view.MenuItem;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.hwyl.sexytopo.R;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Checks how each view context configures the station menu. The menu is a tiny stand-in that only
 * remembers which items are visible, so these run as plain JVM tests.
 */
public class ViewContextMenuTest {

    private Map<Integer, Boolean> visibility;
    private Map<Integer, Integer> titles;
    private Menu menu;

    @Before
    public void setUp() {
        // As inflated from context_station.xml: the cross-section submenu starts hidden and
        // everything else starts visible.
        visibility = new HashMap<>();
        visibility.put(R.id.menu_xsection, false);
        visibility.put(R.id.action_xsection_set_direction, true);
        visibility.put(R.id.action_xsection_create, true);
        visibility.put(R.id.action_xsection_create_horizontal, false);
        visibility.put(R.id.action_jump_to_plan, true);
        visibility.put(R.id.action_jump_to_elevation, true);
        visibility.put(R.id.menu_elevation, true);
        titles = new HashMap<>();
        menu = createMenu(visibility, titles);
    }

    @Test
    public void testPlanShowsCrossSectionsWithSetDirection() {
        ViewContext.PLAN.configureViewSpecificItems(menu);

        Assert.assertTrue(visibility.get(R.id.menu_xsection));
        Assert.assertTrue(visibility.get(R.id.action_xsection_set_direction));
    }

    @Test
    public void testExtendedElevationShowsCrossSectionsWithoutSetDirection() {
        ViewContext.EXTENDED_ELEVATION.configureViewSpecificItems(menu);

        Assert.assertTrue(visibility.get(R.id.menu_xsection));
        Assert.assertFalse(visibility.get(R.id.action_xsection_set_direction));
    }

    @Test
    public void testExtendedElevationKeepsItsOtherItems() {
        ViewContext.EXTENDED_ELEVATION.configureViewSpecificItems(menu);

        Assert.assertTrue(visibility.get(R.id.menu_elevation));
        Assert.assertFalse(visibility.get(R.id.action_jump_to_elevation));
    }

    @Test
    public void testPlanOffersOnlyPlainNewCrossSection() {
        ViewContext.PLAN.configureViewSpecificItems(menu);

        Assert.assertTrue(visibility.get(R.id.action_xsection_create));
        Assert.assertFalse(visibility.get(R.id.action_xsection_create_horizontal));
        Assert.assertFalse(titles.containsKey(R.id.action_xsection_create));
    }

    @Test
    public void testExtendedElevationOffersVerticalAndHorizontal() {
        ViewContext.EXTENDED_ELEVATION.configureViewSpecificItems(menu);

        Assert.assertTrue(visibility.get(R.id.action_xsection_create));
        Assert.assertTrue(visibility.get(R.id.action_xsection_create_horizontal));
        Assert.assertEquals(
                Integer.valueOf(R.string.menu_xsection_create_vertical),
                titles.get(R.id.action_xsection_create));
    }

    @Test
    public void testViewsWithoutCrossSectionsHideTheHorizontalOption() {
        for (ViewContext viewContext :
                new ViewContext[] {ViewContext.TABLE, ViewContext.ELEVATION, ViewContext.THREE_D}) {
            visibility.put(R.id.action_xsection_create_horizontal, true);

            viewContext.configureViewSpecificItems(menu);

            Assert.assertFalse(
                    viewContext.name(), visibility.get(R.id.action_xsection_create_horizontal));
        }
    }

    @Test
    public void testViewsWithoutCrossSectionsHideTheSubmenu() {
        for (ViewContext viewContext :
                new ViewContext[] {ViewContext.TABLE, ViewContext.ELEVATION, ViewContext.THREE_D}) {
            visibility.put(R.id.menu_xsection, true);

            viewContext.configureViewSpecificItems(menu);

            Assert.assertFalse(viewContext.name(), visibility.get(R.id.menu_xsection));
        }
    }

    private static Menu createMenu(Map<Integer, Boolean> visibility, Map<Integer, Integer> titles) {
        Map<Integer, MenuItem> items = new HashMap<>();
        for (Integer id : visibility.keySet()) {
            items.put(id, createItem(id, visibility, titles));
        }
        InvocationHandler handler =
                (proxy, method, args) -> {
                    if (method.getName().equals("findItem")) {
                        return items.get((Integer) args[0]);
                    }
                    return defaultValue(proxy, method);
                };
        return (Menu)
                Proxy.newProxyInstance(
                        ViewContextMenuTest.class.getClassLoader(),
                        new Class<?>[] {Menu.class},
                        handler);
    }

    private static MenuItem createItem(
            int id, Map<Integer, Boolean> visibility, Map<Integer, Integer> titles) {
        InvocationHandler handler =
                (proxy, method, args) -> {
                    if (method.getName().equals("setTitle") && args[0] instanceof Integer) {
                        titles.put(id, (Integer) args[0]);
                        return proxy;
                    }
                    if (method.getName().equals("setVisible")) {
                        visibility.put(id, (Boolean) args[0]);
                        return proxy;
                    }
                    if (method.getName().equals("isVisible")) {
                        return visibility.get(id);
                    }
                    return defaultValue(proxy, method);
                };
        return (MenuItem)
                Proxy.newProxyInstance(
                        ViewContextMenuTest.class.getClassLoader(),
                        new Class<?>[] {MenuItem.class},
                        handler);
    }

    private static Object defaultValue(Object proxy, Method method) {
        Class<?> type = method.getReturnType();
        if (method.getName().equals("hashCode")) {
            return System.identityHashCode(proxy);
        }
        if (method.getName().equals("equals")) {
            return false;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        return null;
    }
}
