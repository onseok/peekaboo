# peekaboo
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.21-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-v1.5.11-blue)](https://github.com/JetBrains/compose-multiplatform)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.team-preat/peekaboo-image-picker?color=orange)](https://search.maven.org/search?q=g:io.github.team-preat)
[![Apache-2.0](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://opensource.org/licenses/Apache-2.0)

[![Build](https://github.com/team-preat/peekaboo/actions/workflows/ci_check.yml/badge.svg)](https://github.com/team-preat/peekaboo/actions/workflows/ci_check.yml)
![badge-android](http://img.shields.io/badge/platform-android-6EDB8D.svg?style=flat)
![badge-ios](http://img.shields.io/badge/platform-ios-CDCDCD.svg?style=flat)
[![Featured in androidweekly.net](https://img.shields.io/badge/Featured%20in%20androidweekly.net-Issue%20%23601-4998C2)](https://androidweekly.net/issues/issue-601)

📂 Kotlin Multiplatform library for Compose Multiplatform, designed for seamless integration of an image picker feature in iOS and Android applications.

## Getting started

### Compose Multiplatform

`peekaboo` is based on `Compose Multiplatform`, currently targeting only `iOS` and `Android`. <br/>
Please note that it primarily focuses on these platforms, and additional platforms may be considered in the future. <br/>
When using `peekaboo` on Android, ensure that Google's Jetpack Compose version is compatible with `peekaboo`'s Compose Multiplatform version. <br/>

## Installation

The minimum supported Android SDK is 24 (Android 7.0).

In your `commonMain` configuration, add the desired dependency, either **`peekaboo-ui`** or **`peekaboo-image-picker`**, to your project. Both are available on Maven Central.
<br/>
### Without Version Catalog

```kotlin
commonMain {
    dependencies {
    	// peekaboo-ui
        implementation("io.github.team-preat:peekaboo-ui:$latest_version")

        // peekaboo-image-picker
        implementation("io.github.team-preat:peekaboo-image-picker:$latest_version")
    }
}
```


### With Version Catalog

First, define the version in `libs.versions.toml`:

```toml
[versions]
peekaboo = "0.3.0"

[libraries]
peekaboo-ui = { module = "io.github.team-preat:peekaboo-ui", version.ref = "peekaboo" }
peekaboo-image-picker = { module = "io.github.team-preat:peekaboo-image-picker", version.ref = "peekaboo" }
```

Then, in your `commonMain` configuration, reference the defined version:

```kotlin
commonMain {
    dependencies {
        // peekaboo-ui
        implementation(libs.peekaboo.ui)

        // peekaboo-image-picker
        implementation(libs.peekaboo.image.picker)
    }
}
```

### Artifacts

| Name                    | Description                                                                 |
|-------------------------|-----------------------------------------------------------------------------|
| `peekaboo-ui` |Provides user-friendly UI elements, including a custom camera view for easy image capture, suitable for both `iOS` and `Android` platforms. |
| `peekaboo-image-picker` | Simplifies the process of selecting single or multiple images both in `iOS` and `Android` platforms. |

<br/>

## Usage
### Xcode setup
In order to access the camera on iOS devices, it's essential to include a specific key-value pair in the `Info.plist` file of your iOS project. This key-value pair comprises a key that identifies the type of permission being requested and a value that provides a user-friendly description explaining why the app needs access to the camera.

Here's the key-value pair you should add to your `Info.plist`:
```xml
<key>Privacy - Camera Usage Description</key>
<string>This app uses camera for capturing photos.</string>
```

### Custimizable Camera UI
`PeekabooCamera` is a `composable` function that provides a customizable camera UI within a `Compose Multiplatform` application.

```kotlin
@Composable
fun CustomCameraView() {
    PeekabooCamera(
        modifier = Modifier.fillMaxSize(),
        cameraMode = CameraMode.Back, // or CameraMode.Front
        captureIcon = { onClick -> /* Custom Capture Button UI */ },
        convertIcon = { onClick -> /* Custom Convert Button UI */ },
        progressIndicator = { /* Custom Progress Indicator UI */ },
        onCapture = { byteArray ->
            // Handle the captured image
        },
        permissionDeniedContent = {
            // Custom UI content for permission denied scenario
        }
    )
}
```
- **`cameraMode`** : The initial camera mode (front or back). Default is `CameraMode.Back`.
- **`captureIcon`** : A `composable` lambda for the capture button. It takes an `onClick` lambda that triggers the image capture process.
- **`convertIcon`** : An optional `composable` lambda for a button to toggle the camera mode (front or back). It takes an `onClick` lambda for switching the camera.
- **`progressIndicator`** : An optional `composable` lambda displayed during photo capture processing.
- **`onCapture`** : A lambda called when a photo is captured, providing the photo as a `ByteArray` or `null` if the capture fails.
- **`permissionDeniedContent`** : An optional `composable` lambda that provides content to be displayed when camera permission is denied. This allows users to define a custom UI to inform or guide the user when camera access has been denied. The content can be informative text, an image, a button to redirect the user to settings, or any other `composable` content. This lambda will be invoked within the `PeekabooCamera` composable scope, replacing the camera preview with the user-defined UI.


> 💡 **Note**: It's recommended to handle camera permission checks before rendering `PeekabooCamera`. This preemptive check ensures the camera UI is displayed only when permissions are granted, avoiding empty views or unexpected user experiences.

#### Capturing an Image from Camera
| Android                                                         | iOS                                                     |
|-----------------------------------------------------------------|---------------------------------------------------------|
| <img src="https://github.com/onseok/onseok/assets/76798309/897a0104-2e8d-4339-90fb-2f61807aa56d" width="300" height="700"> | <img src="https://github.com/onseok/onseok/assets/76798309/fe414cc2-370a-4b0d-9558-c60e1fbbb4f7" width="300" height="700"> |

#### Toggling Camera Mode Between Front and Back
| Android                                                         | iOS                                                     |
|-----------------------------------------------------------------|---------------------------------------------------------|
| <img src="https://github.com/onseok/onseok/assets/76798309/477f49f8-389d-4ba6-a2d1-60155cab355e" width="300" height="700"> | <img src="https://github.com/onseok/onseok/assets/76798309/022da284-cf58-4ce0-9fc1-e592885f09b9" width="300" height="700"> |

#### Handling Denied Camera Permissions
| Android                                                         | iOS                                                     |
|-----------------------------------------------------------------|---------------------------------------------------------|
| <img src="https://github.com/onseok/onseok/assets/76798309/61511ec6-45b9-48bd-9267-fe9f2a086008" width="300" height="700"> | <img src="https://github.com/onseok/onseok/assets/76798309/604ed9d9-d6e0-47ac-9393-5cb627a4f13d" width="300" height="700"> |


### Select Single Image

```kotlin
val scope = rememberCoroutineScope()

val singleImagePicker = rememberImagePickerLauncher(
    selectionMode = SelectionMode.Single,
    scope = scope,
    onResult = { byteArrays ->
        byteArrays.firstOrNull()?.let {
            // Process the selected images' ByteArrays.
            println(it)
        }
    }
)

Button(
    onClick = {
        singleImagePicker.launch()
    }
) {
    Text("Pick Single Image")
}
```

| Android                                                         | iOS                                                     |
|-----------------------------------------------------------------|---------------------------------------------------------|
| <img src="https://github.com/TEAM-PREAT/peekaboo/assets/76798309/03346992-cf2a-4424-88e5-fa53afd36eac" width="300" height="700"> | <img src="https://github.com/TEAM-PREAT/peekaboo/assets/76798309/7b562f6f-e5d5-4858-85b8-acf7196d646f" width="300" height="700"> |

Simply select the desired image with an intuitive interface.

<br/>

### Select Multiple Images

If you want to select multiple images, you can use `SelectionMode.Multiple()`. And you can set the maximum number of images to select.
If you didn't set max selection, the default value is maximum number that the system supports.

```kotlin
val scope = rememberCoroutineScope()

val multipleImagePicker = rememberImagePickerLauncher(
    // Optional: Set a maximum selection limit, e.g., SelectionMode.Multiple(maxSelection = 5).
    // Default: No limit, depends on system's maximum capacity.
    selectionMode = SelectionMode.Multiple(maxSelection = 5),
    scope = scope,
    onResult = { byteArrays ->
        byteArrays.forEach {
            // Process the selected images' ByteArrays.
            println(it)
        }
    }
)

Button(
    onClick = {
        multipleImagePicker.launch()
    }
) {
    Text("Pick Multiple Images")
}
```

| Android                                                         | iOS                                                     |
|-----------------------------------------------------------------|---------------------------------------------------------|
| <img src="https://github.com/TEAM-PREAT/peekaboo/assets/76798309/e26ae1b3-4333-41a9-92c3-ebe56c337d79" width="300" height="700"> | <img src="https://github.com/TEAM-PREAT/peekaboo/assets/76798309/a990ce09-d485-4a0f-9416-8af8b040cf2d" width="300" height="700"> |

<br/>

## ByteArray to ImageBitmap Conversion
We've added a new extension function `toImageBitmap()` to convert a `ByteArray` into an `ImageBitmap`. <br/>
This function simplifies the process of converting image data into a displayable format, enhancing the app's capability to handle image processing efficiently.

### Usage
```kotlin
val imageBitmap = byteArray.toImageBitmap()
```

<br/>

## Contributions 🙏

Contributions are always welcome!

If you'd like to contribute, please feel free to create a PR or open an issue. 👍

<br/>

## Stargazers :star:
Support it by joining __[stargazers](https://github.com/TEAM-PREAT/peekaboo/stargazers)__ for this repository. :star: <br>

## License

```
Copyright 2023 onseok

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
