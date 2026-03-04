plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "rs.djerman.losmobileview"
    compileSdk = 36

    defaultConfig {
        applicationId = "rs.djerman.losmobileview"
        minSdk = 21
        targetSdk = 35

        versionCode = 9
        versionName = "2.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    packaging { jniLibs { useLegacyPackaging = false } }

    buildTypes {
        release {
            // Enable shrinking/obfuscation so Play Console can use mapping.txt
            isMinifyEnabled = true
            isShrinkResources = true
            // Generate native debug symbols archive for Play Console (ANR/crash symbolication)
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)
}

val packageReleaseNativeDebugSymbols by tasks.registering(org.gradle.api.tasks.bundling.Zip::class) {
    group = "build"
    description = "Packages release native debug symbols for Play Console upload"
    dependsOn("mergeReleaseNativeLibs")

    from(layout.buildDirectory.dir("intermediates/merged_native_libs/release/mergeReleaseNativeLibs/out/lib")) {
        include("arm64-v8a/*.so", "armeabi-v7a/*.so")
    }

    destinationDirectory.set(layout.buildDirectory.dir("outputs/native-debug-symbols/release"))
    archiveFileName.set("native-debug-symbols.zip")
}

afterEvaluate {
    tasks.findByName("bundleRelease")?.finalizedBy(packageReleaseNativeDebugSymbols)
}
