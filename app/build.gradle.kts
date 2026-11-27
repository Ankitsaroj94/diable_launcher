import com.android.build.api.artifact.SingleArtifact
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// standard uses this code. full is one higher so it can replace the sideload APK
// of the same release without wiping data. The next release must set this higher
// than the previous full flavor (this value + 1).
val releaseVersionCode = 10

android {
    namespace = "in.ankitsaroj.diable"
    compileSdk = 36

    defaultConfig {
        applicationId = "in.ankitsaroj.diable"
        minSdk = 24
        targetSdk = 36
        versionCode = releaseVersionCode
        versionName = "1.6.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing comes from ~/.gradle/gradle.properties (DIABLE_STORE_FILE, …), never
    // from the repo. Without those properties the release build is simply left unsigned.
    val storeFilePath = providers.gradleProperty("DIABLE_STORE_FILE").orNull
    signingConfigs {
        if (storeFilePath != null) {
            create("release") {
                storeFile = file(storeFilePath)
                storePassword = providers.gradleProperty("DIABLE_STORE_PASSWORD").get()
                keyAlias = providers.gradleProperty("DIABLE_KEY_ALIAS").get()
                keyPassword = providers.gradleProperty("DIABLE_KEY_PASSWORD").get()
            }
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        // Default sideload APK. No notification listener and no accessibility service,
        // so Play Protect's enhanced fraud protection can install it.
        create("standard") {
            dimension = "distribution"
            isDefault = true
        }
        // Same app, with notification dots / quick replies / media / Quick Lock.
        // Play Protect may block this APK when it is installed from a browser,
        // messenger or file manager in India and the other pilot regions.
        create("full") {
            dimension = "distribution"
            versionNameSuffix = "-full"
            versionCode = releaseVersionCode + 1
        }
    }

    buildTypes {
        release {
            if (storeFilePath != null) signingConfig = signingConfigs.getByName("release")
            // A launcher is always resident: shrinking and removing unused resources
            // cuts both install size and the memory the DEX/resources occupy.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.accompanist.drawablepainter)
    implementation(libs.coil.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

androidComponents {
    onVariants { variant ->
        val flavor = variant.flavorName
        if (flavor != "standard" && flavor != "full") return@onVariants
        val cap = variant.name.replaceFirstChar { it.uppercase() }
        val check = tasks.register<PlayProtectManifestCheck>("check${cap}PlayProtectManifest") {
            manifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
            requireSensitiveServices.set(flavor == "full")
        }
        tasks.matching { it.name == "assemble$cap" }.configureEach {
            dependsOn(check)
        }
    }
}

/**
 * Fails the build if the sideload manifest still declares a permission Play Protect's
 * enhanced fraud protection hard-blocks, or if the full manifest lost the services
 * that build exists to keep.
 */
abstract class PlayProtectManifestCheck : DefaultTask() {
    @get:InputFile
    abstract val manifest: RegularFileProperty

    @get:Input
    abstract val requireSensitiveServices: Property<Boolean>

    @TaskAction
    fun check() {
        val text = manifest.get().asFile.readText()
        val triggers = listOf(
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.SEND_SMS",
            "android.permission.RECEIVE_MMS",
            "android.permission.RECEIVE_WAP_PUSH",
            "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
            "android.permission.BIND_ACCESSIBILITY_SERVICE",
            "android.service.notification.NotificationListenerService",
            "android.accessibilityservice.AccessibilityService",
        )
        val found = triggers.filter { text.contains(it) }
        if (requireSensitiveServices.get()) {
            val required = listOf(
                "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
                "android.permission.BIND_ACCESSIBILITY_SERVICE",
                "android.service.notification.NotificationListenerService",
                "android.accessibilityservice.AccessibilityService",
            )
            val missing = required.filter { it !in text }
            if (missing.isNotEmpty()) {
                throw GradleException("Full manifest is missing $missing")
            }
        } else if (found.isNotEmpty()) {
            throw GradleException(
                "Sideload manifest declares Play Protect triggers: $found",
            )
        }
    }
}
