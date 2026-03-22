pluginManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.library") version "8.9.0"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "neko"

// Include sibling projects needed for unit tests, but only when building
// neko standalone (not when consumed as an includeBuild by another project).
if (gradle.parent == null) {
    val runtimeCoreDir = file("../runtime-core")
    if (runtimeCoreDir.isDirectory && file("${runtimeCoreDir}/build.gradle.kts").isFile) {
        includeBuild(runtimeCoreDir)
    }
}
