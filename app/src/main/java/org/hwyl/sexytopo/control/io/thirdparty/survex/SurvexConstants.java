package org.hwyl.sexytopo.control.io.thirdparty.survex;

public class SurvexConstants {

    public static final String SVX_EXTENSION = "svx";
    public static final String SVX_MIME_TYPE = "text/svx";

    /** Extended elevation specification, read by Survex's extend tool alongside the .svx. */
    public static final String ESPEC_EXTENSION = "espec";

    // application/octet-stream stops Android from appending .txt to the filename
    public static final String ESPEC_MIME_TYPE = "application/octet-stream";

    // .espec commands; *evertical isn't supported by Survex's extend tool yet, so it's written
    // commented out but still read back on import
    public static final String ESPEC_START = "*start";
    public static final String ESPEC_LEFT = "*eleft";
    public static final String ESPEC_RIGHT = "*eright";
    public static final String ESPEC_VERTICAL = "*evertical";
    public static final String ESPEC_COMMENT_PREFIX = "; ";

    private SurvexConstants() {}
}
