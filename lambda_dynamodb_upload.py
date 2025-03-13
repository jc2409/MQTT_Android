import json
import logging
import boto3
from botocore.exceptions import ClientError

logger = logging.getLogger()
logger.setLevel(logging.INFO)

def lambda_handler(event, context):
    """
    Processes sensor data sent as JSON from an Android MQTT client.
    Expected JSON payload:
    {
        "temperature": <sensor reading>,
        "set_temperature": <desired threshold>,
        "mode": "heating" or "air conditioning"  // ("ac" is also accepted)
    }
    The function subtracts 8 from the temperature, applies the appropriate logic,
    stores the processed data in DynamoDB, and returns the result.
    """
    try:
        # Validate and retrieve required keys from the event payload
        for key in ["temperature", "set_temperature", "mode"]:
            if key not in event:
                raise KeyError(f"Missing required field: '{key}'")
        
        original_temperature = float(event["temperature"])
        set_temperature = float(event["set_temperature"])
        set_mode = event["mode"].lower()
        
        # Adjust temperature by subtracting 8
        processed_temperature = original_temperature - 8
        
        # Determine status based on the mode and temperature
        if set_mode == "heating":
            status = "on" if processed_temperature <= set_temperature else "off"
        elif set_mode in ["air conditioning", "ac"]:
            status = "off" if processed_temperature <= set_temperature else "on"
        else:
            raise ValueError("Invalid mode provided. Acceptable values are 'heating', 'air conditioning', or 'ac'")
        
        # Prepare the result object
        res = {
            "temperature": processed_temperature,
            "set_temperature": set_temperature,
            "set_mode": set_mode,
            "status": status
        }
        
        # Upload the result to DynamoDB
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
    """
    Uploads the processed data to the DynamoDB table.
    Ensure that the table 'MQTT_Processed_Data' exists and the key schema matches the attributes used.
    """
    dynamodb = boto3.client('dynamodb', region_name='us-east-1')
    try:
        dynamodb.put_item(
            TableName='MQTT_Processed_Data',
            Item={
                'Temperature': {'S': str(res['temperature'])},
                'set_temperature': {'S': str(res['set_temperature'])},
                'set_mode': {'S': res['set_mode']},
                'status': {'S': res['status']}
            }
        )
        return True
    except ClientError as e:
        logger.error("DynamoDB put_item failed: %s", e)
        return False
