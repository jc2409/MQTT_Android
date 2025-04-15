# MQTT Client on Android

This repo is the next step of [https://github.com/jc2409/STM32L4_MQTT](https://github.com/jc2409/STM32L4_MQTT)

In the previous project, we could succesfully make connection between our MQTT client (B-L475E-IOT01A1 Board) and our MQTT broker on a Cloud Instance (AWS EC2 Instance).

In this project, we will create an Android app which invokes a message to an AWS Lambda Function through an HTTP request, and the Lambda function will process data and send it back to us.

First, install Android Studio and create a new project (Empty Views Activity). We will use Java as our main language.

After you create a new project, we will set up the UI to show the sensor data in a table format.

Go to `res/layout/activity_main.xml` and add the following layout:

```xml
<TableLayout
    android:id="@+id/myTableLayout"
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    android:layout_margin="16dp"
    app:layout_constraintTop_toTopOf="parent"
    app:layout_constraintBottom_toBottomOf="parent"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintEnd_toEndOf="parent">
</TableLayout>
```

Include dependencies in `build.gradle.kts (Module :app)` file as follows:
```kts
android {
    namespace = "com.example.<Projces-Name>"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.<Projces-Name>"
        minSdk = 24
        targetSdk = 35
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
    packaging {
        resources {
            excludes += listOf("META-INF/INDEX.LIST", "META-INF/io.netty.versions.properties")
        }
    }

}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.hivemq:hivemq-mqtt-client:1.3.5")
}
```

Allow internet conection in `AndroidManifest.xml`:
```xml
<manifest>
    <uses-permission android:name="android.permission.INTERNET"/>
</manifest>
```

After we add the UI and dependencies, it's time to develop the backend. In the MainActivity.java, there are three main components:

1. **MQTTClient**:

- We're using HiveMQ MQTT to make our Android device act as a client.
- Our serverHost will be the Public IPv4 address of the AWS EC2 Instance.
- This client will subscribe to the sensor/data topic to receive temperature data.

2. **Post to AWS Lambda**:

- Note: `git checkout python-mqtt-broker`
- There will be instructions to set up two distinct AWS Lambda functions: one to process data and upload it to DynamoDB, and another to retrieve that data.
- We will package all the necessary information in JSON format and post an HTTP request to the AWS Lambda function URL.

3. **Add data to the table layout**:

- After we receive the data, we will parse it and add it to the table layout.

We are sending a new request every 5 seconds. It is important to configure set_temperature (the temperature that you want to set) and mode (either "cooling" or "heating") before you run the program.

If everything is ready to be deployed, you can connect your Android device and deploy the app. If it's successful, you will be able to see the temperature data and status (whether on or off, depending on the mode and set_temperature).
