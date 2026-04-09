import os
import boto3
from decimal import Decimal
from botocore.exceptions import ClientError
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger()

def getCurrentWeek():
    FBP_CONFIG_TABLE_NAME = os.environ.get('FBPConfigTableName', 'FBP-Config')
    logger.info(f"Fetching current week from DynamoDB table: {FBP_CONFIG_TABLE_NAME}")
    dynamodb = boto3.resource('dynamodb')
    table = dynamodb.Table(FBP_CONFIG_TABLE_NAME)
    try:
        '''
        There is only one row in FBP-Config.
        '''
        response = table.scan(Limit=1)
        item = response.get('Items')[0] if response.get('Items') else None
        if item:
            week_number = item.get('Week', None)
            if isinstance(week_number, Decimal):
                week_number = int(week_number)
            return week_number
        else:
            logger.error("No configuration found in FBP-Config table.")
            return None
    except ClientError as e:
        logger.error(f"DynamoDB Error: {e}")
        return None
    except Exception as e:
        logger.error(f"Unexpected error: {e}")
        return None
    
