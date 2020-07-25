#!/bin/sh

$JAVA_HOME = "C:\Program Files\Java\jdk-14.0.2"
$BASEDIR = "C:\Users\tkuen\Entwicklung\GitHub\MoniCopy"
$VERSION = "1.0.0" # `sed -n -e 's/.*VERSION = \"\(.*\)\".*/\1/p' < $BASEDIR/src/com/thomaskuenneth/monicopy/Main.java`

Write-Output "JAVA_HOME: $JAVA_HOME"
Write-Output "BASEDIR: $BASEDIR"
Write-Output "Version: $VERSION"

#rmdir -r "$BASEDIR\MoniCopy"

$COMMAND = "$JAVA_HOME\bin\jpackage.exe"
$ARGUMENTS = "--win-menu --win-menu-group `"Thomas Kuenneth`" --vendor `"Thomas Kuenneth`" --name MoniCopy --icon $BASEDIR\artwork\MoniCopy.ico --type msi --app-version $VERSION --module-path $BASEDIR\dist\MoniCopy.jar;`"C:\Program Files\Java\javafx-jmods-13.0.2`" -m main/com.thomaskuenneth.monicopy.Main"

echo $ARGUMENTS

Start-Process -RedirectStandardOutput stdout.txt -RedirectStandardError stderr.txt -FilePath $COMMAND -ArgumentList $ARGUMENTS -Wait