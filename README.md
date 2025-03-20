# Configuring AWS Lambda Functions to Process Sensor Data

As your Android device posts an HTTP request to AWS Lambda functions, they will be executed.

## Lambda_MQTT_Process_Sensor_Data.py

`Lambda_MQTT_Process_Sensor_Data.py` will receive JSON data from your device in the following format:

```json
{
    "temperature": "Temperature value read from the ST Board",
    "set_temperature": "Temperature value set by an user",
    "mode": "cooling or heating set by an user"
}
```

It will then decide whether it has to set the status to `on` or `off` depending on the user setting.

For example, if you set the mode to `cooling`, `set_temperature` as 18 and if the current temperature reading from the ST Board is higher than 18°C, it will set the mode to `on`.
You can imagine this as a very simple replication of an IoT smart controller for an air conditioner.

## DynamoDB Integration and Data Retrieval
After the sensor data is processed, it will be stored in AWS DynamoDB. This data will be read by the second function `Lambda_MQTT_Read_Sensor_Data.py`, which will then return the stored data to be read by your Android device.

## Important Notes
After you create these two functions in your AWS console, you should:

1. Create function URLs for each of these functions and set the Auth type as **NONE**. Add these URLs into your Android main function as well.
2. Give both functions access to AWS DynamoDB to **Read** and **Write**.
