#!/bin/sh

JAVA_HOME=`/usr/libexec/java_home`
BASEDIR=`pwd`
VERSION=`sed -n -e 's/.*VERSION = \"\(.*\)\".*/\1/p' < $BASEDIR/src/com/thomaskuenneth/monicopy/Main.java`

echo "JAVA_HOME: $JAVA_HOME"
echo "BASEDIR: $BASEDIR"
echo "Version: $VERSION"

rm -rf "$BASEDIR/MoniCopy.app"

$JAVA_HOME/bin/jpackage --name MoniCopy --icon $BASEDIR/artwork/MoniCopy.icns --app-version $VERSION --type app-image --module-path $BASEDIR/dist/MoniCopy.jar:/Library/Java/javafx-jmods-11.0.2 -m main/com.thomaskuenneth.monicopy.Main
