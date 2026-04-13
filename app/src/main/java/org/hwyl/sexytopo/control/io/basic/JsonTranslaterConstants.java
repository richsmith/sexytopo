package org.hwyl.sexytopo.control.io.basic;

/**
 * Tags shared between the JSON translaters. These identify informational fields (app version,
 * survey name) that are written into every SexyTopo JSON file so their provenance is visible, but
 * are ignored on load.
 */
public final class JsonTranslaterConstants {

    public static final String VERSION_NAME_TAG = "sexyTopoVersionName";
    public static final String VERSION_CODE_TAG = "sexyTopoVersionCode";
    public static final String SURVEY_NAME_TAG = "name";

    private JsonTranslaterConstants() {}
}
