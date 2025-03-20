package com.example.mqtt_test;

import android.os.Bundle;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttClientBuilder;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Mqtt3AsyncClient asyncClient;
    private volatile boolean isRunning = true;
    private volatile double MqttTemperature = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Build and connect the MQTT client.
        MqttClientBuilder clientBuilder = MqttClient.builder()
                .identifier(UUID.randomUUID().toString())
                .serverHost("Your AWS EC2 Public IPv4 address") // Replace with your broker IP address
                .serverPort(1883);

        Mqtt3Client client = clientBuilder.useMqttVersion3().build();
        asyncClient = client.toAsync();

        asyncClient.connectWith().send().whenComplete((connAck, throwable) -> {
            runOnUiThread(() -> {
                if (throwable != null) {
                    Toast.makeText(MainActivity.this, "MQTT connection failed: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    throwable.printStackTrace();
                } else {
                    Toast.makeText(MainActivity.this, "Connected to MQTT broker!", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Subscribe to the MQTT topic "sensor/data".
        asyncClient.subscribeWith()
                .topicFilter("sensor/data")
                .callback(publish -> {
                    String mqttData = new String(publish.getPayloadAsBytes());
                    try {
                        JSONObject mqttPayload = new JSONObject(mqttData);
                        MqttTemperature = mqttPayload.getDouble("temperature");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    // Do not unsubscribe so that data is continuously received.
                })
                .send();

        // Start a background thread to fetch data every 5 seconds.
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning) {
                    fetchData();
                    try {
                        Thread.sleep(5000); // Wait for 5 seconds.
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop the background thread loop.
        isRunning = false;
    }

    /**
     * Fetches sensor data, calls AWS Lambda functions, and updates the UI.
     * Here, the userTemperature is read from the latest MQTT data.
     */
    private void fetchData() {
        double userTemperature = MqttTemperature;

        // Build JSON payload with the MQTT temperature.
        JSONObject jsonPayload = new JSONObject();
        try {
            jsonPayload.put("temperature", userTemperature);
            jsonPayload.put("set_temperature", "23");
            jsonPayload.put("mode", "cooling");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Replace these URLs with your actual AWS Lambda endpoints.
        String processSensorDataURL = "Your AWS Lambda Function URL";
        String readSensorDataURL = "Your AWS Lambda Function URL";

        try {
            // Call Lambda function 1 with JSON payload.
            String response1 = postJson(processSensorDataURL, jsonPayload.toString());
            // Call Lambda function 2 (read sensor data) with an empty payload.
            String response2 = postJson(readSensorDataURL, "");

            // Parse the processed data from the response.
            JSONObject processedData = new JSONObject(response2);
            final String temperature = processedData.getString("temperature");
            final String setTemperature = processedData.getString("set_temperature");
            final String mode = processedData.getString("set_mode");
            final String status = processedData.getString("status");

            // Update the UI on the main thread.
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TableLayout tableLayout = findViewById(R.id.myTableLayout);
                    tableLayout.removeAllViews(); // Clear previous rows.
                    addRowToTable(tableLayout, "Temperature", temperature);
                    addRowToTable(tableLayout, "Set Temperature", setTemperature);
                    addRowToTable(tableLayout, "Mode", mode);
                    addRowToTable(tableLayout, "Status", status);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error calling lambda functions", Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * Helper method to POST JSON data to a URL and return the response as a String.
     */
    private String postJson(String urlString, String jsonPayload) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);

        if (jsonPayload != null && !jsonPayload.isEmpty()) {
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes("UTF-8"));
            }
        }

        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode < HttpURLConnection.HTTP_BAD_REQUEST)
                ? conn.getInputStream()
                : conn.getErrorStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        conn.disconnect();
        return response.toString();
    }

    /**
     * Helper method to add a row with a field and value to a TableLayout.
     */
    private void addRowToTable(TableLayout tableLayout, String field, String value) {
        TableRow row = new TableRow(this);

        TextView tvField = new TextView(this);
        tvField.setText(field);
        tvField.setPadding(8, 8, 8, 8);

        TextView tvValue = new TextView(this);
        tvValue.setText(value);
        tvValue.setPadding(8, 8, 8, 8);

        row.addView(tvField);
        row.addView(tvValue);

        tableLayout.addView(row);
    }
}
