plugins {
    id("com.android.library")
    `maven-publish`
}

group = "org.clojure-android"
version = "5.0.0-SNAPSHOT"

android {
    namespace = "org.clojure_android.neko"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    // Include Clojure source files as resources so they can be loaded at runtime
    sourceSets["main"].resources.srcDirs("src/clojure")

    // Java sources are in the standard location
    sourceSets["main"].java.srcDirs("src/java")

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    // Clojure runtime — compileOnly because the app provides it
    compileOnly("org.clojure:clojure:1.12.0")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            afterEvaluate {
                from(components["release"])
            }
            groupId = "org.clojure-android"
            artifactId = "neko"
            version = project.version.toString()
        }
    }
}
