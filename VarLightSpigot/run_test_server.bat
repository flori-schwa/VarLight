@echo off
del test-servers\%1\plugins\VarLight-*.jar
copy /Y dist\*.jar test-servers\%1\plugins\
cd test-servers\%1\
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar spigot-%1.jar nogui
cd ..\..

if "%2"=="-ide" goto :EOF

pause