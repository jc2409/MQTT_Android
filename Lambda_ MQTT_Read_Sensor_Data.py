import simplejson as json
import boto3
import decimal
from boto3.dynamodb.conditions import Key

def lambda_handler(event, context):
    # Initialize a DynamoDB resource
    dynamodb = boto3.resource('dynamodb', region_name='us-east-1')
    
    # Get the table; replace 'MQTT_Sensor_Data' with your table name
    table = dynamodb.Table('MQTT_Sensor_Data')
    
    response = table.get_item(Key={"num": 1})
    
    # Retrieve all items from the response
    item = response.get('Item', [])
    
    # Return all items as a JSON response using the custom encoder
    return {
        'statusCode': 200,
        'body': json.dumps(item, use_decimal=True)
    }
