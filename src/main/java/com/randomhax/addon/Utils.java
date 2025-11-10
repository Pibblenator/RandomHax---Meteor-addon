package com.randomhax.addon;

import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.util.math.Vec3d;

public final class Utils {
    private Utils() {}

    /* ------------------------ chat ------------------------ */

    public static void info(String message) {
        ChatUtils.info(message);
    }
    public static void info(String fmt, Object... args) {
        ChatUtils.info(fmt, args);
    }

    /* ------------------------ angles / rotation ------------------------ */

    /** Round yaw to the nearest 45° axis and normalize to [0,360). */
    public static double angleOnAxis(double yawDeg) {
        double r = Math.round(yawDeg / 45.0) * 45.0;
        r %= 360.0;
        if (r < 0) r += 360.0;
        return r;
    }

    /** Smallest signed angle difference (target - current) in degrees in (-180, 180]. */
    public static double angleDifference(double targetDeg, double currentDeg) {
        double diff = (targetDeg - currentDeg + 180.0) % 360.0;
        if (diff < 0) diff += 360.0;
        return diff - 180.0;
    }

    /**
     * Smoothly steps current toward target by a fraction (rotationScaling in [0,1]).
     * Returns the new yaw in degrees.
     */
    public static float smoothRotation(float currentDeg, double targetDeg, double rotationScaling) {
        // clamp scaling just in case
        if (rotationScaling < 0) rotationScaling = 0;
        if (rotationScaling > 1) rotationScaling = 1;

        double step = angleDifference(targetDeg, currentDeg) * rotationScaling;
        return (float) (currentDeg + step);
    }

    /* ------------------------ vectors / geometry ------------------------ */

    /** Convert yaw degrees to a horizontal direction vector. */
    public static Vec3d yawToDirection(double yawDeg) {
        double rad = Math.toRadians(yawDeg);
        return new Vec3d(-Math.sin(rad), 0.0, Math.cos(rad));
    }

    /** Origin + (normalized yaw direction * distance). */
    public static Vec3d positionInDirection(Vec3d origin, double yawDeg, double distance) {
        return origin.add(yawToDirection(yawDeg).multiply(distance));
    }

    /**
     * Distance from a horizontal point to an infinite horizontal ray (dir, origin).
     * dir does not need to be normalized; Y is ignored.
     */
    public static double distancePointToDirection(Vec3d point, Vec3d dir, Vec3d origin) {
        Vec3d d = new Vec3d(dir.x, 0.0, dir.z);
        double len = d.length();
        if (len < 1e-8) return point.distanceTo(origin);

        d = d.multiply(1.0 / len);

        Vec3d p = new Vec3d(point.x, 0.0, point.z);
        Vec3d o = new Vec3d(origin.x, 0.0, origin.z);

        double t = p.subtract(o).dotProduct(d);
        Vec3d closest = o.add(d.multiply(t));
        return p.distanceTo(closest);
    }
}
