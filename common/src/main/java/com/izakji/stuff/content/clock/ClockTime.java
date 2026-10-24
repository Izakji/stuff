package com.izakji.stuff.content.clock;

/**
 * Minecraft tick zero is 06:00; angles are clockwise from twelve o'clock.
 *
 * @author Izakji
 */
public final class ClockTime {
    private ClockTime() {}

    public static float hourAngle(long dayTime, float fraction) {
        double ticks = Math.floorMod(dayTime, 24000L) + 6000.0 + fraction;
        return (float) ((ticks % 12000.0) * 360.0 / 12000.0);
    }

    public static float minuteAngle(long dayTime, float fraction) {
        return (float) ((Math.floorMod(dayTime, 1000L) + fraction) * 360.0 / 1000.0);
    }
}
