# King under the Mountain

Note this is a private repository for the game code and assets.

## Setup

Download and extract https://drive.google.com/file/d/1P1C5OzH_slQEUsJXPO43s8YLyXSHBFBQ/view?usp=sharing
to release_tools/jdks (or alternatively download AdoptOpen JDK releases from https://adoptopenjdk.net/releases.html) 
for the packing process (packr.jar) to use. 


## Build / Framework

This is written in Java using [LibGDX](https://github.com/libgdx/libgdx/wiki)

LibGDX uses Gradle as a build tool, so you'll need to run `gradlew build` at least once (though not for general development).

The `core` module contains the main game code and assets, the `desktop` module is the desktop launcher and platform-specific binaries.

### Desktop Module

The source contains two classes with runnable main methods, `DesktopLauncher` and `RunTexturePacker`.
**Both of these classes expect to be run where the `assets` directory exists, i.e. they need to be run with `./core` as the working directory.**


`RunTexturePacker` reads the source asset files in (from current working directory) `./mods/base` and packages them into `./assets`.
This process has already been run and this repository contains the output files, it only needs to be re-run when there is a change to asset files.

`DesktopLauncher` is the game launcher. Run this with `./core` as the working directory.


