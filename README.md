# TestingTools
JCServer is a testing tool, which provides a way to communicate with 
JCardSimulator/JCOPSimulator from android emulator/device.
It basically opens a socket connection on the port(8080)
and listens for the incomming data on this port. This tool uses apduio and JCardsim jars
to validate and transmit the APDUs to the Keymaster Applet. It also uses OpenCard Framework
to test with JCOP simulator.

### Build
Import JCServer server application either in Eclipse or IntelliJ. Add the provided jars inside
lib/ directory and also add precompiled applets .cap and *jar file for jcop and 
jcarsim respectively inside ExtBinaries/ directory. ExtBinaries is the default cap files path. Users may pass the optional external cap file path as an argument to the application.

### Program Arguments
Program takes two or three arguments
- Simulator type either 'jcop' or 'jcardsim'
- Packages to install either single or multiple 
- Optional cap files path. (If not provided, ExtBinaries becomes the default cap file path)

Example to install multiple packages
<pre>
jcop
keymaster,weaver,fira,ic
</pre>
Example to install single package
<pre>
jcop
keymaster
</pre>
Example to install multiple packages with custom cap path
<pre>
jcop
keymaster,weaver,fira,ic
&lt;path_to_cap_files&gt;
</pre>

