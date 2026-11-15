package org.hwyl.sexytopo.control.util.amalgamation;

import java.util.List;
import java.util.Locale;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.model.survey.Leg;

/**
 * The user-selectable algorithms for deciding whether repeated readings should be combined into a
 * leg, and for averaging them once they are. Each value dispatches to a stateless strategy.
 */
public enum LegAmalgamationAlgorithm {
    ANGULAR(
            R.string.settings_leg_amalgamation_angular_name,
            R.string.settings_leg_amalgamation_angular_description) {
        @Override
        public boolean areReadingsCompatible(List<Leg> legs) {
            return AngularAmalgamator.areReadingsCompatible(legs);
        }

        @Override
        public Leg average(List<Leg> legs) {
            return AngularAmalgamator.average(legs);
        }
    },
    SLOPE_CORRECTED(
            R.string.settings_leg_amalgamation_slope_corrected_name,
            R.string.settings_leg_amalgamation_slope_corrected_description) {
        @Override
        public boolean areReadingsCompatible(List<Leg> legs) {
            return SlopeCorrectedAmalgamator.areReadingsCompatible(legs);
        }

        @Override
        public Leg average(List<Leg> legs) {
            return SlopeCorrectedAmalgamator.average(legs);
        }
    },
    CARTESIAN(
            R.string.settings_leg_amalgamation_cartesian_name,
            R.string.settings_leg_amalgamation_cartesian_description) {
        @Override
        public boolean areReadingsCompatible(List<Leg> legs) {
            return CartesianAmalgamator.areReadingsCompatible(legs);
        }

        @Override
        public Leg average(List<Leg> legs) {
            return CartesianAmalgamator.average(legs);
        }
    },
    PAIRWISE(
            R.string.settings_leg_amalgamation_pairwise_name,
            R.string.settings_leg_amalgamation_pairwise_description) {
        @Override
        public boolean areReadingsCompatible(List<Leg> legs) {
            return PairwiseAmalgamator.areReadingsCompatible(legs);
        }

        @Override
        public Leg average(List<Leg> legs) {
            return PairwiseAmalgamator.average(legs);
        }
    };

    private final int nameId;
    private final int descriptionId;

    LegAmalgamationAlgorithm(int nameId, int descriptionId) {
        this.nameId = nameId;
        this.descriptionId = descriptionId;
    }

    public int getNameId() {
        return nameId;
    }

    public int getDescriptionId() {
        return descriptionId;
    }

    /** Returns whether the given readings agree closely enough to be combined into a single leg. */
    public abstract boolean areReadingsCompatible(List<Leg> legs);

    /** Combines the given compatible readings into a single averaged leg. */
    public abstract Leg average(List<Leg> legs);

    public String toPreferenceValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static LegAmalgamationAlgorithm getActive() {
        return fromPreferenceValue(GeneralPreferences.getLegAmalgamationAlgorithm());
    }

    public static LegAmalgamationAlgorithm fromPreferenceValue(String value) {
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            Log.e("Unknown leg amalgamation algorithm: " + value + "; defaulting to ANGULAR");
            return ANGULAR;
        }
    }
}
