@echo off
set taskfile=%1
java -classpath ".;./lib/*" DeleteTask %taskfile%
echo This program is finished.
