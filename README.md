# hy-plugin
## Installation:
Copy plugin jar [file](https://github.com/gagauz/hy-plugin/releases/download/v1.1.1/hybris_1.1.1.201610131136.jar) into the eclipse plugins folder.
## Usage:
Import hybris project via Import -> Hybris -> Import hybris platform.

The hybris project must have standard folder structure, i.g.:
    config/
    bin/
      custom/
      ext-.../
      plafrotm/

### You can choose what extensions to import into workspace:
1. All required extension (default).
2. Only _custom_ extensions.
3. All required except for _platform/ext/*_ extensions

Plugin will setup class output folders as it's required for hybris. Changes in class will be visible for running server. 
Using it with JRebel plugin allows of service live reloading and mvc controller remapping. Without JRebel only changes in method body are visible.

### Warning:
Plugins completely recreate .classpath and .project files. All natures and external builders will be removed except for following _javanature, jrebelNature, remotingNature_. All project specific preferences will be removed, so you should setup them for entire workspace.
