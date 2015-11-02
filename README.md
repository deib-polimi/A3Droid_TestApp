# A3Droid Test Applications: Greenhouse

Currently alljoyn framework only supports wi-fi as a stable connection method. Bluetooth has been disabled and wi-fi direct is still experimental. Make sure all devices are in the same wi-fi local area network.

## Required libraries

* [Alljoyn for Android](https://allseenalliance.org/framework/download) .jar and the .so - armeabi folder, in case of devices ~~and arme VDM, x86 folder, in case of x86 VDM~~
* A3Droid .jar
* Android v4 .jars

## Experiment steps

* Select one device to be the server
* Select one or more devices to be the sensors and the actuators
* For each device
  * Open the application
  * Click in **Start [Server|Sesor|Actuator]** button.

* Make sure the server is started before any sensor or actuator.
* Click in the **Start Experiment** button.
* Check the log messages at the application interface of the server or any other device
* Click in the **Stop Experiment** button whenever you want to finish it.
* Check the A3Droid_Greenhouse_*NUMBER_OF_DEVICES*_*NUMBER_OF_GROUPS* file at the *Virtual external disk* folder of the server device with all experiment measurements.
