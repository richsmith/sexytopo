package org.hwyl.sexytopo.control.util;

/**
 * Wrapper for inner classes to use to return variables. This is a hack to get around the
 * restriction that inner classes can only access variables mark final and therefore not
 * reassign them.
 */
public class Wrapper {
    public Object value;
}
