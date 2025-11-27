# How to release a new version

1. Check out the project, open it in Intellij IDEA
2. Check that plugin works correctly in the latest stable version of Android Studio:
    1. Set the `platformVersion` to the stable version
       from [this list](https://plugins.jetbrains.com/docs/intellij/android-studio-releases-list.html).
    2. Check TODOs related to platform version upgrades
    3. Launch the plugin by running "Run Plugin" configuration (`./gradlew :runIde`)
3. Increment `pluginVersion` in [gradle.properties](gradle.properties)
4. Run this [GitHub Action](https://github.com/ozontech/kelp/actions/workflows/publish-ide-plugin.yml)
5. Add a changelog to the new GitHub release in the GitHub UI.

## How to introduce a breaking change to `config.json`

If it's required to change the scheme of `config.json` in a way that when an older plugin version reads the new config
it will crash, you need to do the following:

1. Add ```version: 2``` field to the top of the new `config.json` (assuming the current **major** plugin version is 1) and
   make it optional.
2. Create a check that:
   1. Reads this field **before parsing the rest** of the json file.
   2. Assumes that if there is no `version` field, the `version` is 1
   3. If plugin's current **major** version doesn't match the `version`:
      1. Displays a notification that says to follow the migration guide and links to it
      2. Stops the rest of the config parsing.
