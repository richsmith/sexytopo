package org.hwyl.sexytopo.model.survey;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import java.util.ArrayList;
import java.util.List;
import org.hwyl.sexytopo.R;

/**
 * The licences offered by default on the Trip screen. These seed the suggestion list; they are not
 * the only licences available, since the Trip licence field takes free text and any licence used
 * there is remembered alongside these (see GeneralPreferences).
 *
 * <p>A trip stores its licence as the plain name, not as one of these constants, so that a licence
 * typed by hand or read from an imported survey is treated no differently from one picked here.
 */
public enum Licence {
    GPL_3_PLUS(
            "GPLv3.0+",
            R.string.licence_summary_gpl_3_plus,
            "https://www.gnu.org/licenses/gpl-3.0.html",
            true),
    CC0(
            "CC0",
            R.string.licence_summary_cc0,
            "https://creativecommons.org/publicdomain/zero/1.0/",
            true),
    CC_BY_4(
            "CC BY 4.0",
            R.string.licence_summary_cc_by,
            "https://creativecommons.org/licenses/by/4.0/",
            true),
    CC_BY_SA_4(
            "CC BY-SA 4.0",
            R.string.licence_summary_cc_by_sa,
            "https://creativecommons.org/licenses/by-sa/4.0/",
            true),
    CC_BY_NC_4(
            "CC BY-NC 4.0",
            R.string.licence_summary_cc_by_nc,
            "https://creativecommons.org/licenses/by-nc/4.0/",
            true),
    CC_BY_NC_SA_4(
            "CC BY-NC-SA 4.0",
            R.string.licence_summary_cc_by_nc_sa,
            "https://creativecommons.org/licenses/by-nc-sa/4.0/",
            true),
    ALL_RIGHTS_RESERVED(
            "All rights reserved", R.string.licence_summary_all_rights_reserved, null, false);

    /**
     * The licence recommended for survey data: it's a copyleft licence, so derived surveys stay
     * open, and it's the one SexyTopo itself is published under. Tagged as such where licences are
     * listed, so the recommendation is visible at the point of choosing without being applied on
     * the user's behalf.
     */
    public static final Licence RECOMMENDED = GPL_3_PLUS;

    /**
     * The name meaning "no licence": a deliberate choice to leave the survey unlicensed, as opposed
     * to not having decided yet. Stored as the empty string, so it needs no special-casing on
     * export - a trip with this licence behaves exactly like one with none.
     */
    public static final String NONE = "";

    /**
     * Prefixed to the summary of a licence that lets others build on the survey. A tick rather than
     * an information symbol, since choosing one of these is a good outcome and should feel like
     * one. U+2705 is the green-filled variant, so it carries its own colour.
     */
    public static final String FREE_PREFIX = "\u2705 ";

    /**
     * Prefixed to the summary of a licence that doesn't, and to leaving the survey unlicensed, so
     * the restrictive choices are visibly flagged rather than reading like the rest.
     */
    public static final String WARNING_PREFIX = "\u26A0\uFE0F ";

    private final String name;

    @StringRes private final int summaryId;

    private final String url;

    private final boolean isFree;

    Licence(String name, @StringRes int summaryId, String url, boolean isFree) {
        this.name = name;
        this.summaryId = summaryId;
        this.url = url;
        this.isFree = isFree;
    }

    /**
     * Whether the licence lets other cavers reuse and build on the survey. The non-commercial
     * licences count: they wouldn't meet the Open Definition, but they don't stand in the way of
     * the sharing between cavers that this is about.
     */
    public boolean isFree() {
        return isFree;
    }

    /** The emoji flagging how a licence's summary should read: a good outcome, or a warning. */
    public String getSummaryPrefix() {
        return isFree ? FREE_PREFIX : WARNING_PREFIX;
    }

    public String getName() {
        return name;
    }

    /** A one-sentence plain-English summary of what the licence permits. */
    @StringRes
    public int getSummaryId() {
        return summaryId;
    }

    /** Where the licence itself can be read, or null for one that has no canonical page. */
    public String getUrl() {
        return url;
    }

    public boolean hasUrl() {
        return url != null;
    }

    /** The licence with the given name, or null if it isn't one of the defaults. */
    public static Licence forName(String name) {
        for (Licence licence : values()) {
            if (licence.getName().equals(name)) {
                return licence;
            }
        }
        return null;
    }

    /** The names of all the default licences, in the order they should be offered. */
    public static List<String> getDefaultNames() {
        List<String> names = new ArrayList<>();
        for (Licence licence : values()) {
            names.add(licence.getName());
        }
        return names;
    }

    /** Whether the given licence name is one of the defaults, as opposed to a user's own. */
    public static boolean isDefault(String name) {
        return forName(name) != null;
    }

    @Override
    public @NonNull String toString() {
        return name;
    }
}
