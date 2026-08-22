package org.hwyl.sexytopo.control.io.thirdparty.survextherion;

import androidx.annotation.NonNull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and compares SexyTopo version numbers embedded in exported file headers.
 *
 * <p>Scans comment lines (those starting with {@code #} for Therion or {@code ;} for Survex) for a
 * token of the form {@code SexyTopo X.Y.Z}. Surrounding wording is irrelevant; data lines are
 * ignored so a station or trip name containing the word "SexyTopo" cannot be mistaken for a version
 * stamp.
 *
 * <p>Files with no such token return null from extractFromText.
 */
public class SexyTopoVersion {

    /**
     * The first version that writes leg/splay comments inline on data lines rather than as station
     * comments in a separate data passage block. Files written by versions strictly after this
     * cutoff (i.e. 1.11.3+) use the new leg-comment import path. Files at or before this version,
     * use the legacy station-comment path.
     */
    public static final SexyTopoVersion LEG_COMMENTS_VERSION_CUTOFF = new SexyTopoVersion(1, 11, 2);

    private static final Pattern VERSION_PATTERN =
            Pattern.compile("SexyTopo\\s+(\\d+)\\.(\\d+)\\.(\\d+)");

    private final int major;
    private final int minor;
    private final int patch;

    public SexyTopoVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /**
     * Scan comment lines in the file text for a {@code SexyTopo X.Y.Z} token and parse the version.
     *
     * @param text full file text
     * @return parsed version, or null if no SexyTopo token is found in any comment line
     */
    public static SexyTopoVersion extractFromText(String text) {
        if (text == null) {
            return null;
        }
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("#") && !trimmed.startsWith(";")) {
                continue;
            }
            Matcher matcher = VERSION_PATTERN.matcher(trimmed);
            if (matcher.find()) {
                return new SexyTopoVersion(
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3)));
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if this version is strictly greater than {@code other}.
     *
     * <p>Comparison is major → minor → patch.
     */
    public boolean isAfter(SexyTopoVersion other) {
        if (this.major != other.major) return this.major > other.major;
        if (this.minor != other.minor) return this.minor > other.minor;
        return this.patch > other.patch;
    }

    @NonNull
    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
