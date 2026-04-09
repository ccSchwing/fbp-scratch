import json
import boto3
import logging
import os
from botocore.exceptions import ClientError
from aws_lambda_powertools.utilities.data_classes import APIGatewayProxyEvent


logger = logging.getLogger()
logger.setLevel(logging.INFO)

def lambda_handler(event, context):
    """
    Lambda function to retrieve poolOpen Boolean value from FBP-Config table
    Week number is passed via the event
    """
    
    # Initialize DynamoDB resource
    dynamodb = boto3.resource('dynamodb')
    table = dynamodb.Table('FBP-Config')
    try:
        week_number = None
        poolOpen = None

        apigw_event = APIGatewayProxyEvent(event)
        logger.info(f"Received API Gateway event body: {apigw_event.body}")
        body = apigw_event.json_body
        if body is None:
            raw_body = event.get('body') if isinstance(event, dict) else None
            if isinstance(raw_body, str) and raw_body:
                body = json.loads(raw_body)
            elif isinstance(raw_body, dict):
                body = raw_body
            elif isinstance(event, dict):
                body = event
            else:
                body = {}
        logger.info(f"Parsed JSON body: {body}")
        week_number = body.get('week')
        poolOpen = body.get('poolOpen')
        logger.info(f"Extracted week_number: {week_number}, poolOpen: {poolOpen} from API Gateway event")

        if isinstance(week_number, str) and week_number.strip():
            week_number = int(week_number)



    
        if week_number is None:
            return {
            'statusCode': 400,
            'body': json.dumps({
                'error': 'Week number is required',
                'message': 'Please provide week number in the event'
            })
        }
        if poolOpen is None:
            return {
            'statusCode': 400,
            'body': json.dumps({
                'error': 'poolOpen value is required',
                'message': 'Please provide poolOpen value (true or false) in the POST event'
            })
        }

        # Get PoolOpen and week frm FBP-Config table for the specified week number
        response = table.get_item(
            Key={
                'Week': week_number
            }
        )
        if 'Item' not in response:
            return {
                'statusCode': 404,
                'body': json.dumps({
                    'error': f'Configuration for week {week_number} not found',
                    'week': week_number
                })
            }
        current_week = response['Item'].get('Week', week_number)
        logger.info(f"Fetched configuration for week {current_week} from DynamoDB: {json.dumps(response['Item'], default=str)}")
        current_poolOpen = response['Item'].get('poolOpen', False)
        logger.info(f"Current poolOpen value for week {week_number} is: {current_poolOpen}")

        # Update DynamoDB table with the new poolOpen value for the specified week

        table.update_item(
            Key={
                'Week': week_number
            },
            UpdateExpression='SET poolOpen = :val',
            ExpressionAttributeValues={
                ':val': poolOpen
            }
        )

        response = table.get_item(
            Key={
                'Week': week_number
            }
        )
        logger.info(f"Updated poolOpen value for week {week_number} to: {poolOpen}")
        # Check if item exists and return poolOpen value
        if 'Item' in response:
            poolOpen = response['Item'].get('poolOpen', False)
            logger.info(f"Returning updated poolOpen value for week {week_number}: {poolOpen}")
            return {
                'statusCode': 200,
                'body': json.dumps({
                    'week': week_number,
                    'poolOpen': poolOpen
                })
            }
        else:
            return {
                'statusCode': 404,
                'body': json.dumps({
                    'error': f'Configuration for week {week_number} not found',
                    'week': week_number,
                    'poolOpen': False
                })
            }
            
    except ClientError as e:
        print(f"DynamoDB Error: {e}")
        return {
            'statusCode': 500,
            'body': json.dumps({
                'error': 'Database error',
                'details': str(e)
            })
        }
    except Exception as e:
        print(f"Unexpected error: {e}")
        return {
            'statusCode': 500,
            'body': json.dumps({
                'error': 'Internal server error'
            })
        }
