plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

android {
    namespace = "xyz.xiao6.myboard"
    compileSdk = 34

    defaultConfig {
        applicationId = "xyz.xiao6.myboard"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    packaging {
        jniLibs {
            // Force extraction of native libs to avoid 16 KB page size mmap issues.
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets {
        getByName("main") {
            // Be explicit to avoid accidentally dropping the default assets folder when customizing sourceSets.
            assets.srcDir(project.layout.projectDirectory.dir("src/main/assets"))
        }
    }
}

val generateSubtypes by tasks.registering(Exec::class) {
    // Write generated subtypes directly into src/main/assets so it is always packaged into the APK.
    // (Unreleased project workflow; avoid relying on build/generated assets at runtime.)
    val outFile = project.layout.projectDirectory.file("src/main/assets/subtypes/generated.json")

    inputs.file(project.rootDir.resolve("scripts/generate_subtypes.py"))
    inputs.dir(project.layout.projectDirectory.dir("src/main/assets/layouts"))
    inputs.dir(project.layout.projectDirectory.dir("src/main/assets/dictionary/meta"))
    outputs.file(outFile)

    // convertDictionaries writes `base.mybdict` into assets/dictionary; keep ordering explicit.
    dependsOn(convertDictionaries)
    commandLine(
        "python",
        project.rootDir.resolve("scripts/generate_subtypes.py").absolutePath,
        "--layouts-dir",
        project.layout.projectDirectory.dir("src/main/assets/layouts").asFile.absolutePath,
        "--dictionary-dir",
        project.layout.projectDirectory.dir("src/main/assets/dictionary/meta").asFile.absolutePath,
        "--output",
        outFile.asFile.absolutePath,
        "--fail-on-empty",
    )
}

val convertDictionaries by tasks.registering(Exec::class) {
    // Write generated dictionary directly into src/main/assets so it is always packaged into the APK.
    // (Unreleased project workflow; avoid relying on build/generated assets at runtime.)
    val outFile = project.layout.projectDirectory.file("src/main/assets/dictionary/base.mybdict")

    // Keep a draft meta json under build/ for reference (manual spec lives in assets/dictionary/meta/dict_pinyin.json).
    val outDir = layout.buildDirectory.dir("generated/dictionaryAssets/dictionary")
    val outMeta = outDir.map { it.file("dict_pinyin.generated.json") }

    inputs.file(project.rootDir.resolve("scripts/dict_tool.py"))
    inputs.files(
        project.layout.projectDirectory.file("src/main/assets/dictionary/8105.dict.yaml"),
        project.layout.projectDirectory.file("src/main/assets/dictionary/base.dict.yaml"),
        project.layout.projectDirectory.file("src/main/assets/dictionary/ext.dict.yaml"),
        project.layout.projectDirectory.file("src/main/assets/dictionary/cn_en.txt"),
        project.layout.projectDirectory.file("src/main/assets/dictionary/41448.dict.yaml"),
    )
    outputs.file(outFile)
    outputs.file(outMeta)

    commandLine(
        "python",
        project.rootDir.resolve("scripts/dict_tool.py").absolutePath,
        "convert-multi",
        "--inputs",
        listOf(
            project.layout.projectDirectory.file("src/main/assets/dictionary/8105.dict.yaml").asFile.absolutePath,
            project.layout.projectDirectory.file("src/main/assets/dictionary/base.dict.yaml").asFile.absolutePath,
            project.layout.projectDirectory.file("src/main/assets/dictionary/ext.dict.yaml").asFile.absolutePath,
            project.layout.projectDirectory.file("src/main/assets/dictionary/cn_en.txt").asFile.absolutePath,
            project.layout.projectDirectory.file("src/main/assets/dictionary/41448.dict.yaml").asFile.absolutePath,
        ).joinToString(","),
        "--dedupe",
        "first",
        "--output",
        outFile.asFile.absolutePath,
        "--dictionary-id",
        "dict_pinyin",
        "--name",
        "Pinyin",
        "--languages",
        "zh-CN",
        "--dict-version",
        "1.0.0",
        "--meta-output",
        outMeta.get().asFile.absolutePath,
        "--asset-path",
        "dictionary/base.mybdict",
        "--layout-ids",
        "qwerty,t9",
        "--code-scheme",
        "PINYIN_FULL",
        "--kind",
        "PINYIN",
        "--core",
        "PINYIN_CORE",
        "--variant",
        "quanpin",
        "--is-default",
        "--priority",
        "20",
        "--fail-on-empty",
    )
}

tasks.named("preBuild").configure {
    dependsOn(generateSubtypes)
    dependsOn(convertDictionaries)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.kotlinx.serialization.json)
    
    implementation(libs.digital.ink.recognition)

    // Room - 修正版本
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Accompanist - 修正版本
    implementation("com.google.accompanist:accompanist-pager:0.28.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.28.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
