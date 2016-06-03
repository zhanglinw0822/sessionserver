@echo off
setLocal enableDelayedExpansion
set cp=.
FOR %%i IN ("%~dp0libs\*.jar") DO set cp=!cp!;%%~fsi
start "sessionserver" java -server -cp %cp% tpme.PMES.timebargain.server.ServerShell
