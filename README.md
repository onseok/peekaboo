# peekaboo
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.21-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Apache-2.0](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.team-preat/peekaboo-image-picker?color=orange)](https://search.maven.org/search?q=g:io.github.mirzemehdi)
![badge-android](http://img.shields.io/badge/platform-android-6EDB8D.svg?style=flat)
![badge-ios](http://img.shields.io/badge/platform-ios-CDCDCD.svg?style=flat)

📂 Kotlin Multiplatform library for Compose Multiplatform, designed for seamless integration of an image picker feature in iOS and Android applications.

## Getting started
[![Maven Central](https://img.shields.io/maven-central/v/io.github.team-preat/peekaboo-image-picker?color=orange)](https://search.maven.org/search?q=g:io.github.mirzemehdi)

### Compose Multiplatform

As of version 0.1.1, `Peekaboo` is based on `Compose Multiplatform`, currently targeting only `iOS` and `Android`.
Please note that it primarily focuses on these platforms, and additional platforms may be considered in the future.
When using `Peekaboo` on Android, ensure that Google's Jetpack Compose version is compatible with Peekaboo's Compose Multiplatform version.
Presently, the only available artifact is `peekaboo-image-picker`, but the intention is to gradually expand the range of features and artifacts over time.

### Installation

The minimum supported Android SDK is 24 (Android 7.0).

Add `peekaboo` as a dependency to your project in `commonMain`. It's available on Maven Central.

```kotlin
commonMain {
    dependencies {
        implementation("io.github.team-preat:peekaboo-image-picker:$latest_version")
    }
}
```

#### Artifacts

| Name                    | Description                                                                 |
|-------------------------|-----------------------------------------------------------------------------|
| `peekaboo-image-picker` | Simplifies the process of selecting single or multiple images in `iOS` and `Android` apps. |

## Usage
### Select Single Image

```kotlin
val scope = rememberCoroutineScope()

val singleImagePicker = rememberImagePickerLauncher(
    selectionMode = SelectionMode.Single,
    scope = scope,
    onResult = { byteArrays ->
        byteArrays.firstOrNull()?.let {
            // Do something.
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
| <img src="https://github.com/TEAM-PREAT/peekaboo/assets/76798309/a655bfd0-0499-4e30-879f-5155a2685928" width="300" height="700"> | <img src="https://github.com/TEAM-PREAT/peekaboo/assets/76798309/350fffd7-6d25-45e6-8be2-de86ff5e8d82" width="300" height="700"> |

You can just simply select the desired image.

### Select Multiple Images

```kotlin
val scope = rememberCoroutineScope()

val multipleImagePicker = rememberImagePickerLauncher(
    selectionMode = SelectionMode.Multiple,
    scope = scope,
    onResult = { byteArrays ->
        // Do something.
        println(byteArrays)
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
| <img src="https://github.com/TEAM-PREAT/peekaboo/assets/76798309/3090cbff-36d8-462b-9b36-af07efe5e253" width="300" height="700"> | <img src="https://github.com/TEAM-PREAT/peekaboo/assets/76798309/06203301-eb41-4a05-b0ac-812b60731274" width="300" height="700"> |

## Contributions

Contributions are always welcome. 🙏

If you'd like to contribute, please feel free to create a PR or open an issue. 👍

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