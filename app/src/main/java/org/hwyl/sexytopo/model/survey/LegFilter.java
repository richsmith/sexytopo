package org.hwyl.sexytopo.model.survey;

import java.util.function.Predicate;

/**
 * Which of a station's onward legs to fetch. Start from ALL, LEGS or SPLAYS and narrow or widen it
 * from there, e.g. SPLAYS.includeFeatures(false). Disabled splays are left out unless asked for
 * with includeDisabled, and splays to other features such as stal are kept unless left out with
 * includeFeatures.
 *
 * <p>This is a Predicate, so it can also be used directly to filter a stream of legs.
 */
public final class LegFilter implements Predicate<Leg> {

    private static final boolean DEFAULT_INCLUDE_DISABLED = false;
    private static final boolean DEFAULT_INCLUDE_FEATURES = true;

    /** Every enabled leg and splay. */
    public static final LegFilter ALL = new LegFilter(true, true);

    /** Connected legs, i.e. ones that go to another station. Legs are always enabled. */
    public static final LegFilter LEGS = new LegFilter(true, false);

    /** Enabled splays. */
    public static final LegFilter SPLAYS = new LegFilter(false, true);

    private final boolean includeLegs;
    private final boolean includeSplays;
    private final boolean includeDisabled;
    private final boolean includeFeatures;

    private LegFilter(boolean includeLegs, boolean includeSplays) {
        this(includeLegs, includeSplays, DEFAULT_INCLUDE_DISABLED, DEFAULT_INCLUDE_FEATURES);
    }

    private LegFilter(
            boolean includeLegs,
            boolean includeSplays,
            boolean includeDisabled,
            boolean includeFeatures) {
        this.includeLegs = includeLegs;
        this.includeSplays = includeSplays;
        this.includeDisabled = includeDisabled;
        this.includeFeatures = includeFeatures;
    }

    /**
     * The same filter, but keeping or leaving out disabled splays. Only keep them when they
     * genuinely have to be included, such as when saving or editing the survey.
     */
    public LegFilter includeDisabled(boolean include) {
        return new LegFilter(includeLegs, includeSplays, include, includeFeatures);
    }

    /**
     * The same filter, but keeping or leaving out splays to another feature, such as a stal, rather
     * than to a wall. Those don't show the shape of the passage. Legs are unaffected.
     */
    public LegFilter includeFeatures(boolean include) {
        return new LegFilter(includeLegs, includeSplays, includeDisabled, include);
    }

    @Override
    public boolean test(Leg leg) {
        boolean kindMatches = leg.hasDestination() ? includeLegs : includeSplays;
        return kindMatches
                && (includeDisabled || leg.isEnabled())
                && (includeFeatures || leg.isToWall());
    }
}
