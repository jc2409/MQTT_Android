import paho.mqtt.client as mqtt
import json
import os
import logging
import boto3
from botocore.exceptions import ClientError
import sys

# The callback for when the client receives a CONNACK response from the server.
def on_connect(client, userdata, flags, reason_code, properties):
    print(f"Connected with result code {reason_code}")
    # Subscribing in on_connect() means that if we lose the connection and
    # reconnect then subscriptions will be renewed.
    client.subscribe("sensor/data")

# The callback for when a PUBLISH message is received from the server.
def on_message(client, userdata, msg):
    try:
        data = msg.payload
        decoded_data = data.decode('utf-8')
        parsed_data = json.loads(decoded_data)
        temperature = parsed_data["temperature"] - 8
        predict(temperature)

    except:
        return 0
    
def predict(temperature):
    if set_mode == "heating":
        if (temperature <= set_temperature):
            statement = f'current temperature: {temperature:.2f}. Turning on the heating\n'
        else:
            statement = f'current temperature: {temperature:.2f}. Turning off the heating\n'
    elif set_mode == "air conditioning" or set_mode == "ac":
        if (temperature <= set_temperature):
            statement = f'current temperature: {temperature:.2f}. Turning off the AC\n'
        else:
            statement = f'current temperature: {temperature:.2f}. Turning on the AC\n'
    print(statement)
    with open('data.txt', 'a') as f:
        f.write(statement)
    
    # batch submission to AWS S3 bucket
    with open('data.txt', 'r') as f:
        if len(f.readlines()) % 5 == 0:
            upload_file('data.txt', bucket_name)         

def upload_file(file_name, bucket, object_name=None):
    if object_name is None:
        object_name = os.path.basename(file_name)

    # Upload the file
    s3_client = boto3.client('s3')
    try:
        response = s3_client.upload_file(file_name, bucket, object_name)
    except ClientError as e:
        logging.error(e)
        return False
    return True

def delete_file(file_name, bucket, object_name=None):
    if object_name is None:
        object_name = os.path.basename(file_name)
    
    s3_client = boto3.client('s3')
    try:
        response = s3_client.delete_object(Bucket=bucket, Key=object_name)
    except ClientError as e:
        logging.error(e)
        return False
    return True

if __name__ == '__main__':
    global set_temperature, mode, bucket_name

    if os.path.exists('data.txt'):
        open('data.txt', 'w').close()

    while True:
        set_mode = input("Enter the mode (Air Conditioning or Heating): ").lower()
        if set_mode == "air conditioning" or set_mode == "ac" or set_mode == "heating":
            break
        else:
            print("Invalid mode. Please enter either 'Air Conditioning' or 'Heating'.")
            continue
    set_temperature = float(input("Enter the temperature: "))
    bucket_name = input("Enter the AWS S3 bucket name: ")

    mqttc = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
    mqttc.on_connect = on_connect
    mqttc.on_message = on_message

    mqttc.connect("test.mosquitto.org", 1883, 60)

    while True:
        try:
            # Blocking call that processes network traffic, dispatches callbacks and
            # handles reconnecting.
            # Other loop*() functions are available that give a threaded interface and a
            # manual interface.
            mqttc.loop_forever()
        except KeyboardInterrupt:
            if os.path.exists('data.txt'):
                open('data.txt', 'w').close()
            # delete data.txt file in AWS S3 bucket
            delete_file('data.txt', bucket_name)
            print("Exiting...")
            sys.exit(0)