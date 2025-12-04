import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import java.util.Properties
import java.io.FileInputStream
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.spotlessPlugin)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.crashlytics)
    alias(libs.plugins.composeCompiler)
}

val enableApkSplits: Boolean = (project.findProperty("apkSplits") as? String)?.toBoolean() ?: false

android {
    namespace = "com.github.shiroedev2024.leaf.android"
    compileSdk = 36

    defaultConfig {
        val properties = Properties()
        properties.load(FileInputStream(file("${rootDir}/version.properties")))

        applicationId = "com.github.shiroedev2024.leaf.android"
        minSdk = 24
        targetSdk = 36

        versionCode = Integer.parseInt(properties.getProperty("app.version.code"))
        versionName = properties.getProperty("app.version.name")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val properties = Properties()
            properties.load(FileInputStream(file("../release.properties")))
            storeFile = file(properties.getProperty("storeFile"))
            storePassword = properties.getProperty("storePassword")
            keyAlias = properties.getProperty("keyAlias")
            keyPassword = properties.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            configure<CrashlyticsExtension> {
                nativeSymbolUploadEnabled = true
            }
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    if (enableApkSplits) {
        splits {
            abi {
                isEnable = true
                reset()
                include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
                isUniversalApk = true
            }
        }

        val versionCodes = mapOf(
            "armeabi-v7a" to 1,
            "arm64-v8a" to 2,
            "x86" to 3,
            "x86_64" to 4,
            "all" to 5
        )

        applicationVariants.all {
            this.outputs.map { it as ApkVariantOutputImpl }.forEach { output ->
                val abi = output.filters.find { it.filterType == "ABI" }?.identifier ?: "all"

                this.buildConfigField("int", "ORIGINAL_VERSION_CODE", this.versionCode.toString())

                output.outputFileName = "leaf_vpn_${this.versionName}_${abi}.apk"
                output.versionCodeOverride = (1000000 * versionCodes[abi]!!) + this.versionCode
            }
        }
    }

}

spotless {
    kotlin {
        target("src/*/java/**/*.kt")
        ktfmt().kotlinlangStyle()
    }
}

dependencies {
    implementation(libs.leaf.sdk.android)

    implementation(libs.locale.helper.android)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    implementation(libs.nv.i18n)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation (libs.material)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.okhttp.logging)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}