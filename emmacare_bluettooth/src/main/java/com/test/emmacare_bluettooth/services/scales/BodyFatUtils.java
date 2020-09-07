package com.test.emmacare_bluettooth.services.scales;

public class BodyFatUtils {
    public static String assemblyData(String unit, String group) {
        String xor = Integer.toHexString(
                hexToTen("fd") ^ hexToTen("37") ^
                        hexToTen(unit) ^ hexToTen(group));
        return "fd37" + unit + group + "000000000000" + xor;
    }

    public static int hexToTen(String hex) {
        if (null == hex || (null != hex && "".equals(hex))) {
            return 0;
        }
        return Integer.valueOf(hex, 16);
    }
}
