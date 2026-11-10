import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// ---- App version: one source (version.properties) for Android and iOS -------------------------
val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    load(providers.fileContents(rootProject.layout.projectDirectory.file("version.properties")).asText.get().reader())
}
val appVersionName: String = versionProps.getProperty("VERSION_NAME")
val appVersionCode: Int = versionProps.getProperty("VERSION_CODE").toInt()

// ---- Release signing: keystore.properties (gitignored) or env vars for CI ---------------------
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.reader().use(::load)
}
fun signingValue(key: String, env: String): String? =
    keystoreProps.getProperty(key) ?: providers.environmentVariable(env).orNull

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.jetbrains.kotlinx.datetime)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinxJson)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.filekit.dialogs.compose)
            implementation(libs.koalaplot.core)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "org.celimited.manager"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.celimited.manager"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName
    }
    buildFeatures {
        buildConfig = true
    }

    // Test vs Live server builds. Installable side by side (different applicationId); only `live`
    // goes to the Play Store. Base URLs here and in iosApp/Configuration/{Test,Live}.xcconfig must match.
    flavorDimensions += "env"
    productFlavors {
        create("stage") {
            dimension = "env"
            applicationIdSuffix = ".test"
            versionNameSuffix = "-test"
            resValue("string", "app_name", "ManageEsae Test")
            buildConfigField("String", "BASE_URL", "\"http://210.4.65.222/SupervisorApp/api/v1/\"")
            buildConfigField("String", "ENVIRONMENT", "\"TEST\"")
        }
        create("live") {
            dimension = "env"
            resValue("string", "app_name", "ManageEsae")
            buildConfigField("String", "BASE_URL", "\"https://syngentawebapp.azurewebsites.net/SupervisorApp/api/v1/\"")
            buildConfigField("String", "ENVIRONMENT", "\"LIVE\"")
        }
    }

    signingConfigs {
        val storePath = signingValue("storeFile", "MANAGER_KEYSTORE_FILE")
        if (storePath != null) {
            create("release") {
                storeFile = rootProject.file(storePath)
                storePassword = signingValue("storePassword", "MANAGER_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "MANAGER_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "MANAGER_KEY_PASSWORD")
            }
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            // Unsigned release output if no keystore is configured (e.g. on a fresh checkout).
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}


// ---- ./gradlew bumpVersion -Pbump=patch|minor|major|build -----------------------------------
// Rewrites version.properties and iosApp/Configuration/Version.xcconfig together, so Android and
// iOS always ship the same version. versionCode / build number goes up by 1 on every bump.
val iosVersionFile = rootProject.file("iosApp/Configuration/Version.xcconfig")
val bumpType = providers.gradleProperty("bump")
tasks.register("bumpVersion") {
    group = "release"
    description = "Bumps the shared Android/iOS version. -Pbump=patch|minor|major|build"
    val propsFile = versionPropsFile
    val xcconfigFile = iosVersionFile
    val type = bumpType
    doLast {
        val props = Properties().apply { propsFile.reader().use(::load) }
        val (major, minor, patch) = props.getProperty("VERSION_NAME").split(".").map(String::toInt)
        val newName = when (val t = type.orNull ?: error("Pass -Pbump=patch|minor|major|build")) {
            "major" -> "${major + 1}.0.0"
            "minor" -> "$major.${minor + 1}.0"
            "patch" -> "$major.$minor.${patch + 1}"
            "build" -> "$major.$minor.$patch"
            else -> error("Unknown bump type '$t'. Use patch, minor, major or build.")
        }
        val newCode = props.getProperty("VERSION_CODE").toInt() + 1
        propsFile.writeText(
            """
            |# Single source of truth for the app version on Android AND iOS.
            |# Don't edit by hand: run `./gradlew bumpVersion -Pbump=patch|minor|major|build`,
            |# which also regenerates iosApp/Configuration/Version.xcconfig.
            |VERSION_NAME=$newName
            |VERSION_CODE=$newCode
            |""".trimMargin()
        )
        xcconfigFile.writeText(
            """
            |// GENERATED by `./gradlew bumpVersion` from version.properties. Do not edit.
            |MARKETING_VERSION=$newName
            |CURRENT_PROJECT_VERSION=$newCode
            |""".trimMargin()
        )
        println("Version: $newName ($newCode)")
    }
}
