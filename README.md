# FARO ION gRPC

gRPC wrapper and service for interacting with a FARO ION laser tracker

https://knowledge.faro.com/Hardware/Laser_Tracker/Tracker/Software_Download_of_FARO_Utilities_for_the_Laser_Tracker

## Usage

### FARO JARs

The following JAR files are not included in this repo (as they are provided by the laser tracker software package) but are required:

- `Apps4xxx.jar`
- `AppsKeystoneSim.jar`
- `Ftp.jar`
- `Tracker.jar`
- `Utility.jar`

The files should be placed in a `faro-lib` subfolder, as is currently described in [`build.gradle`](build.gradle).
