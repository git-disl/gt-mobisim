# gt-mobisim

This is a mirror of https://code.google.com/archive/p/gt-mobisim/

Simulator for generating mobility traces and query traces for large numbers of mobile agents moving in a road network.

Check the `wiki` branch for documentation.

### How to get gt-mobisim, along with source code, sample simulation configs & maps?

1. QuickStart
2. FAQ

### Example codes for using & extending the simulator

ExampleCode

### Demo

Playback of pre-generated simulation with http://code.google.com/p/gt-mobisim/source/browse/trunk/configs/web-demo.xml this configuration xml.
http://gt-mobisim.googlecode.com/svn/trunk/jnlp/gtmobisim.jnlp
http://gt-mobisim.googlecode.com/svn/trunk/jnlp/img/button0.png
http://gt-mobisim.googlecode.com/svn/trunk/jnlp/img/screenshot.png

## Features

### Does:

1. generate mobility traces for mobile objects moving in a road network
    100k objects, 10 minutes, flowing traffic speeds, velocity step-function representation, continuous time (no sampling) => ~235 MB trace

2. simulation driven by an xml configuration file
    can be run as a text-mode only batch job (eg. on ssh-access-only Linux box)
    simple GUI, when running on graphical terminal

3. vector map formats:
    ESRI Shapefile (.shp), eg. U.S. Census Bureau, TIGER/Line Shapefiles http://www.census.gov/geo/www/tiger/
    GlobalMapper-exported USGS data (.svg), see: http://edc2.usgs.gov/geodata/index.php 

4. various mobility models on road networks
    random waypoint
    random trip

5. 3 ways to represent continuous-time traces
    location step-function
    velocity step-function
    acceleration step-function

6. locations of mobile objects at any time instance (ie. continuous vs. sampled locations)

7. conversion from continuous-time traces to periodically sampled location traces

8. ability to specify various parameter distributions
    Gaussian distribution with mean & standard deviation + min & max cutoff
    normal distribution with min & max

9. generate query traces
    query creation
    query deletion

### Doesn't do:

execution of client-side codes (ie. code that runs on simulated mobile users' phones)
have a simulated server, or have simulated client-server communication
