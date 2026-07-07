package com.markgame.character;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

/**
 * Shared math for anything that walks on a spherical planet with radial
 * ("down is toward the center") gravity instead of a fixed world-down vector.
 * Used by CharacterController, EnemyController, FollowCamera, and PlanetBuilder.
 *
 * Verified against the libGDX 1.x Matrix4/Quaternion/Vector3 API:
 * Matrix4.set(Vector3 xAxis, Vector3 yAxis, Vector3 zAxis, Vector3 pos) and
 * Quaternion.setFromMatrix(Matrix4) / setFromAxisRad(x,y,z,radians) both exist
 * as used below.
 */
public final class GravityMath {

    private GravityMath() {}

    /** Outward radial direction at `position` relative to a planet centered at `center`. Writes into `out`. */
    public static Vector3 up(Vector3 position, Vector3 center, Vector3 out) {
        out.set(position).sub(center);
        if (out.len2() < 0.0001f) out.set(0, 1, 0);
        return out.nor();
    }

    /**
     * Projects v onto the tangent plane of `up` and normalizes in place, falling back
     * to an arbitrary perpendicular vector if v is (near) parallel to up.
     */
    public static Vector3 reprojectTangent(Vector3 v, Vector3 up) {
        float d = v.dot(up);
        v.mulAdd(up, -d);
        if (v.len2() < 0.0001f) {
            Vector3 arbitrary = Math.abs(up.y) < 0.99f ? new Vector3(0, 1, 0) : new Vector3(1, 0, 0);
            v.set(arbitrary).crs(up).nor();
        } else {
            v.nor();
        }
        return v;
    }

    /** Rotates v (assumed perpendicular to axis) around axis by `radians`, Rodrigues' formula, in place. */
    public static Vector3 rotateAroundAxis(Vector3 v, Vector3 axis, float radians) {
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        Vector3 crossTerm = new Vector3(axis).crs(v).scl(sin);
        Vector3 result = new Vector3(v).scl(cos).add(crossTerm);
        v.set(result);
        return v;
    }

    /** Signed angle (radians) to rotate `from` onto `to` around `axis`, both assumed tangent to `axis`. */
    public static float signedAngle(Vector3 from, Vector3 to, Vector3 axis) {
        Vector3 cross = new Vector3(from).crs(to);
        float sinA = cross.dot(axis);
        float cosA = from.dot(to);
        return (float) Math.atan2(sinA, cosA);
    }

    /** A point on a sphere of `radius` centered at `center`, colatitude `phi` (0 = north pole), longitude `theta`. */
    public static Vector3 pointOnSphere(Vector3 center, float radius, float phi, float theta, Vector3 out) {
        float sinPhi = (float) Math.sin(phi);
        out.set(
            radius * sinPhi * (float) Math.cos(theta),
            radius * (float) Math.cos(phi),
            radius * sinPhi * (float) Math.sin(theta)
        );
        return out.add(center);
    }

    private static final Matrix4 tmpMatrix = new Matrix4();

    /** Orientation whose local +X/+Y/+Z map to right/up/forward. */
    public static Quaternion basisOrientation(Vector3 right, Vector3 up, Vector3 forward, Quaternion out) {
        tmpMatrix.set(right, up, forward, Vector3.Zero);
        return out.setFromMatrix(tmpMatrix);
    }

    /**
     * Rotation that maps unit vector `from` onto unit vector `to` (shortest arc).
     * Implemented manually via axis-angle rather than relying on a specific
     * Quaternion convenience method, so it's correct across libGDX versions.
     */
    public static Quaternion fromToRotation(Vector3 from, Vector3 to, Quaternion out) {
        Vector3 f = new Vector3(from).nor();
        Vector3 t = new Vector3(to).nor();
        float dot = f.dot(t);

        if (dot > 0.999999f) {
            return out.idt();
        }
        if (dot < -0.999999f) {
            Vector3 axis = new Vector3(1, 0, 0).crs(f);
            if (axis.len2() < 0.000001f) axis.set(0, 1, 0).crs(f);
            axis.nor();
            return out.setFromAxisRad(axis.x, axis.y, axis.z, (float) Math.PI);
        }

        Vector3 axis = new Vector3(f).crs(t).nor();
        float angle = (float) Math.acos(dot);
        return out.setFromAxisRad(axis.x, axis.y, axis.z, angle);
    }
}
