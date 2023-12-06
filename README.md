# ![icon](src/main/resources/META-INF/pluginIcon.svg) Kelp

![License](https://img.shields.io/github/license/popovanton0/Blueprint?color=blue)
<!-- ![Build](https://github.com/ozontech/kelp/workflows/Build/badge.svg) -->

<!-- Plugin description -->
Kelp is an Android Studio plugin that enhances support for **custom design systems**.

| Feature                                                                                                                                                            | Screenshot                                                                                                                                                                                                                                                                                                                           |
|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Replacing the default icon of <br>design system components in the <br>code completion with a customizable icon                                                     | <img src="https://github.com/ozontech/kelp/blob/57ca01bd5bab159b05906cced4fd9213c23d6492/images/componentFunHighlighting-light.png#gh-light-mode-only" width="600"><img src="https://github.com/ozontech/kelp/blob/57ca01bd5bab159b05906cced4fd9213c23d6492/images/componentFunHighlighting-dark.png#gh-dark-mode-only" width="600"> |
| Rendering design system icons <br>in the code completion and gutter <br>(where breakpoints are), like with regular Android resources                               | <img src="https://github.com/ozontech/kelp/blob/57ca01bd5bab159b05906cced4fd9213c23d6492/images/iconsRendering-light.png#gh-light-mode-only" width="600"><img src="https://github.com/ozontech/kelp/blob/57ca01bd5bab159b05906cced4fd9213c23d6492/images/iconsRendering-dark.png#gh-dark-mode-only" width="600">                     |
| Installing the apk file of <br>the demo app (showcase app) on an Android <br>device, as well as navigating to the component <br>page in it via an Intention Action | <img src="https://github.com/ozontech/kelp/blob/57ca01bd5bab159b05906cced4fd9213c23d6492/images/demoApkInstalling-light.png#gh-light-mode-only" width="600"><img src="https://github.com/ozontech/kelp/blob/57ca01bd5bab159b05906cced4fd9213c23d6492/images/demoApkInstalling-dark.png#gh-dark-mode-only" width="600">               |

These features enable users of your custom design system to develop UI **faster and easier**.
<!-- Plugin description end -->

## Customization
Plugin allows you to configure aforementioned features and adapt them to _your_ project by using `config.json` 
file (see below).

However, if your custom design system is part of a large project with unique requirements, it is _encouraged_ to fork
this repo and add/modify features needed only by your company's project.

Don't worry, it is **not at all hard** to do it! You can create many powerful and incredibly useful features to increase 
developer happiness and productivity. These are some of the resources that will help you on your journey:
1. [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
2. [Intellij IDEA GitHub repo](https://github.com/JetBrains/intellij-community) — use search to find examples of desired functionality
3. [The JetBrains Platform Slack community](https://plugins.jetbrains.com/slack) — ask the community

## Installation

1. Make sure that you are using **Android Studio Hedgehog 2023.1.1** or later
2. Download the [latest release](https://github.com/ozontech/kelp/releases/latest) and install it manually using
<kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>
3. _(Optional)_ If you want to notify developers about the need to install this plugin, 
create this file `/.idea/externalDependencies.xml`, add it to git, and paste this in the file:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="ExternalDependencies">
    <plugin id="ru.ozon.ideplugin.kelp" min-version="0.0.1" max-version="0.0.1" />
  </component>
</project>
```
You can read more about it [here](https://www.jetbrains.com/help/idea/managing-plugins.html#required-plugins).

4. Open the project you want to use this plugin with, create this file:
  `/.idea/kelp/config.json`, and add it to git.
5. Paste this content into it (without comments, they are unsupported in JSON):

<details>
<summary>Without comments</summary>

```json
{
  "componentFunHighlighting": {
    "functionFqnPrefix": "com.your.designsystem.package.components.",
    "functionSimpleNamePrefix": "Ds"
  },
  
  "iconsRendering": {
    "codeCompletionEnabled": true,
    "gutterEnabled": true,
    "containerClassName": "com.your.designsystem.package.DsIcons",
    
    "propertyNameFilter": {
      "startsWith": ["ic_"],
      "doesNotStartWith": ["allIconsAsList", "otherProperty"]
    },
    
    "propertyToResourceMapper": {
      "addPrefix": "ic_",
      "convertToSnakeCase": true
    }
  },
  "demoApp": {
    "intentionName": "🚀 Open in MY CUSTOM design system demo app",
    "functionFqnPrefix": "com.your.designsystem.package.components.",
    "functionSimpleNamePrefix": "Ds",
    "appPackageName": "com.your.designsystem.package.demo",
    "componentDeeplink": "yourscheme://component/DS_COMPONENT_FQN_DEEPLINK_PLACEHOLDER",
    "apkInstalling": {
      "latestVersion": {
        "file": "/gradle/libs.versions.toml",
        "regex": "demoApp=\"(?<version>[a-zA-Z0-9.-]+)\""
      }
    }
  }
}
```
</details>

```json5
{
  // If you want to disable some of these features, just don't their sections to your json file.
  
  // Replacing the default icon of design system components
  // in the code completion with a customizable icon
  // Custom icon MUST be 
  // 1. an svg 
  // 2. with size — 40x40
  // 3. placed here: /.idea/kelp/dsComponentFunIcon.svg
  // 4. optionally, a dark version can be added : /.idea/kelp/dsComponentFunIcon_dark.svg
  "componentFunHighlighting": {
    // custom icon will be added to all functions in this package
    "functionFqnPrefix": "com.your.designsystem.package.components.",
    "functionSimpleNamePrefix": "Ds" // optional
  },
  
  // Rendering design system icons in the code completion and gutter (where breakpoints are). 
  // Like with regular Android resources.
  "iconsRendering": {
    "codeCompletionEnabled": true,
    "gutterEnabled": true,
    // class with a lot of properties that return icons and are named as icons
    "containerClassName": "com.your.designsystem.package.DsIcons",
    
    // optional: filters out properties that do not represent an icon
    "propertyNameFilter": {
      // optional: only properties with this prefix will be considered as an icon
      "startsWith": ["ic_"],
      // optional: all properties with this prefix will be skipped
      "doesNotStartWith": ["allIconsAsList", "otherProperty"]
    },
    
    // maps property names to drawable resource names
    "propertyToResourceMapper": {
      "addPrefix": "ic_", // optional
      "convertToSnakeCase": true // optional; e.g. "AddAccount" -> "add_account"
    }
  },
  
  // Opening the component page in the dmo app via an Intention Action
  "demoApp": {
    // optional: custom name of the intention action
    "intentionName": "🚀 Open in MY CUSTOM design system demo app",
    "functionFqnPrefix": "com.your.designsystem.package.components.",
    "functionSimpleNamePrefix": "Ds", // optional
    // package name of the demo app
    "appPackageName": "com.your.designsystem.package.demo",
    // deeplink that will be used to open a component page in the demo app.
    // DS_COMPONENT_FQN_DEEPLINK_PLACEHOLDER will bw replaced with
    // the fully qualified name of the 
    // composable function, e.g. com.your.designsystem.package.components.Badge
    "componentDeeplink": "yourscheme://component/DS_COMPONENT_FQN_DEEPLINK_PLACEHOLDER",
    
    // optional
    // Installing (if not installed) the [android_studio_product_releases.xml](build%2Ftmp%2FdownloadAndroidStudioProductReleasesXml%2Fandroid_studio_product_releases.xml)ap[android_studio_product_releases.xml](build%2Ftmp%2FdownloadAndroidStudioProductReleasesXml%2Fandroid_studio_product_releases.xml)[android_studio_product_releases.xml](build%2Ftmp%2FdownloadAndroidStudioProductReleasesXml%2Fandroid_studio_product_releases.xml)k file
    // of the demo app (showcase app) on an Android device.
    
    // Demo app apk must be placed here: /.idea/kelp/demoApp-0.12.0.apk
    // Plugin will acquire the latest version from apkInstalling.latestVersion.file (for example, 0.12.0)
    // and install an apk file with that version (from a path above) on the device.
    "apkInstalling": {
      "latestVersion": {
        // looks in this file
        "file": "/gradle/libs.versions.toml",
        // for this regex. Text in the named group "version" 
        // MUST contain "versionName" of the demo apk
        "regex": "demoApp=\"(?<version>[a-zA-Z0-9.-]+)\""
      },
    }
  }
}
```
Now, press <kbd>⌘</kbd>+<kbd>S</kbd> (or <kbd>Ctrl</kbd>+<kbd>S</kbd>) to save the config.json and plugin will pick up 
new changes.

## Boom 💥
Everything should work now!

If it doesn't, please, make sure that your `config.json` complies with 
[this](https://github.com/ozontech/kelp/blob/main/src/main/kotlin/ru/ozon/ideplugin/kelp/KelpConfig.kt) format.

If that does not help, please, file [an issue](https://github.com/ozontech/kelp/issues/new) in this repo.

## License

```
Copyright © 2023 LLC "Internet Solutions"

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
