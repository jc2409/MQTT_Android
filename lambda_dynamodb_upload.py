import json
import logging
import base64
import boto3
from botocore.exceptions import ClientError

logger = logging.getLogger()
logger.setLevel(logging.INFO)

def lambda_handler(event, context):
    try:
        logger.info("Received event: %s", event)
        
        # Check if the event has a "body" key.
        if "body" in event:
            body = event["body"]
            # Decode if the body is Base64-encoded.
            if event.get("isBase64Encoded", False):
                body = base64.b64decode(body).decode("utf-8")
            # Parse the JSON string into a dictionary.
            event = json.loads(body)
        
        # Validate required keys.
        for key in ["temperature", "set_temperature", "mode"]:
            if key not in event:
                raise KeyError(f"Missing required field: '{key}'")
        
        # Process the sensor data.
        original_temperature = float(event["temperature"])
        set_temperature = float(event["set_temperature"])
        set_mode = event["mode"].lower()
        
        # Adjust temperature by subtracting 8.
        processed_temperature = original_temperature - 8
        
        # Determine status based on mode.
        if set_mode == "heating":
            status = "on" if processed_temperature <= set_temperature else "off"
        elif set_mode == "cooling":
            status = "off" if processed_temperature <= set_temperature else "on"
        else:
            raise ValueError("Invalid mode provided. Acceptable values are 'heating', 'cooling', etc.")
        
        # Prepare result.
        res = {
            "temperature": processed_temperature,
            "set_temperature": set_temperature,
            "set_mode": set_mode,
            "status": status
        }
        
        # Upload to DynamoDB (ensure unique primary key if needed)
        if not upload_to_dynamodb(res):
            raise Exception("Failed to upload data to DynamoDB")
        
        return {
            "statusCode": 200,
            "body": json.dumps(res)
        }
    
    except Exception as e:
        logger.exception("Error processing sensor data:")
        return {
            "statusCode": 500,
            "body": json.dumps({"error": str(e)})
        }

def upload_to_dynamodb(res):
    dynamodb = boto3.client('dynamodb', region_name='us-east-1')
    try:
        dynamodb.put_item(
            TableName='MQTT_Sensor_Data',
            Item={
                'num': {'N': '1'},
                'temperature': {'S': str(res['temperature'])},
                'set_temperature': {'S': str(res['set_temperature'])},
                'set_mode': {'S': res['set_mode']},
                'status': {'S': res['status']}
            }
        )
        return True
    except ClientError as e:
        logger.error("DynamoDB put_item failed: %s", e)
        return False

