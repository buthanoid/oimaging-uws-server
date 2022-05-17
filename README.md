# OImaging-uws-server    ![JMMC logo](doc/JMMC-logo.jpg)

This module contains the OImaging backend developed by the JMMC technical team.

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)


See [OImaging](https://www.jmmc.fr/oimaging)
See [JMMC releases](https://www.jmmc.fr/releases/)
See [JMMC OpenDev @ github](https://github.com/JMMC-OpenDev/)

See [CI nightly builds](https://github.com/JMMC-OpenDev/jmmc-java-build/actions/workflows/build.yml)


## Issues / Board

See the [OImaging Board](https://github.com/orgs/JMMC-OpenDev/projects/3) to see issues & activity.


## Documentation

This module contains the source code of the JMMC OImaging UWS server:
- java UWS server + ImageOI interface
- Dockerfile to install image reconstruction software including:
  - BSMEM
  - MIRA
  - SPARCO (base on MIRA)
  - WISARD

To be continued...


## Build

This JMMC module uses java / maven to build the war file, to build the OImaging UWS server,
and Docker to build the complete image including the image reconstruction software.

Requirements:
- OpenJDK 8+
- Maven 3.6+
- Docker

See [README](https://github.com/JMMC-OpenDev/oimaging-uws-server/blob/master/runtime/docker/oiservices/README.txt)

See [JMMC Java Build](https://github.com/JMMC-OpenDev/jmmc-java-build)
See [CI nightly builds](https://github.com/JMMC-OpenDev/jmmc-java-build/actions/workflows/build.yml)

