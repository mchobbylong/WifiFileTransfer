package computing.project.wififiletransfer.common;

import android.annotation.SuppressLint;

public class CommonUtils {
    public static final String[] remainingTimeUnitText = new String[]{
            "hour(s)",
            "minute(s)",
            "second(s)",
    };
    public static final long[] remainingTimeUnitBound = new long[]{ 3600, 60, 0 };

    public static String getRemainingTimeText(long remainingTime) {
        for (int i = 0; i < remainingTimeUnitText.length; ++i)
            if (remainingTime >= remainingTimeUnitBound[i]) {
                remainingTime /= remainingTimeUnitBound[i] > 0 ? remainingTimeUnitBound[i] : 1;
                return remainingTime + " " + remainingTimeUnitText[i];
            }
        return "Error";
    }

    @SuppressLint("DefaultLocale")
    public static String getSpeedText(double speed) {
        if (speed > 1024.0) return String.format("%dMB/s", (long) speed / 1024);
        else return String.format("%dKB/s", (long) speed);
    }
}
