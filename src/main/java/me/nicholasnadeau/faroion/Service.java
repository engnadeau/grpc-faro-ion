package me.nicholasnadeau.faroion;


import com.google.protobuf.BoolValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Empty;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import me.nicholasnadeau.faroion.FaroIonServiceGrpc.FaroIonServiceImplBase;
import smx.tracker.TrackerException;

import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Logger;

class Service extends FaroIonServiceImplBase implements Runnable, Closeable {
    private static final Logger LOGGER = Logger.getLogger(Service.class.getName());
    private static final int DEFAULT_PORT = 30000;
    private FaroIon faroIon;
    private Server server;

    Service() throws TrackerException {
        this(new FaroIon(), DEFAULT_PORT);
    }

    private Service(FaroIon faroIon, int port) {
        this.faroIon = faroIon;
        server = ServerBuilder.forPort(port).addService(this).build();
    }

    public void close() {
        LOGGER.info("Closing FARO service");
        this.server.shutdown();
        try {
            this.faroIon.disconnect();
        } catch (TrackerException e) {
            LOGGER.severe(e.getText());
        }
    }


    void blockUntilShutdown() throws InterruptedException {
        LOGGER.info("Blocking until shutdown");
        this.server.awaitTermination();
    }

    @Override
    public void run() {
        try {
            startGrpcServer();
        } catch (IOException e) {
            LOGGER.severe(e.getLocalizedMessage());
        }

        try {
            connectFaro();
        } catch (TrackerException e) {
            LOGGER.severe(e.getText());
            this.close();
        }
    }

    private void connectFaro() throws TrackerException {
        LOGGER.info("Connecting to FARO");
        this.faroIon.setBlocking(true);
        this.faroIon.connect();
    }

    private void startGrpcServer() throws IOException {
        LOGGER.info("Starting gRPC server");
        this.server.start();
        LOGGER.info("Server started, listening on " + this.server.getPort());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may has been reset by its
            // JVM shutdown hook.
            System.err.println("Shutting down gRPC server since JVM is shutting down");
            Service.this.close();
            System.err.println("Server shut down");
        }));
    }


    @Override
    public void moveCartesian(CartesianPosition request, StreamObserver<Empty> responseObserver) {
        LOGGER.info("Move Cartesian to:\n" + request);
        try {
            this.faroIon.moveCartesian(new double[]{request.getX(), request.getY(), request.getZ()});
        } catch (TrackerException e) {
            LOGGER.severe(e.getText());
            this.close();
        }

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }


    @Override
    public void moveHome(Empty request, StreamObserver<Empty> responseObserver) {
        LOGGER.info("Moving home");
        try {
            this.faroIon.home();
        } catch (TrackerException e) {
            LOGGER.severe(e.getText());
            this.close();
        }
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void initialize(Empty request, StreamObserver<Empty> responseObserver) {
        LOGGER.info("Initializing FARO");
        try {
            if (this.faroIon.isInitialized()) {
                LOGGER.info("Tracker already initialized");
            } else {
                this.faroIon.initialize();
                LOGGER.info("Setting home");
                this.faroIon.home();
            }
        } catch (TrackerException e) {
            LOGGER.severe(e.getText());
            this.close();
        }

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void isTargetDetected(Empty request, StreamObserver<BoolValue> responseObserver) {
        boolean targetDetected = false;

        for (int i = 0; i < 2; i++) {
            try {
                targetDetected = this.faroIon.isTargetDetected();
                if (targetDetected) {
                    break;
                }
            } catch (TrackerException e) {
                LOGGER.severe(e.getText());
                this.close();
            }
        }

        LOGGER.info("Target detected: " + targetDetected);
        responseObserver.onNext(BoolValue.newBuilder().setValue(targetDetected).build());
        responseObserver.onCompleted();
    }

    @Override
    public void search(DoubleValue request, StreamObserver<Empty> responseObserver) {
        LOGGER.info("Searching for target");
        try {
            this.faroIon.search(request.getValue());
        } catch (TrackerException e) {
            LOGGER.severe(e.getText());
            this.close();
        }

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void measurePoint(Empty request, StreamObserver<Measure> responseObserver) {
        Measure measure = Measure.getDefaultInstance();
        try {
            // get position
            double[] doubles = this.faroIon.measurePoint();
            doubles = this.faroIon.sphericalToCartesian(doubles);
            CartesianPosition.Builder cartesianBuilder = CartesianPosition.newBuilder();
            cartesianBuilder
                    .setX(doubles[0])
                    .setY(doubles[1])
                    .setZ(doubles[2]);

            // get temperature
            double temperature = this.faroIon.getExtTemperature();

            // build message
            measure = Measure.newBuilder()
                    .setPosition(cartesianBuilder.build())
                    .setTemperature(temperature)
                    .setIsSuccess(true)
                    .build();
        } catch (TrackerException e) {
            LOGGER.severe(e.getText());
            this.close();
        }

        responseObserver.onNext(measure);
        responseObserver.onCompleted();
    }
}
