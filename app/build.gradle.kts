plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
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
}


dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.media3.ui)
    implementation(libs.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    //recyclerview animators
    implementation ("jp.wasabeef:recyclerview-animators:4.0.2")
    //exoplayer
    implementation("androidx.media3:media3-exoplayer:1.1.0")
    //circle image
    implementation("de.hdodenhof:circleimageview:3.1.0")
    //audio visualiser
    implementation("io.github.gautamchibde:audiovisualizer:2.2.5")
    //extracting colors from artwork
    implementation("androidx.palette:palette:1.0.0")
    //blurImageView
    implementation("com.github.jgabrielfreitas:BlurImageView:1.0.1")
    implementation ("com.google.android.material:material:1.10.0")



}