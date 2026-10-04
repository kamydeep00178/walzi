plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// Google's sample AdMob *app* id. Fine for debug builds; must never ship in a release.
val testAdmobAppId = "ca-app-pub-3940256099942544~3347511713"

// The real AdMob app id for release builds, set in gradle.properties (ADMOB_APP_ID_RELEASE).
// It must belong to the same AdMob app as the ad *unit* ids in ads/AdManager.kt.
val releaseAdmobAppId: String = (findProperty("ADMOB_APP_ID_RELEASE") as String?) ?: testAdmobAppId

gradle.taskGraph.whenReady {
    val buildingRelease = allTasks.any { it.path == ":app:bundleRelease" || it.path == ":app:assembleRelease" }
    if (buildingRelease && releaseAdmobAppId == testAdmobAppId) {
        logger.warn(
            "\n⚠️  RELEASE BUILD IS USING GOOGLE'S TEST ADMOB APP ID.\n" +
                "    Set ADMOB_APP_ID_RELEASE in gradle.properties to your real AdMob app id\n" +
                "    (ca-app-pub-4136650480208705~XXXXXXXXXX), or set ADS_ENABLED = false in AdsConfig.kt.\n"
        )
    }
}

// Tell the Compose compiler our immutable-by-convention model/state classes and Kotlin
// collections are stable, so composables taking them can skip recomposition.
composeCompiler {
    stabilityConfigurationFiles.add(layout.projectDirectory.file("compose_stability.conf"))
}

android {
    namespace = "com.yunok.walzi"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.yunok.walzi"
        minSdk = 24
        targetSdk = 36
        versionCode = 10
        versionName = "1.0.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            buildConfigField("Boolean", "DEBUG_BUILD", "false")
            manifestPlaceholders["admobAppId"] = releaseAdmobAppId
            // Firebase Analytics collects only in release builds (see AndroidManifest + WalziApp).
            manifestPlaceholders["analyticsEnabled"] = "true"

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            buildConfigField("Boolean", "DEBUG_BUILD", "true")
            manifestPlaceholders["admobAppId"] = testAdmobAppId
            // No Analytics from debug builds - testing must not pollute production stats.
            manifestPlaceholders["analyticsEnabled"] = "false"

            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true

    }
    kotlinOptions {
        jvmTarget = "17"
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.crashlytics)

    implementation(libs.coil.compose)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.accompanist.systemuicontroller)

    // Lets the baseline profile (src/main/baseline-prof.txt) be applied to sideloaded / pre-Play installs too.
    implementation(libs.androidx.profileinstaller)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.play.services.ads)

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
