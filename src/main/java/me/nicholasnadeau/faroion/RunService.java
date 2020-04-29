package me.nicholasnadeau.faroion;

import smx.tracker.TrackerException;

import java.util.logging.Logger;

public class RunService {
    private static final Logger LOGGER = Logger.getLogger(RunService.class.getName());

    public static void main(String[] args) {
        // start grpc server
        LOGGER.info("Constructing service");
        try (Service service = new Service()) {
            LOGGER.info("Running serivce");
            service.run();
            service.blockUntilShutdown();
        } catch (TrackerException e) {
            LOGGER.severe(e.getText());
        } catch (InterruptedException e) {
            LOGGER.severe(e.getLocalizedMessage());
        }
    }
}
