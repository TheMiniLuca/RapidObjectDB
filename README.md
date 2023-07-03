![Build Status](https://repo.roinz.xyz/api/badge/latest/snapshots/io/github/theminiluca/sql/RapidObjectDB?color=40c14a&name=RapidObjectDB)

About
-
이 프로젝트는 빠르고 쉽게 데이터베이스에 오브젝트를
저장함을 목적으로 합니다.

Use
-
You can implement this project with<br/>
Gradle:
```gradle
repositories {
    maven {
        name = "roinSnapshots"
        url = uri("https://repo.roinz.xyz/snapshots")
    }
}

dependencies {
    implementation("io.github.theminiluca.sql:RapidObjectDB:LATEST VERSION")
}
```
Maven:
```xml
<repositories>
    <repository>
        <id>roin-snapshots</id>
        <name>Roin Repository</name>
        <url>https://repo.roinz.xyz/snapshots</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>io.github.theminiluca.sql</groupId>
        <artifactId>RapidObjectDB</artifactId>
        <version>LATEST VERSION</version>
    </dependency>
</dependencies>
```

Build
-
Simply build the source with Maven:

    ./gradlew publishToMavenLocal

Migration to v2 SNAPSHOT!
-

```java
import io.github.theminiluca.rapidobjectdb.RapidSyncManager;

import java.util.concurrent.TimeUnit;

public class YourClass {
    public static RapidSyncManager rsm;

    public static void main(String[] args) {
        rsm = new RapidSyncManager(
                "localhost", // Hostname of the SQL Server
                3306, // Port of the SQL Server
                "database", // Name of Database
                "user", // Username
                "****" // Password
        );

        AnotherClass thatHasAMap = new AnotherClass();

        rsm.registerBackup(thatHasAMap, 5, TimeUnit.MINUTES);
        
        
        //Do Something..
        // ..
        // ..
        
        //After finishing task
        rsm.close();
    }
}
```

Code Example
-

```java
public static SQLSyncManager sqlManager;
public static void main(String[] args) {
                  sqlManager = new SQLSyncManager(ConfigManager.Option.getOption(DATABASE_HOST) 
                     , ConfigManager.Option.getOption(DATABASE_PORT), 
                     <연결할 데이터베이스 이름>, ConfigManager.Option.getOption(DATABASE_USER),ConfigManager.Option.getOption(DATABASE_PASSWORD)); 
             try { 
                 sqlManager.connectSQL(); 
             } catch (Exception e) { 
                 e.printStackTrace(); 
                 System.out.println("데이터베이스 연결 실패."); 
              
                 return; 
             }
}
```
