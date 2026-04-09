import json
import os
import boto3
import logging
from decimal import Decimal
from botocore.exceptions import ClientError
from aws_lambda_powertools.utilities.data_classes import APIGatewayProxyEvent
from FBPLib import getCurrentWeek
from FBPLib import fbpLog

logger = logging.getLogger()
logger.setLevel(logging.INFO)

FBP_PICKS_TABLE_NAME = os.environ.get('FBPPicksTableName', 'FBP-Picks')
logger.info(f"Using FBP Picks DynamoDB table: {FBP_PICKS_TABLE_NAME}")

CORS_HEADERS = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET,OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token',
    'Content-Type': 'application/json',
}


def decimal_default(value):
    if isinstance(value, Decimal):
        return int(value) if value % 1 == 0 else float(value)
    raise TypeError(f"Object of type {type(value).__name__} is not JSON serializable")


def lambda_handler(event, context):
    if event.get('httpMethod') == 'OPTIONS':
        return {'statusCode': 200, 'headers': CORS_HEADERS, 'body': ''}

    week = getCurrentWeek.getCurrentWeek()
    if week is None:
        logger.error("Could not determine current week")
        return {
            'statusCode': 500,
            'headers': CORS_HEADERS,
            'body': json.dumps({'error': 'Could not determine current week'}),
        }

    logger.info(f"Fetching all FBP picks for week: {week}")
    fbpLog.fbpLog("fbpadmin@my-fbp.com", "GetAllFBPPicks", f"Fetching all FBP picks for week: {week}", "INFO", week)
    picks = get_all_picks_for_week(week)
    logger.info(f"Retrieved {len(picks)} picks for week {week}")
    fbpLog.fbpLog("fbpadmin@my-fbp.com", "GetAllFBPPicks", f"Retrieved {len(picks)} picks for week {week}", "INFO", week)
    return {
        'statusCode': 200,
        'headers': CORS_HEADERS,
        'body': json.dumps(picks, default=decimal_default),
    }


def get_all_picks_for_week(week):
    dynamodb = boto3.resource('dynamodb')
    table = dynamodb.Table(FBP_PICKS_TABLE_NAME)
    try:
        response = table.scan(
            FilterExpression=boto3.dynamodb.conditions.Attr('week').eq(week)
        )
        items = response.get('Items', [])
        # Handle pagination
        while 'LastEvaluatedKey' in response:
            response = table.scan(
                FilterExpression=boto3.dynamodb.conditions.Attr('week').eq(week),
                ExclusiveStartKey=response['LastEvaluatedKey']
            )
            items.extend(response.get('Items', []))

        items.sort(key=lambda x: (x.get('displayName') or '').lower())
        return items
    except ClientError as e:
        logger.error(f"DynamoDB Error: {e}")
        return []
    except Exception as e:
        logger.error(f"Unexpected error: {e}")
        return []
