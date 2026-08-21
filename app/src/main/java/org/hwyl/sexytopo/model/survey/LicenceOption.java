package org.hwyl.sexytopo.model.survey;

import androidx.annotation.NonNull;

/**
 * One entry in the user-configurable list of licence choices offered on the Trip screen. Exactly
 * one entry in any given list is flagged as the default; this is enforced by the code that manages
 * the stored list (see GeneralPreferences.)
 */
public class LicenceOption {

    private final String name;
    private final boolean isDefault;

    public LicenceOption(String name, boolean isDefault) {
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
    public LicenceOption withDefault(boolean newIsDefault) {
        return new LicenceOption(name, newIsDefault);
    }

    /** Returns a copy of this option with a new name but the same default flag. */
    public LicenceOption withName(String newName) {
        return new LicenceOption(newName, isDefault);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LicenceOption)) {
            return false;
        }
        LicenceOption that = (LicenceOption) other;
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
