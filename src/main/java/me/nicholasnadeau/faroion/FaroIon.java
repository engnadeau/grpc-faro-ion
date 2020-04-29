package me.nicholasnadeau.faroion;

import org.apache.commons.math3.geometry.euclidean.threed.SphericalCoordinates;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import smx.tracker.*;

import java.util.logging.Logger;

/**
 * Created by nicholas on 2016-03-31.
 */
public class FaroIon {

    private static final String DEFAULT_IP = "192.168.0.2";
    private static final String USERNAME = "user";
    private static final String PASSWORD = "";
    private final Tracker tracker;
    private static final Logger LOGGER = Logger.getLogger(FaroIon.class.getName());


    /**
     * Default constructor.
     *
     * @throws TrackerException
     */
    FaroIon() throws TrackerException {
        this(new TrackerKeystone());
    }


    /**
     * Constructor.
     *
     * @param trackerInterface
     * @throws TrackerException
     */
    private FaroIon(TrackerInterface trackerInterface) throws TrackerException {
        this.tracker = new Tracker(trackerInterface);
        this.setBlocking(true);
    }


    /**
     * Gets the Tracker object used by the FARO API.
     *
     * @return
     */
    Tracker getTracker() {
        return this.tracker;
    }


    /**
     * The move method commands the tracker to point to the tracker to the specified location.
     * <p>
     * If a target is found at this location and tracker is turned on, the tracker will track to the center of the target.
     * <p>
     * The unit of measure for X, Y, and Z is meters.
     *
     * @param xyz {x, y, z}, meters
     * @throws TrackerException
     */
    void moveCartesian(double[] xyz) throws TrackerException {
        this.getTracker().move(xyz[0], xyz[1], xyz[2], false, false);
    }


    /**
     * The move method commands the tracker to point to the tracker to the specified location.
     * <p>
     * If a target is found at this location and tracker is turned on, the tracker will track to the center of the target.
     * <p>
     * The unit of measure for X, Y, and Z is meters.
     * <p>
     * The input parameters are added to the current location to obtain the new location where the tracker should point.
     *
     * @param xyz {x, y, z}, meters
     * @throws TrackerException
     */
    void moveCartesianRel(double[] xyz) throws TrackerException {
        this.getTracker().move(xyz[0], xyz[1], xyz[2], false, true);
    }


    /**
     * The move method commands the tracker to point to the tracker to the specified location.
     * <p>
     * If a target is found at this location and tracker is turned on, the tracker will track to the center of the target.
     * <p>
     * The unit of measure for the azimuth and zenith angles is radians. The unit of measure for the distance is meters.
     * <p>
     * The input parameters are added to the current location to obtain the new location where the tracker should point.
     *
     * @param azr {azimuth, zenith, distance}, radians and meters
     * @throws TrackerException
     */
    void moveSpherical(double[] azr) throws TrackerException {
        this.getTracker().move(azr[0], azr[1], azr[2], false);
    }


    /**
     * The move method commands the tracker to point to the tracker to the specified location.
     * <p>
     * If a target is found at this location and tracker is turned on, the tracker will track to the center of the target.
     * <p>
     * The unit of measure for the azimuth and zenith angles is radians. The unit of measure for the distance is meters.
     * <p>
     * The input parameters are added to the current location to obtain the new location where the tracker should point.
     *
     * @param azr {azimuth, zenith, distance}, radians and meters
     * @throws TrackerException
     */
    void moveSphericalRel(double[] azr) throws TrackerException {
        this.getTracker().move(azr[0], azr[1], azr[2], true);
    }


    /**
     * The home method commands the tracker to point to the home location on the tracker.
     * The tracker uses the current target type setting to determine which home position to use.
     *
     * @throws TrackerException
     */
    void home() throws TrackerException {
        this.getTracker().home(false);
    }


    /**
     * The search method commands the tracker to run a spiral search from its current location to find a target.
     * If a target is found at this location and tracking is on, the tracker will track to the center of the target.
     * <p>
     * The working distance must be within about 20% of the actual distance to the target.
     *
     * @param radius meters
     * @throws TrackerException
     */
    void search(double radius) throws TrackerException {
        this.getTracker().search(radius);
    }


    /**
     * Calls measurePoint(10).
     *
     * @return
     * @throws TrackerException
     */
    double[] measurePoint() throws TrackerException {
        return this.measurePoint(1, 3);
    }


    /**
     * The azimuth and zenith methods return the two angles for the target location.
     * <p>
     * The unit of measure is radians. The values are normalized to the range of ±pi.
     * The distance method returns the radial distance to the target. The unit of measure is meters.
     * <p>
     * Note, if the status indicates that the data is in error, all three of these methods will return 0.0.
     *
     * @param samples number of sample measurements to average for one data set
     * @return
     * @throws TrackerException
     */
    double[] measurePoint(int samples, int timeout) throws TrackerException {
        // set up and start measure
        MeasureCfg measureCfg = new MeasureCfg(
                samples,
                new AverageFilter(),
                new NullStartTrigger(),
                new NullContinueTrigger()
        );
        this.getTracker().startMeasurePoint(measureCfg);

        // read data
        double[] result = new double[]{
                this.getTracker().readMeasurePointData().azimuth(),
                this.getTracker().readMeasurePointData().zenith(),
                this.getTracker().readMeasurePointData().distance()
        };

        // close measurement connection
        this.getTracker().stopMeasurePoint();

        return result;
    }

    private boolean isConnected() {
        try {
            return this.getTracker().connected();
        } catch (TrackerException e) {
            return false;
        }
    }

    public boolean isTargetDetected() throws TrackerException {
        return this.getTracker().targetLocationValid() && this.getTracker().targetPresent();
    }

    public boolean isBusy() {
        return this.getTracker().busy();
    }

    public boolean isBlocking() {
        return this.getTracker().getBlocking();
    }

    void setBlocking(boolean isBlocking) throws TrackerException {
        this.getTracker().setBlocking(isBlocking);
    }

    void disconnect() throws TrackerException {
        this.getTracker().disconnect();
    }

    public double[] sphericalToCartesian(double[] azr) {
        SphericalCoordinates sphericalCoordinates = new SphericalCoordinates(
                azr[2],
                azr[0],
                azr[1]
        );

        return sphericalCoordinates.getCartesian().toArray();

    }

    public double[] cartesianToSpherical(double[] xyz) {
        SphericalCoordinates sphericalCoordinates = new SphericalCoordinates(new Vector3D(
                xyz));
        return new double[]{
                sphericalCoordinates.getTheta(),
                sphericalCoordinates.getPhi(),
                sphericalCoordinates.getR()
        };
    }

    /**
     * If the MCU firmware supports the smart initialze feature, the initialize smart method commands the tracker to run its smart initialization sequence.
     * Otherwise, it commands the tracker to run its initialization sequence.
     * <p>
     * The smart initialization sequence allow the tracker skip steps that are not necessary to complete initialization.
     * The initializeSmart method will significantly reduce initialization time in cases where the tracker was already initialized.
     *
     * @throws TrackerException
     */
    void initialize() throws TrackerException {

        if (!this.isInitialized()) {
            this.getTracker().initializeSmart();
        }
    }


    /**
     * This method is used to return air temperature, air pressure, and humidity to the user.
     * <p>
     * The air temperature in degrees C.
     * <p>
     * The air pressure in mmHg.
     * <p>
     * The humidity as a percentage.
     *
     * @return
     * @throws TrackerException
     */
    public WeatherInformation getWeatherInfo() throws TrackerException {
        return this.getTracker().getWeatherInfo();
    }


    /**
     * The unit of measure for the temperature is degrees Celsius.
     *
     * @return
     * @throws TrackerException
     */
    double getExtTemperature() throws TrackerException {
        return this.getTracker().measureExternalTempSensor(1);
    }

    boolean isInitialized() throws TrackerException {
        return this.getTracker().initialized();
    }


    /**
     * Once a tracker object is created, a connection must be established to the tracker itself before most of the other methods in the Tracker class can be called.
     * This is done using the connect method.
     *
     * @param ip
     * @throws TrackerException
     */
    private void connect(String ip) throws TrackerException {
        this.getTracker().connect(ip, USERNAME, PASSWORD);
    }

    /**
     * Once a tracker object is created, a connection must be established to the tracker itself before most of the other methods in the Tracker class can be called.
     * This is done using the connect method.
     *
     * @throws TrackerException
     */
    void connect() throws TrackerException {
        this.connect(DEFAULT_IP);
    }
}
