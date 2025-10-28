plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "metro.plascreem"
    compileSdk = 35

    defaultConfig {
        applicationId = "metro.plascreem"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        // 1. Definir la clave en una variable, incluyendo las comillas que necesita el String de Java.
        val fcmServerKey =
            "\"BCIGKBSueN26-106y122fTCtA85RQQ7_-Jmy1LsLhXiBPeAtS-tpu4gMq-tkAv67594iUeQN0rNEhpxtDR6mRUE\""
        // 2. Usar la variable en el buildConfigField. Esto evita errores de anidamiento de comillas.
        buildConfigField("String", "FCM_SERVER_KEY", fcmServerKey)

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
        compose = true
        viewBinding = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1" // Usa la versión más reciente
    }
}

dependencies {


    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // Compose
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")
    implementation("androidx.activity:activity-compose:1.8.1")
// LiveData
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    // ... otras dependencias
    implementation ("androidx.recyclerview:recyclerview:1.3.2") // O la versión más reciente
    implementation ("com.google.android.material:material:1.12.0") // O la versión más reciente
    //firebase
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")


    // Volley para peticiones de red
    implementation("com.android.volley:volley:1.2.1")



    // Apache POI for Excel
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")

}