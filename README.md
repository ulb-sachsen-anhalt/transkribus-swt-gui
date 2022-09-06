# TranskribusSwtGui / Transkribus expert client

[![Java CI with Maven](https://github.com/ulb-sachsen-anhalt/transkribus-swt-gui/actions/workflows/main.yml/badge.svg)](https://github.com/ulb-sachsen-anhalt/transkribus-swt-gui/actions/workflows/main.yml)

SWT based Java-GUI-Application for Transkribus platform forked from [TranskribusSwtGui / Transkribus expert client / TranskribusX](https://gitlab.com/readcoop/transkribus/TranskribusSwtGui) (formerly: [TranskribusSwtGui](https://github.com/Transkribus/TranskribusSwtGui)).

Features enhanced filename-matching and fixed XSLT-Transformations from ALTO (3+4) to custom Transkribus PAGE2013-Flavour.

## Prerequisities

* OpenJDK 11+
* Maven 3.6+

## Installation with Dependencies

Please open Terminal and create a _fresh_ clone:

```bash
git clone https://github.com/ulb-sachsen-anhalt/TranskribusSwtGui <local-path>
```

_Please note_:
To update existings forks of _this_ Repository, it is required to do `git pull origin ulb/master --force --rebase`.  
I needed to remove binary artifacts from repository history just to be able to push the latest changes from [TranskribusSwtGui / Transkribus expert client / TranskribusX](https://gitlab.com/readcoop/transkribus/TranskribusSwtGui) Repository. Dropping those items resulted in a complete divergation of the history.

To be able to build Transkribus default and Windows64 Package (which includes a fallback JRE), it is required to have required Dependencies in proper version at hand.  
Build additional Transkribus libraries on your own in the requested version, i.e. currently TranskribusCore:0.14.19 and TranskribusClient:0.3.19.

```bash
git clone --branch 0.14.19 https://gitlab.com/readcoop/transkribus/TranskribusCore.git
cd TranskribusCore && mvn clean install -DskipTests
cd ..
git clone --branch 0.3.19 https://gitlab.com/readcoop/transkribus/TranskribusClient.git
cd TranskribusClient && mvn clean install -DskipTests
```

Additionally, please download the configured Windows64 OpenJDK-JRE (see: `pom.xml`) manually from [https://www.openlogic.com/openjdk-downloads](https://builds.openlogic.com/downloadJDK/openlogic-openjdk-jre/11.0.16+8/openlogic-openjdk-jre-11.0.16+8-windows-x64.zip) and place the archive file in subdir `jre`.  
This is mandatory for Application assembly.

Further, configured SWT-Binaries (see: `pom.xml`) must be downloaded and placed inside `swt` directory.  
They can be found at [Eclipse Downloads](https://archive.eclipse.org/eclipse/downloads/). Go there and select, for example, both SWT-Binaries for [Version 4.22](https://archive.eclipse.org/eclipse/downloads/drops4/R-4.22-202111241800/), extract the archives and rename the jars for each plattform.

Finally, go and execute Maven to start the build

```bash
cd <local-path>
mvn deploy -DskipTests
```

This will create ZIP-Packages for Linux and Windows inside the project's `target` dir, _if and only if_ all required libraries and archives are available.

_Please note_:

* No connection to Transkribus Backend for training supported.
  Just a plain OCR-Editor
* Only bare Package and Window64-Package included.  
  If you're looking for different Distributions, please check out the official [TranskribusSwtGui / Transkribus expert client / TranskribusX](https://gitlab.com/readcoop/transkribus/TranskribusSwtGui) Repository
* SWT-Support on Linux dependes both on OS and SWT-Version.  
  On 20.04 LTS Desktop latest known working Version is 4.17, where on 22.04 LTS Desktop it's SWT 4.22
