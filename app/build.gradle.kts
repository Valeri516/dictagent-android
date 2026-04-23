plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.valera.dictagent"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.valera.dictagent"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "3.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
}
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.media:media:1.7.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
}
