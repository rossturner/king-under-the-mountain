#!/usr/bin/env bash
rm -rf win64_release
java -jar packr.jar win-config.json
rm -rf macosx_release
java -jar packr.jar mac-config.json
rm -rf linux64_release
java -jar packr.jar linux-config.json
