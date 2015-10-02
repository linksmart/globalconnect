echo starting de.fhg.e3.dc.opc...
@echo off
setLocal EnableDelayedExpansion
set CLASSPATH="
for /R ./lib %%a in (*.jar) do (
  set CLASSPATH=!CLASSPATH!;%%a
)
set CLASSPATH=!CLASSPATH!"
echo !CLASSPATH!
java -client -cp %CLASSPATH% eu.linksmart.gc.main.MainApp