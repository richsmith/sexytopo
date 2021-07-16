package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.R;


public enum InputMode {

    FORWARD(R.id.action_input_mode_forward),
    BACKWARD(R.id.action_input_mode_backward),
    COMBO(R.id.action_input_mode_combo),
    CALIBRATION_CHECK(R.id.action_input_mode_cal_check);

    private final int menuId;

    InputMode(int menuId) {
        this.menuId = menuId;
    }

    public int getMenuId() {
        return menuId;
    }

    public static InputMode byMenuId(int menuId) {
        for (InputMode inputMode : values()) {
            if (inputMode.menuId == menuId) {
                return inputMode;
            }
        }
        throw new IllegalArgumentException("Unknown input mode");
    }
}