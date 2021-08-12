@echo off
cd VarLightCore
call mvn clean package
cd ..\VarLightSpigot
call ant
cd ..