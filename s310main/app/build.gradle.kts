plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.s310main"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.s310main"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    packagingOptions {
        resources {
            excludes += setOf("META-INF/DEPENDENCIES")
        }
    }
}

dependencies {

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")

    implementation("com.google.firebase:firebase-auth")  // バージョン指定なしでfirebase-bomが管理
    implementation("com.google.firebase:firebase-database-ktx")


    implementation ("androidx.activity:activity-ktx:1.6.0")
    implementation ("androidx.activity:activity:1.6.0")

    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-analytics")

    implementation("com.google.oauth-client:google-oauth-client:1.34.1")
    implementation("com.google.api-client:google-api-client:1.32.1")
    implementation("com.google.api-client:google-api-client-gson:1.32.1")

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.7.0")
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.5.3")
    implementation("androidx.compose.runtime:runtime-livedata:1.3.3")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.firebase:firebase-auth-ktx:21.0.5")
    implementation("com.google.firebase:firebase-functions-ktx:21.2.1")
    implementation(libs.androidx.constraintlayout)
    // Kotlin互換性の問題が発生しにくいfirebase-authライブラリを使用

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Google Sheets API
    implementation("com.google.apis:google-api-services-sheets:v4-rev612-1.25.0")

    // Google認証
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Coroutine
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    // JSONライブラリの追加
    implementation("org.json:json:20210307")


    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.accompanist:accompanist-permissions:0.30.1")

    // 🔹 Google Maps Compose の追加
    implementation("com.google.maps.android:maps-compose:2.12.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")

    implementation("androidx.compose.material:material-icons-extended:1.6.0-beta01") // こちらも最新版を確認
    implementation("androidx.compose.material:material-icons-core:1.6.0") // または最新のバージョン
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") // または最新の安定版
    implementation("androidx.compose.runtime:runtime:1.6.0")

}
