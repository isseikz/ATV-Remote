rootProject.name = "signalinglib"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

// Signalinglib library modules
include(":shared")           // 汎用シグナリング共通コード
include(":server")           // シグナリングサーバーライブラリ
include(":client")           // シグナリングクライアントライブラリ

// Demo application modules
include(":demo:shared")      // デモアプリ専用共通コード
include(":demo:server")      // デモアプリサーバー
include(":demo:composeApp")  // デモアプリUI
