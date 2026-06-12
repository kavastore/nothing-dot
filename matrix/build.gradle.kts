plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "tech.dotlab.dot.matrix"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    api(project(":core"))
    api(project(":device"))
    implementation(libs.androidx.core.ktx)

    // GlyphMatrix SDK ships as a local .aar — see matrix/libs/README.md.
    // Exposed via `api` so modules that host a Glyph Toy service (e.g. :feature-editor) can
    // reference com.nothing.ketchum.* through their dependency on :matrix.
    api(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
}
