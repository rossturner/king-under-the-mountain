@echo OFF
@echo Remember to have run the gradle build first so /dist is up to date
@echo:
@echo Packaging win build
rm -rf win64_release
java -jar packr.jar win-config.json
rcedit-x64 "win64_release/King under the Mountain.exe" --set-icon "icon/favicon.ico"
@echo Packaging mac build
rm -rf macosx_release
java -jar packr.jar mac-config.json
@echo Packaging linux build
rm -rf linux64_release
java -jar packr.jar linux-config.json