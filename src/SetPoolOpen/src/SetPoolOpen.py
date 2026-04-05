import json
import boto3
from botocore.exceptions import ClientError

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

        if(event.get('queryStringParameters') and event['queryStringParameters'].get('week') is not None):
            week_number = event['queryStringParameters'].get('week')
        elif 'week' in event and event.get('week') is not None:
            week_number = event.get('week')

        if(event.get('queryStringParameters') and event['queryStringParameters'].get('poolOpen') is not None):
            poolOpen = event['queryStringParameters'].get('poolOpen')
        elif 'poolOpen' in event and event.get('poolOpen') is not None:
            poolOpen = event.get('poolOpen')
    
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
                'message': 'Please provide poolOpen value (true or false)in the event'
            })
        }

        if isinstance(poolOpen, str):
            lowered = poolOpen.strip().lower()
            if lowered == 'true':
                poolOpen = True
            elif lowered == 'false':
                poolOpen = False
            else:
                return {
                    'statusCode': 400,
                    'body': json.dumps({
                        'error': 'Invalid poolOpen value',
                        'message': 'poolOpen must be true or false'
                    })
                }
        elif not isinstance(poolOpen, bool):
            return {
                'statusCode': 400,
                'body': json.dumps({
                    'error': 'Invalid poolOpen value',
                    'message': 'poolOpen must be a boolean'
                })
            }

        # Convert to integer if it's a string
        try:
            week_number = int(week_number)
        except (ValueError, TypeError):
            return {
                'statusCode': 400,
                'body': json.dumps({
                    'error': 'Invalid week number',
                    'message': 'Week must be a valid number'
                })
            }
        
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
        
        # Check if item exists and return poolOpen value
        if 'Item' in response:
            poolOpen = response['Item'].get('poolOpen', False)
            
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
