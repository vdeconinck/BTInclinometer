## "Remote" inclinometer for 4x4 (or other vehicles).

# What ?
Compared to other apps available, this one does not rely on the device's internal sensor, but on an external module.

# How ?
The module must be securely fixed to the vehicle and the app can then reliably show the vehicle's tilt and roll angles.
This app can of course be installed on phones or tablets, but also on Android head units.
Currently, the app only support WitMotion's modules, and tests have been performed with a BWT901CL.

#Details
This app was derived from WitMotion's demo application.

It is made of 3 modules (subprojects) :
 - Main (originally wtapp) : the main entry point of the app
 - Util (originally wtfile) : provides File handling (to save data) as well as a "SharedUtil" static class used by the main module 
 - BT901 : handles all sensor-related operations
