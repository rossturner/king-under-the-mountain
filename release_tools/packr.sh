#!/usr/bin/env bash
rm -rf win64_release
java -jar packr-all-4.0.0.jar win-config.json
rm -rf macosx_release
java -jar packr-all-4.0.0.jar mac-config.json
rm -rf linux64_release
java -jar packr-all-4.0.0.jar linux-config.json
