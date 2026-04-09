import json
import os
import boto3
import logging
from botocore.exceptions import ClientError
from aws_lambda_powertools.utilities.data_classes import APIGatewayProxyEvent
from FBPLib.fbpLog import fbpLog


'''
This function will return the user information for the given email address in the event.
'''

logger = logging.getLogger()
logger.setLevel(logging.INFO)

USERS_TABLE_NAME = os.environ.get('FBPUsersTableName', 'FBP-Users')
fbpLog("fbpadmin@my-fbp.com", "GetFBPUser", "Lambda function initialized", "INFO")
def lambda_handler(event, context):

    try:
        apigw_event = APIGatewayProxyEvent(event)
        logger.info(f"Received API Gateway event: {apigw_event}")
        body = apigw_event.json_body or {}
        logger.info(f"Parsed JSON body: {body}")
        email = body.get('email')
        logger.info(f"Extracted email from API Gateway event: {email}")

    except Exception as e:
        logger.error(f"Unexpected error: {e}")
        return {
            'statusCode': 400,
            'body': json.dumps({
                'error': 'Invalid request body',
                'message': 'Request body must be valid JSON with an email field'
            })
        }

    if not email:   
        return {
            'statusCode': 400,
            'body': json.dumps({
                'error': 'Email address is required',
                'message': 'Please provide email address in the event'
            })
        }
    item = get_fbp_user(email)
    
    if item:
        return {
            'statusCode': 200,
            'body': json.dumps({
                'email': item.get('email'),
                'defaultAlgorithm': item.get('defaultAlgorithm'),
                'displayName': item.get('displayName'),
                'emailGridSheet': item.get('emailGridSheet'),
                'emailPickSheet': item.get('emailPickSheet'),
                'emailReminders': item.get('emailReminders'),
                'firstName': item.get('firstName'),
                'lastName': item.get('lastName'),
                'isAccountLocked': item.get('isAccountLocked'),
                'isAdmin': item.get('isAdmin'),
                'isPaidUser': item.get('isPaidUser'),
                })
            }
    else:
        logger.info(f"User not found: {email}")
        return {
            'statusCode': 404,
            'body': json.dumps({
                'error': f'User with email {email} not found',
                'email': email,
                'name': None,
                'team': None
                })
            } 
        

def get_fbp_user(emailAddress):
    dynamodb = boto3.resource('dynamodb')
    table = dynamodb.Table(USERS_TABLE_NAME)
    try:
        response = table.get_item(
            Key={'email': emailAddress}
        )
        item = response['Item'] if 'Item' in response else None
        logger.info(f"Fetched user from DynamoDB: {json.dumps(item, default=str) if item else 'None'}")
        return item
    except ClientError as e:
        logger.error(f"DynamoDB Error: {e}")
        fbpLog("fbpadmin@my-fbp.com", "GetFBPUser", f"DynamoDB Error: {e}", "ERROR")
        return None
    except Exception as e:
        fbpLog("fbpadmin@my-fbp.com", "GetFBPUser", f"Unexpected error: {e}", "ERROR")
        logger.error(f"Unexpected error: {e}")
        return None
