package org.hwyl.sexytopo.control.util.amalgamation;

import java.util.List;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.model.survey.Leg;

/**
 * The user-selectable algorithms for deciding whether repeated readings should be combined into a
 * leg, and for averaging them once they are. Each value dispatches to a stateless strategy.
 */
public enum LegAmalgamationAlgorithm {
    ANGULAR {
        @Override
        public boolean areReadingsCompatible(List<Leg> legs) {
            return AngularAmalgamator.areReadingsCompatible(legs);
        }

        @Override
        public Leg average(List<Leg> legs) {
            return AngularAmalgamator.average(legs);
        }
    },
    CARTESIAN {
        @Override
        public boolean areReadingsCompatible(List<Leg> legs) {
            return CartesianAmalgamator.areReadingsCompatible(legs);
        }

        @Override
        public Leg average(List<Leg> legs) {
            return CartesianAmalgamator.average(legs);
        }
    },
    PAIRWISE {
        @Override
        public boolean areReadingsCompatible(List<Leg> legs) {
            return PairwiseAmalgamator.areReadingsCompatible(legs);
        }

        @Override
        public Leg average(List<Leg> legs) {
            return PairwiseAmalgamator.average(legs);
        }
    };

    /** Returns whether the given readings agree closely enough to be combined into a single leg. */
    public abstract boolean areReadingsCompatible(List<Leg> legs);

    /** Combines the given compatible readings into a single averaged leg. */
    public abstract Leg average(List<Leg> legs);

    public static LegAmalgamationAlgorithm getActive() {
        return fromPreferenceValue(GeneralPreferences.getLegAmalgamationAlgorithm());
    }

    public static LegAmalgamationAlgorithm fromPreferenceValue(String value) {
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            Log.e("Unknown leg amalgamation algorithm: " + value + "; defaulting to ANGULAR");
            return ANGULAR;
        }
    }
}
