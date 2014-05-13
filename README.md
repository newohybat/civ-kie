civ-kie
=======

Project containing definitions and tests of rules and processes implemented with jBPM 6.0 and Drools 6.0

**purpose:**

Project defines a kmodule which contains all the definitions of rules and processes. After installing into local repository the kmodule is available to be injected.

**requirements:**

* dependency to civ-persistence

**usage:**

mvn install

after that following dependency is available

```xml
<dependency>
    <groupId>cz.muni.fi.civ</groupId>
    <artifactId>civ-kie</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```
