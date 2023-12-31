package org.firstinspires.ftc.teamcode.modules.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.acmerobotics.roadrunner.geometry.Pose2d;

/**
 * A point.  The coordinate system we are using is horizontal and vertical translation in a rotated
 *  XY coordinate plane, and our unit of distance is nanoseconds.
 */
public class Movement {
    public static final double EPSILON = 0.001;

    /**
     * Determines if the point is small enough to be considered 'zero'
     * @return true if the point is small enough
     */
    public boolean isZero() {
        return Math.abs(x) < EPSILON &&
                Math.abs(y) < EPSILON &&
                Math.abs(theta) < EPSILON;
    }

    /**
     * Checks for equality with another object
     * @param obj The other object
     * @return True if the object is of type {@link Movement} and their {@link #x}, {@link #y}, and {@link #theta} are
     *  within {@link #EPSILON} of ours
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (super.equals(obj)) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Movement)) {
            return false;
        }
        return negate().add((Movement) obj).isZero();
    }

    /**
     * Gets the hash code of this object
     * @return The XOR of {@link #x}, {@link #y}, and {@link #theta}
     */
    @Override
    public int hashCode() {
        return Double.hashCode(x) ^ Double.hashCode(y) ^ Double.hashCode(theta);
    }

    public double x;
    public double y;
    public double theta;

    public Movement(double x, double y, double theta) {
        this.x = x;
        this.y = y;
        this.theta = theta;
    }

    public Movement(double x, double y) {
        this(x, y, 0);
    }

    /**
     * A new zeroed-out {@link Movement} object
     * @return {@code <0,0,0>}
     */
    public static Movement zero() {
        return new Movement(0,0,0);
    }

    /**
     * Adds two points together (doesn't modify existing points)
     * @param addend the other addend
     * @return the sum of the two points
     */
    public Movement add(Movement addend) {
        return new Movement(x + addend.x, y + addend.y, theta + addend.theta);
    }

    /**
     * Negates all coordinates in the point (doesn't modify the existing point)
     * @return the negated Point
     */
    public Movement negate() {
        return multiply(-1);
    }

    /**
     * Subtracts the given term from this {@link Movement} object (doesn't modify existing points)
     * @param subtrahend the value to subtract from this {@link Movement} object
     * @return the sum of the two points
     */
    public Movement subtract(Movement subtrahend) {
        return add(subtrahend.negate());
    }

    /**
     * Multiplies this point's x, y, & theta by a given factor (doesn't modify existing points)
     * @param factor the other factor
     * @return the product of the two points
     */
    public Movement multiply(double factor) {
        return new Movement(x * factor, y * factor, theta * factor);
    }

    @NonNull
    @Override
    public String toString() {
        return "<" + x + ", " + y + ", " + theta + ">";
    }

    public Pose2d toPose() {
        return new Pose2d(x, y, theta);
    }

    public static Movement fromPose(Pose2d pose) {
        return new Movement(pose.getX(), pose.getY(), pose.getHeading());
    }
}
