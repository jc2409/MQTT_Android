package com.example.mqtt_test;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttClientBuilder;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MainActivity extends AppCompatActivity {

    private TextView connectionResultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Find the TextView from your layout
        connectionResultTextView = findViewById(R.id.connectionResult);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Build the MQTT client
        MqttClientBuilder clientBuilder = MqttClient.builder()
                .identifier(UUID.randomUUID().toString())
                .serverHost("test.mosquitto.org")
                .serverPort(1883);

        Mqtt3Client client = clientBuilder.useMqttVersion3().build();
        Mqtt3AsyncClient asyncClient = client.toAsync();

        // Connect to the MQTT broker
        CompletableFuture<Mqtt3ConnAck> connAckFuture = asyncClient.connectWith().send();

        connAckFuture.whenComplete((connAck, throwable) -> {
            runOnUiThread(() -> {
                if (throwable != null) {
                    // Update UI with connection error
                    connectionResultTextView.setText("Connection failed: " + throwable.getMessage());
                    throwable.printStackTrace();
                } else {
                    // Connection successful
                    connectionResultTextView.setText("Connected to MQTT broker!");

                    // Subscribe to a topic (replace "my/topic" with your desired topic)
                    asyncClient.subscribeWith()
                            .topicFilter("sensor/data")
                            .callback(publish -> {
                                // Get the payload as a String
                                String receivedMessage = new String(publish.getPayloadAsBytes());
                                runOnUiThread(() -> {
                                    // Update UI with the received message
                                    connectionResultTextView.setText("Received: " + receivedMessage);
                                });
                            })
                            .send();
                }
            });
        });
    }
}
