# Adobe AEM Tools which contains following utilities

1.page assets references packager

Find all asset references in a page or under content folder(s), and create package definition in package manager

Installing
--------


a.Source Code Building via maven.
--------

This project uses Maven for building. Common commands:

From the root directory, run ``mvn -PautoInstallPackage clean install`` to build the bundle and content package and install to a CQ instance.

From the bundle directory, run ``mvn -PautoInstallBundle clean install`` to build *just* the bundle and install to a CQ instance.

b.Install pre-compiled package.
--------

1) Download the package[aem-tools-content-1.0.zip] which is placed at the root folder.

2) Install this package to your instance via package manager.

3) Open packager page[http://localhost:4502/cf#/etc/aem-tools/page-asset-reference-packager.html]

4) Complete the configuration, click "create package" or "preview", you will get the corresponding results. 




