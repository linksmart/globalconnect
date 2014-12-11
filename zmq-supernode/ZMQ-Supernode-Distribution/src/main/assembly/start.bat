echo starting isa-adapter...
@echo off
setLocal EnableDelayedExpansion
set CLASSPATH="
for /R ./lib %%a in (*.jar) do (
  set CLASSPATH=!CLASSPATH!;%%a
)
set CLASSPATH=!CLASSPATH!"
echo !CLASSPATH!
set CURRENTDIR="%cd%"
java -Dlog4j.debug=true -Dlog4j.configuration=file:%CURRENTDIR%/log4j.properties -client -cp %CLASSPATH% eu.linksmart.global.backbone.zmq.ProxyApplication %1 %2 %3

