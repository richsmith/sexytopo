package org.hwyl.sexytopo.control;

import android.Manifest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SexyTopoPermissions {

    // ********** Permissions **********

    private static final String[] BASIC_PERMISSIONS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
    };

    public static List<String> getPermissions() {
        List<String> permissions = new ArrayList<>(Arrays.asList(BASIC_PERMISSIONS));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            permissions.addAll(
                Arrays.asList(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN));
        }

        return permissions;
    }
}
