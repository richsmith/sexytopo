package org.hwyl.sexytopo.model.survey;

import androidx.annotation.NonNull;

/**
 * One entry in the user-configurable list of license choices offered on the Trip screen. Exactly
 * one entry in any given list is flagged as the default; this is enforced by the code that manages
 * the stored list (see GeneralPreferences.)
 */
public class LicenseOption {

    private final String name;
    private final boolean isDefault;

    public LicenseOption(String name, boolean isDefault) {
        this.name = name;
        this.isDefault = isDefault;
    }

    public String getName() {
        return name;
    }

    public boolean isDefault() {
        return isDefault;
    }

    /** Returns a copy of this option with the same name but the given default flag. */
    public LicenseOption withDefault(boolean newIsDefault) {
        return new LicenseOption(name, newIsDefault);
    }

    /** Returns a copy of this option with a new name but the same default flag. */
    public LicenseOption withName(String newName) {
        return new LicenseOption(newName, isDefault);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LicenseOption)) {
            return false;
        }
        LicenseOption that = (LicenseOption) other;
        if (isDefault != that.isDefault) {
            return false;
        }
        return name == null ? that.name == null : name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (isDefault ? 1 : 0);
        return result;
    }

    @Override
    public @NonNull String toString() {
        return name;
    }
}
