package org.firstinspires.ftc.teamcode.modules.location;

import androidx.annotation.NonNull;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.util.ElapsedTime;
import org.firstinspires.ftc.teamcode.Movement;
import org.firstinspires.ftc.teamcode.modules.DriveTrain;

import java.util.function.BiFunction;

/**
 * A drive-to-position variant of {@link DriveTrain} that uses a
 *  Proportional, Derivative, and Integral system to control the robot
 */
public class PIDController extends Odometry { // TODO TUNE THE PID CONTROLLER
    public static final class PIDConfig {
        public final double proportionalCoefficient;
        public final double integralCoefficient;
        public final double derivativeCoefficient;
        public final BiFunction<Double, Double, Double> feedForwardSupplier;
        public final double integralSumLimit;
        public final boolean useIntegralSumLimit;
        public final double lowPassFilter;
        public final boolean useLowPassFilter;

        public final double minimumAbsPower;

        public PIDConfig(double p, double i, double d, BiFunction<Double, Double, Double> feedForwardSupplier, double minimumAbsPower, double integralSumLimit, double lowPassFilter) {
            proportionalCoefficient = p;
            integralCoefficient = i;
            derivativeCoefficient = d;
            this.feedForwardSupplier = feedForwardSupplier;
            this.minimumAbsPower = minimumAbsPower;
            this.integralSumLimit = integralSumLimit;
            useIntegralSumLimit = true;
            if (lowPassFilter <= 0 || lowPassFilter >= 1) {
                throw new RuntimeException("Low Pass must be > 0 and < 1!");
            }
            this.lowPassFilter = lowPassFilter;
            useLowPassFilter = true;
        }
        public PIDConfig(double p, double i, double d, double minimumAbsPower, double integralSumLimit, double lowPassFilter) {
            this(p, i, d, (a, b) -> 0.0, minimumAbsPower, integralSumLimit, lowPassFilter);
        }

        public PIDConfig(double p, double i, double d, BiFunction<Double, Double, Double> feedForwardSupplier, double minimumAbsPower) {
            proportionalCoefficient = p;
            integralCoefficient = i;
            derivativeCoefficient = d;
            this.feedForwardSupplier = feedForwardSupplier;
            this.minimumAbsPower = minimumAbsPower;
            integralSumLimit = 0;
            lowPassFilter = 0;
            useIntegralSumLimit = false;
            useLowPassFilter = false;
        }
        public PIDConfig(double p, double i, double d, double minimumAbsPower) {
            this(p, i, d, (a, b) -> 0.0, minimumAbsPower);
        }
    }

    /**
     * The proportional coefficient
     */
    public static final double KP = 0.1;

    /**
     * The derivative coefficient
     */
    public static final double KD = 0;

    /**
     * The integral coefficient
     */
    public static final double KI = 0;

    /**
     * The minimum power that will actually rotate the motor
     */
    public static final double MINIMUM_ABS_POWER = 0.1;

    /**
     * The maximum power the drive train can provide.
     * This value is used for Integrator clamping
     */
    public static final double INTEGRAL_SUM_LIMIT = 1;

    /**
     * Used in the low-pass filter for the derivative term. <br />
     * <b>MUST BE BETWEEN 0 AND 1 (exclusive)</b>
     */
    public static final double A = 0.8;

    @Override
    public void log() {
        super.log();
    }

    /**
     * Attempts to initialize the module by getting motors with the default names from a hardware map
     *
     * @param registrar the OpMode that will be using the module
     */
    public PIDController(@NonNull OpMode registrar) {
        super(registrar);
    }

    /**
     * Internal class for PID data
     */
    public static final class MovementInfo {
        public double error;
        public double derivative;
        public double integralSum;
        public double filterEstimate;

        public MovementInfo() {
            error = 0;
            derivative = 0;
            integralSum = 0;
            filterEstimate = 0;
        }
    }

    /**
     * Moves the robot to the target position
     * @param target The target position
     */
    public void driveTo(LocalizedMovement target) {
        if (target == null) { return; }

        final PIDConfig config = new PIDConfig(
                KP,
                KI,
                KD,
                MINIMUM_ABS_POWER,
                INTEGRAL_SUM_LIMIT,
                A
        );

        Locator locator = target.getLocator();
        if (locator.getKind() == LocatorKind.NO_ABSOLUTE_POSITION) {
            throw new NoAbsolutePositionException(locator);
        }

        final LocalizedMovement odomTarget = target.convertToOtherLocator(this);

        MovementInfo infoX = new MovementInfo();
        MovementInfo infoY = new MovementInfo();
        MovementInfo infoRotation = new MovementInfo();
        Movement velocity = Movement.zero();
        LocalizedMovement currentPosition;
        ElapsedTime timer = new ElapsedTime();
        double deltaTime;

        do {
            if (!locator.isActive() // if the given locator fails, default to odometry
                    && !locator.equals(this) // if odometry is failing, let getLocation() fail so we pass the error on
            ) {
                driveToOdometry(odomTarget);
                return;
            }

            deltaTime = timer.seconds();
            currentPosition = locator.getLocation();
            velocity.x = calcVelocity(config,
                    currentPosition.x,
                    target.x,
                    infoX,
                    deltaTime);
            velocity.y = calcVelocity(config,
                    currentPosition.y,
                    target.y,
                    infoY,
                    deltaTime);
            velocity.theta = calcVelocity(config,
                    currentPosition.theta,
                    target.theta,
                    infoRotation,
                    deltaTime);
            setVelocity(velocity);
            timer.reset();
        } while (!velocity.isZero());
    }

    public void driveToOdometry(Movement target) {
        driveTo(LocalizedMovement.construct(target, this));
    }

    public static double calcVelocity(PIDConfig config,
            double currentPosition,
            double targetPosition,
            MovementInfo info,
            double deltaTime) {
        double lastError = info.error;
        info.error = targetPosition - currentPosition;

        if (config.useLowPassFilter) {
            // filter out height frequency noise to increase derivative performance
            double errorChange = (info.error - lastError);
            info.filterEstimate = (config.lowPassFilter * info.filterEstimate) + (1 - config.lowPassFilter) * errorChange;

            // rate of change of the error
            info.derivative = info.filterEstimate / deltaTime;
        }
        else {
            info.derivative = (info.error - lastError) / deltaTime;
        }

        // sum of all error over time
        info.integralSum += info.error * deltaTime;

        if (config.useIntegralSumLimit) {
            // set a limit on our integral sum
            if (info.integralSum > config.integralSumLimit) {
                info.integralSum = config.integralSumLimit;
            } else if (info.integralSum < -config.integralSumLimit) {
                info.integralSum = -config.integralSumLimit;
            }
        }

        final double ff = config.feedForwardSupplier.apply(currentPosition, targetPosition);

        double power = (config.proportionalCoefficient * info.error) + (config.integralCoefficient * info.integralSum) + (config.derivativeCoefficient * info.derivative) + ff;
        return Math.abs(power - ff) < config.minimumAbsPower ? ff : power;
    }
}
