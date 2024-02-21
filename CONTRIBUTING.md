# How to release a new version
1. Check out the project, open it in Intellij IDEA
2. Check that plugin works correctly in the latest stable version of Android Studio:
   1. Set the `platformVersion` to the stable version from [this list](https://plugins.jetbrains.com/docs/intellij/android-studio-releases-list.html).
   2. Check TODOs related to platform version upgrades
   3. Launch the plugin by running "Run Plugin" configuration (`./gradlew :runIde`)
3. Increment `pluginVersion` in [gradle.properties](gradle.properties)
4. Run `./gradlew :buildPlugin`
5. Upload `build/libs/Kelp-{version}.jar` and to the new GitHub release.