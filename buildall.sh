#!/bin/bash

# rm -rf .aws-sam/build/FBPLibLayer
sam build -t min.yaml --use-container --no-cached FBPLibLayer

sam build -t min.yaml --use-container --no-cached CalcWeeklyResultsPython GetWeeklyResultsPython SaveFBPPicksPython GetPickSheetPython GetPoolOpenEvent GetPoolConfig SetPoolStatusOpen SetPoolStatusClosed GetFBPUserPython UpdateFBPUserPython AddFBPUserPython GetFBPPicksPython GetAllFBPPicksPython
sam deploy -t .aws-sam/build/template.yaml --stack-name fbp-lambda-v2 --region us-east-1 --capabilities CAPABILITY_IAM --resolve-s3 --s3-prefix fbp-lambda-v2 --force-upload
