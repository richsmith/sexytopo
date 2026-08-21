package org.hwyl.sexytopo.model.survey;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

/**
 * The licences offered by default on the Trip screen. These seed the suggestion list; they are not
 * the only licences available, since the Trip licence field takes free text and any licence used
 * there is remembered alongside these (see GeneralPreferences).
 *
 * <p>A trip stores its licence as the plain name, not as one of these constants, so that a licence
 * typed by hand or read from an imported survey is treated no differently from one picked here.
 */
public enum Licence {
    GPL_3_PLUS("GPLv3.0+"),
    CC0("CC0"),
    CC_BY_4("CC BY 4.0"),
    CC_BY_SA_4("CC BY SA 4.0"),
    CC_BY_SA_NC_4("CC BY SA NC 4.0"),
    ALL_RIGHTS_RESERVED("All rights reserved");

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

    private final String name;

    Licence(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
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
        for (Licence licence : values()) {
            if (licence.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @NonNull String toString() {
        return name;
    }
}
