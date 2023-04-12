About
-
이 프로젝트는 빠르고 쉽게 데이터베이스에 오브젝트를
저장함을 목적으로 합니다.

Build
-
Simply build the source with Maven:

    mvn install

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
