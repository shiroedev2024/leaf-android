plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.rustPlugin)
}

android {
    // Don't have the matching NDK for AGP installed: https://developer.android.com/studio/projects/install-ndk#default-ndk-per-agp
    ndkVersion = "26.1.10909125"

    namespace = "com.github.shiroedev2024.leaf.android.library"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        aidl = true
    }
}

cargo {
    module = "./native"
    libname = "native"
    targets = listOf("arm64", "x86_64", "x86", "arm")
    profile = "debug"
    exec = { spec, toolchain ->
        spec.environment("ANDROID_NDK_HOME", "${System.getenv("HOME")}/Android/Sdk/ndk/26.1.10909125")
        spec.environment("CLANG_VERSION", "17")
    }
}

project.afterEvaluate {
    tasks.withType(com.nishtahir.CargoBuildTask::class)
        .forEach { buildTask ->
            tasks.withType(com.android.build.gradle.tasks.MergeSourceSetFolders::class)
                .configureEach {
                    this.inputs.dir(
                        layout.buildDirectory.dir("rustJniLibs" + File.separatorChar + buildTask.toolchain!!.folder)
                    )
                    this.dependsOn(buildTask)
                }
        }
}

dependencies {

    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}