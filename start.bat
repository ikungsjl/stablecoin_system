@echo off
set JAVA_HOME=C:\Users\admin\.jdks\openjdk-22.0.1
set PATH=%JAVA_HOME%\bin;C:\maven\apache-maven-3.9.6\bin;%PATH%
cd /d C:\Users\admin\Desktop\stablecoin_system\collateral-service
echo Starting Spring Boot with OpenJDK 22...
C:\maven\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run -DskipTests > C:\Users\admin\Desktop\stablecoin_system\app.log 2>&1
