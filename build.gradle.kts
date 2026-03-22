plugins {
    id("com.android.library")
    `maven-publish`
}

group = "com.goodanser.clj-android"
version = "5.0.0-SNAPSHOT"

android {
    namespace = "com.goodanser.clj_android.neko"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Include Clojure source files as resources so they can be loaded at runtime
    sourceSets["main"].resources.srcDirs("src/clojure")

    // Java sources are in the standard location
    sourceSets["main"].java.srcDirs("src/java")

    // Unit test sources
    sourceSets["test"].java.srcDirs("test/java")
    sourceSets["test"].resources.srcDirs("test")

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

// AGP's processJavaRes doesn't always track file changes in resources.srcDirs,
// causing stale .clj files in incremental/composite builds.  Explicitly add
// the Clojure source tree as a tracked input so the task re-runs on changes.
afterEvaluate {
    val clojureDir = file("src/clojure")
    listOf("processDebugJavaRes", "processReleaseJavaRes").forEach { taskName ->
        tasks.matching { it.name == taskName }.configureEach {
            inputs.files(fileTree(clojureDir) { include("**/*.clj") })
                .withPathSensitivity(org.gradle.api.tasks.PathSensitivity.RELATIVE)
                .withPropertyName("clojureSources")
        }
    }
}

dependencies {
    // Clojure runtime — compileOnly because the app provides it
    compileOnly("org.clojure:clojure:1.12.0")

    // AndroidX support library widgets — compileOnly because the app provides them
    compileOnly("androidx.recyclerview:recyclerview:1.3.2")
    compileOnly("androidx.cardview:cardview:1.0.0")
    compileOnly("androidx.appcompat:appcompat:1.7.0")
    compileOnly("androidx.drawerlayout:drawerlayout:1.2.0")
    compileOnly("androidx.viewpager:viewpager:1.0.0")
    compileOnly("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    compileOnly("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    compileOnly("androidx.core:core:1.13.1")
    compileOnly("com.google.android.material:material:1.12.0")

    // Unit tests
    testImplementation("org.clojure:clojure:1.12.0")
    testImplementation("org.clojure:spec.alpha:0.5.238")
    testImplementation("org.clojure:core.specs.alpha:0.4.74")
    testImplementation("com.goodanser.clj-android:runtime-core:0.1.0-SNAPSHOT")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.15.1")
    testImplementation("androidx.test:core:1.6.1")
}

// When consumed via includeBuild(), raw project configurations are exposed
// instead of published module metadata.  AGP's published metadata includes
// these attributes automatically, but the raw configurations do not, so we
// add them here for composite-build compatibility.
afterEvaluate {
    val categoryAttr = Attribute.of("org.gradle.category", Named::class.java)
    val jvmEnvAttr = Attribute.of("org.gradle.jvm.environment", Named::class.java)
    val kotlinPlatformAttr = Attribute.of("org.jetbrains.kotlin.platform.type", Named::class.java)

    configurations.configureEach {
        if (isCanBeConsumed && !isCanBeResolved && name != "archives") {
            attributes {
                attribute(categoryAttr, objects.named("library"))
                attribute(jvmEnvAttr, objects.named("android"))
                attribute(kotlinPlatformAttr, objects.named("androidJvm"))
            }
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            afterEvaluate {
                from(components["release"])
            }
            groupId = "com.goodanser.clj-android"
            artifactId = "neko"
            version = project.version.toString()
        }
    }
}
