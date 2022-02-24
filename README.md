# IBM z/OS Connect Gradle Plugin
## Overview
This plugin is used within the z/OS Connect Designer `build.gradle` file and is used to build a war file.

## How To Configure

Add the plugin to your `build.gradle` file.

```
plugins {
    id 'com.ibm.zosconnect.gradle'
}
```

This plugin can be configured using a DSL block within the `build.gradle` file, both `archiveFileName` and `archiveVersion` are configurable.

```
zosConnectApi {
    archiveFileName = "customFileName.war"
    archiveVersion = "1.0"
}
```
