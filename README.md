![kelp-banner-light.svg](images%2Fkelp-banner-light.svg#gh-light-mode-only)
![kelp-banner-dark.svg](images%2Fkelp-banner-dark.svg#gh-dark-mode-only)

# Kelp

[![Introductory Medium Article](https://img.shields.io/badge/medium-article-grey?labelColor=black&logo=medium&logoColor=white&link=https://proandroiddev.com/kelp-plugin-for-android-studio-4374127939aa)](https://proandroiddev.com/kelp-plugin-for-android-studio-4374127939aa)
![License](https://img.shields.io/github/license/popovanton0/Blueprint?color=blue)
<!-- ![Build](https://github.com/ozontech/kelp/workflows/Build/badge.svg) -->
<!-- Plugin description -->
Kelp is an Android Studio plugin that enhances support for **custom design systems**.

[Introductory Medium Article](https://proandroiddev.com/kelp-plugin-for-android-studio-4374127939aa)

| Feature                                                                                                                                                                | Screenshot                                                                                                                                                                                                       |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Customizable icon for<br> design system **component functions**                                                                                                        | <img src="https://github.com/ozontech/kelp/raw/main/images/componentFunHighlighting-light.png#gh-light-mode-only" width="600"><img src="images/componentFunHighlighting-dark.png#gh-dark-mode-only" width="600"> |
| DS **icons** in the code completion<br> and gutter (where breakpoints are),<br> like with `R.drawable`                                                                 | <img src="https://github.com/ozontech/kelp/raw/main/images/iconsRendering-light.png#gh-light-mode-only" width="600"><img src="images/iconsRendering-dark.png#gh-dark-mode-only" width="600">                     |
| **Colors** from DS palette in <br>the code completion and<br> gutter (where breakpoints are),<br> like with `R.color`                                                  | <img src="https://github.com/ozontech/kelp/raw/main/images/colorPreview-light.png#gh-light-mode-only" width="600"><img src="images/colorPreview-dark.png#gh-dark-mode-only" width="600">                         |
| Installing the apk file of <br>the **demo app** (showcase app) on an Android <br>device, as well as navigating to the component <br>page in it via an Intention Action | <img src="https://github.com/ozontech/kelp/raw/main/images/demoApkInstalling-light.png#gh-light-mode-only" width="600"><img src="images/demoApkInstalling-dark.png#gh-dark-mode-only" width="600">               |
| **KDoc Images** Rendering                                                                                                                                              | <img src="https://github.com/ozontech/kelp/raw/main/images/kdocImagesRendering-light.png#gh-light-mode-only" width="600"><img src="images/kdocImagesRendering-dark.png#gh-dark-mode-only" width="600">           |
| Handy **live templates**<br>(customizable; after applying, automatically opens code completion popup)                                                                  | <img src="https://github.com/ozontech/kelp/raw/main/images/live-templates-light.png#gh-light-mode-only" width="600"><img src="images/live-templates-dark.png#gh-dark-mode-only" width="600">                     |

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

## KDoc Images Rendering
Until this [issue](https://youtrack.jetbrains.com/issue/KTIJ-13687/KDoc-support-inline-images) is resolved,
Android Studio cannot render images referenced in KDoc.

This plugin ✨automatically✨ fixes this behaviour.

However, KDoc image syntax does not support specifying image size. This plugin introduces new syntax to achieve this:
```kotlin
/**
 * ![Extended FAB image](https://example.com/image.png)
 * ![256x75 Extended FAB image](https://example.com/image.png)
 * ![256x Extended FAB image](https://example.com/image.png)
 * ![x75 Extended FAB image](https://example.com/image.png)
 * 
 * Space is not necessary but is used for readability.
 */
fun Button()
```

This feature is especially useful for design system creators and users — it increases **discoverability** of ds
components, allowing users to instantly preview them, for example, in code completion menu.

## Color Previews
For this feature to work, you need to implement your color system like this:
```kotlin
class MyColors(
  val primary: Color,
  val secondary: Color,
  val accent: Color,
) {
  /**
   * This class must have MUST structure and name.
   * It MUST be placed here.
   * You can create it manually or autogenerate it using code generators.
   */
  private class KelpColorPreview {
    /**
     * The pattern is "name_lightColor_darkColor"
     * If you don't have a dark theme, you MUST set `darkColor`
     * to be the same as `lightColor`, then it won't be rendered.
     * 
     * Colors MUST be in ARGB:
     * The format of an ARGB hexadecimal color is AARRGGBB. 
     * AA is the alpha channel. It represents the opacity of the color. 
     * RR is the red value, GG is the green, and BB is the blue.
     * 
     * If your colors are in RGB format, just add FF to them, 
     * representing no transparency.
     */
    val primary_FFD0BCFF_FF6650A4 = Unit
    val secondary_12CCC2DC_FF625B71 = Unit
    val accent_FFEFB8C8_FF7D5260 = Unit
  }
}

class MyColors2 {
  val primary: Color = TODO()
  val secondary: Color = TODO()
  val accent: Color by lazy { calculation() }

  private class KelpColorPreview {
    val primary_FFD0BCFF_FF6650A4 = Unit
    val secondary_12CCC2DC_FF625B71 = Unit
    val accent_FFEFB8C8_FF7D5260 = Unit
  }
}
```

Optionally, Kelp also supports color tokens.
To enable, set `enumColorTokensEnabled` in `config.json` (see below)

More info about color tokens — [here](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/ColorScheme.kt;l=879?q=ColorSchemeKeyTokens&ss=androidx%2Fplatform%2Fframeworks%2Fsupport).

```kotlin
enum class MyColorTokens {
  Primary,
  Secondary,
  Accent,
  ;

  private class KelpColorPreview {
    val Primary_FFD0BCFF_FF6650A4 = Unit
    val Secondary_12CCC2DC_FF625B71 = Unit
    val Accent_FFEFB8C8_FF7D5260 = Unit
  }
}
```
Using this convention, there is **no need** to connect a configuration file with 
color values to the plugin per project.

> [!WARNING]  
> Until [this issue](https://youtrack.jetbrains.com/issue/GRZ-4351) is resolved, **Grazie Pro** plugin
> is incompatible with color previews in the gutter.
>
> Please, disable the Grazie Pro plugin if you want to use this feature.

## Experimental support for IntelliJ IDEA
Kelp plugin supports IntelliJ IDEA in addition to Android Studio.

> [!WARNING]  
> However, IntelliJ IDEA support is experimental and can be **dropped** anytime. **DO NOT** rely on it.

## Installation

1. Make sure that you are using **Android Studio Koala | 2024.1.1 Canary 3** or later
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
  "colorPreview": {
    "codeCompletionEnabled": true,
    "gutterEnabled": true,
    "enumColorTokensEnabled": true
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
    "apkInstallation": true
  },
  "liveTemplates": [
    {
      "abbreviation": "dt",
      "text": "com.your.designsystem.DsTheme.$CODE_COMPLETION$",
      "description": "Writes \"DsTheme.\""
    },
    {
      "abbreviation": "dtc",
      "text": "com.your.designsystem.DsTheme.colors.$CODE_COMPLETION$",
      "description": "Writes \"DsTheme.colors\""
    },
    {
      "abbreviation": "dtt",
      "text": "com.your.designsystem.DsTheme.typography.$CODE_COMPLETION$",
      "description": "Writes \"DsTheme.typography\""
    },
    {
      "abbreviation": "dtt",
      "text": "com.your.designsystem.DsTheme.icons.$CODE_COMPLETION$",
      "description": "Writes \"DsTheme.icons\""
    }
  ]
}
```
</details>

```json5
{
  // If you want to disable some of these features, just don't include their sections in your json file.
  
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

  // Rendering design system colors in the code completion and gutter (where breakpoints are). 
  // Like with regular Android resources.
  "colorPreview": {
    "codeCompletionEnabled": true,
    "gutterEnabled": true,
    // optional, color tokens from enum class
    "enumColorTokensEnabled": true,
  },
  
  // Rendering design system icons in the code completion and gutter (where breakpoints are). 
  // Like with regular Android resources.
  // This feature:
  // 1. scans the fields of `containerClassName`
  // 2. filters them using `propertyNameFilter`
  // 3. applies `propertyToResourceMapper`
  // 4. retrieves an icon by the resulting name from the xml that is available in the project
  // or it's dependencies and places this icon in the code completion and gutter.
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
  
  // Opening the component page in the demo app via an Intention Action
  "demoApp": {
    // optional: custom name of the intention action
    "intentionName": "🚀 Open in MY CUSTOM design system demo app",
    "functionFqnPrefix": "com.your.designsystem.package.components.",
    "functionSimpleNamePrefix": "Ds", // optional
    // package name of the demo app
    "appPackageName": "com.your.designsystem.package.demo",
    // deeplink that will be used to open a component page in the demo app.
    // DS_COMPONENT_FQN_DEEPLINK_PLACEHOLDER will be replaced with
    // the fully qualified name of the 
    // composable function, e.g. com.your.designsystem.package.components.Badge
    "componentDeeplink": "yourscheme://component/DS_COMPONENT_FQN_DEEPLINK_PLACEHOLDER",
    
    // optional
    // Installing (if not installed) the apk file
    // of the demo app (showcase app) on an Android device.
    
    // Demo app apk must be placed here with this name: /.idea/kelp/demoApp-VERSION_NAME.apk
    // For example: /.idea/kelp/demoApp-0.12.0.apk
    // The plugin will acquire the latest version from the apk file name (for example, 0.12.0).
    // If the app is not installed OR installed, but has a lower
    // version, the plugin will install the apk on the device.
    "apkInstallation": true
  },
  
  // Installs live templates into the IDE.
  // Useful for writing frequent code, like "MaterialTheme.colors." in just 3 keystrokes.
  // After completion, opens code completion in place of $CODE_COMPLETION$, saving even more effort.
  "liveTemplates": [
    {
      "abbreviation": "dt",
      "text": "com.your.designsystem.DsTheme.$CODE_COMPLETION$",
      "description": "Writes \"DsTheme.\""
    },
    {
      "abbreviation": "dtc",
      "text": "com.your.designsystem.DsTheme.colors.$CODE_COMPLETION$",
      "description": "Writes \"DsTheme.colors\""
    },
    {
      "abbreviation": "dtt",
      "text": "com.your.designsystem.DsTheme.typography.$CODE_COMPLETION$",
      "description": "Writes \"DsTheme.typography\""
    },
    {
      "abbreviation": "dtt",
      "text": "com.your.designsystem.DsTheme.icons.$CODE_COMPLETION$",
      "description": "Writes \"DsTheme.icons\""
    }
  ]
}
```
Now, press <kbd>⌘</kbd> + <kbd>S</kbd> (or <kbd>Ctrl</kbd> + <kbd>S</kbd>) to save the config.json and plugin will 
pick up new changes.

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
