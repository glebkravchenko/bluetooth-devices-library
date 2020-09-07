package com.test.emmacare_bluettooth.services.scales;

public class UnitConfig {

    public static final int UNIT_KG = 0x0;
    public static final int UNIT_LB = 0x1;
    public static final int UNIT_JIN = 0x2;

    private static class UnitSave {
        int unit;
    }

    public static synchronized int getUnit() {
        UnitSave unitSave = new UnitSave();
        unitSave.unit = UNIT_KG;
        return UNIT_KG;
    }
}
