package com.randomhax.addon;

import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.util.math.Vec3d;

public final class Utils {
    private Utils() {}

    public static double angleOnAxis(double yawDeg) {
        double r = Math.round(yawDeg / 45.0) * 45.0;
        r %= 360.0;
        if (r < 0) r += 360.0;
        return r;
    }

    public static Vec3d yawToDirection(double yawDeg) {
        double rad = Math.toRadians(yawDeg);
        return new Vec3d(-Math.sin(rad), 0.0, Math.cos(rad));
    }

    public static Vec3d positionInDirection(Vec3d origin, double yawDeg, double distance) {
        return origin.add(yawToDirection(yawDeg).multiply(distance));
    }

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

    public static void info(String message) {
        ChatUtils.info(message);
    }
    public static void info(String fmt, Object... args) {
        ChatUtils.info(fmt, args);
    }
}
