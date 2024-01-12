# King under the Mountain

King under the Mountain was developed by Rocket Jump Technology, and has now been released as open-source under the very permissive MIT license.

## Build / Framework

This game is written in Java (currently version 17) using [LibGDX](https://github.com/libgdx/libgdx/wiki)

LibGDX uses Gradle as a build tool, so you'll need to run `gradlew build` at least once (though not for general development).

The `core` module contains the main game code and assets, the `desktop` module is the desktop launcher and platform-specific binaries.

### Desktop Module

The source contains two classes with runnable main methods, `DesktopLauncher` and `RunTexturePacker`.
**Both of these classes expect to be run where the `assets` directory exists, i.e. they need to be run with `./core` as the working directory.**


`RunTexturePacker` reads the source asset files in (from current working directory) `./mods/base` and packages them into `./assets`.
This process has already been run and this repository contains the output files, it only needs to be re-run when there is a change to asset files.

`DesktopLauncher` is the game launcher. Run this with `./core` as the working directory.

## Building for release

To package for release, you need to download AdoptOpen JDK releases from https://adoptopenjdk.net/releases.html)
for the packing process (packr.jar) to use. You will likely need to change the packr config files from
OpenJDK17U-jdk_x64_windows_hotspot_17.0.2_8 to whatever version you have downloaded.

