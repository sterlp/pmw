```xml
    <properties>
        <pmw.version>2.x.x</pmw.version>
        <spt.version>2.x.x</spt.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.sterl.pmw</groupId>
            <artifactId>spring-pmw-core</artifactId>
            <version>${pmw.version}</version>
        </dependency>
        <!-- (optional) if the dashboard should be running too -->
        <dependency>
            <groupId>org.sterl.pmw</groupId>
            <artifactId>spring-pmw-ui</artifactId>
            <version>${pmw.version}</version>
        </dependency>
        <!-- if you setup the DB with liquibase -->
        <dependency>
            <groupId>org.sterl.spring</groupId>
            <artifactId>spring-persistent-tasks-db</artifactId>
            <version>${spt.version}</version>
        </dependency>
```
