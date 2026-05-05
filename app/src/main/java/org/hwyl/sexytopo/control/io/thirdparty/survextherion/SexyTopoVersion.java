package org.hwyl.sexytopo.control.io.thirdparty.survextherion;

import androidx.annotation.NonNull;

/**
 * Parses and compares SexyTopo version numbers embedded in exported file headers.
 *
 * <p>The header format written by the exporter is:
 *
 * <pre>
 *   # Created with SexyTopo 1.11.3 on 2025-03-01   (Therion)
 *   ; Created with SexyTopo 1.11.3 on 2025-03-01   (Survex)
 * </pre>
 *
 * <p>Files with no SexyTopo header (third-party files) return {@code null} from {@link
 * #extractFromText(String)}.
 */
public class SexyTopoVersion {

    /**
     * The first version that writes leg/splay comments inline on data lines rather than as station
     * comments in a separate data passage block. Files written by versions strictly after this
     * cutoff (i.e. 1.11.3+) use the new leg-comment import path. Files at or before this version,
     * use the legacy station-comment path.
     */
    public static final SexyTopoVersion LEG_COMMENTS_VERSION_CUTOFF = new SexyTopoVersion(1, 11, 2);

    private static final String HEADER_MARKER = "Created with SexyTopo ";

    private final int major;
    private final int minor;
    private final int patch;

    public SexyTopoVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /**
     * Scan the first lines of a file for a SexyTopo version header and parse the version.
     *
     * @param text full file text
     * @return parsed version, or {@code null} if no SexyTopo header is found or it cannot be parsed
     */
    public static SexyTopoVersion extractFromText(String text) {
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            // Strip leading comment character (# or ;) then look for the marker
            String stripped =
                    (trimmed.startsWith("#") || trimmed.startsWith(";"))
                            ? trimmed.substring(1).trim()
                            : trimmed;

            int markerIndex = stripped.indexOf(HEADER_MARKER);
            if (markerIndex < 0) {
                continue;
            }

            // Version token is the word immediately after the marker
            String afterMarker = stripped.substring(markerIndex + HEADER_MARKER.length()).trim();
            String[] tokens = afterMarker.split("\\s+");
            if (tokens.length == 0) {
                return null;
            }

            return parse(tokens[0]);
        }
        return null;
    }

    /**
     * Parse a dotted version string such as {@code "1.11.3"}.
     *
     * @return parsed version, or {@code null} if the string is not a valid version
     */
    static SexyTopoVersion parse(String versionString) {
        String[] parts = versionString.split("\\.");
        if (parts.length != 3) {
            return null;
        }
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = Integer.parseInt(parts[2]);
            return new SexyTopoVersion(major, minor, patch);
        } catch (NumberFormatException e) {
            return null;
        }
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
