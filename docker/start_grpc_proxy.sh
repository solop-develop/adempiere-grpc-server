#!/usr/bin/env sh
# @autor Edwin Betancourt <EdwinBetanc0urt@outlook.com> https://github.com/EdwinBetanc0urt

PROD_FILE=/etc/envoy/envoy.yaml


# copy `envoy_template.yaml` file to `envoy.yaml`
cp -rf /etc/envoy/envoy_template.yaml $PROD_FILE


# Set server values
sed -i "s|5555|$SERVER_PORT|g" $PROD_FILE


# # create array to iterate
# SERVICES_LIST=$(echo $SERVICES_ENABLED | tr "; " "\n")

# SERVICES_LIST_TO_SET=""
# for SERVICE_ITEM in $SERVICES_LIST
# do
# 	# Service to lower case
# 	SERVICE_LOWER_CASE=$(echo $SERVICE_ITEM | tr '[:upper:]' '[:lower:]')

# 	NEW_LINE="\n"
# 	PREFIX="                - "
# 	if [ -z "$SERVICES_LIST_TO_SET" ]
# 	then
# 		NEW_LINE=""
# 		PREFIX="- "
# 	fi

# 	# Add to the list of services
# 	SERVICES_LIST_TO_SET="${SERVICES_LIST_TO_SET}${NEW_LINE}${PREFIX}${SERVICE_LOWER_CASE}"
# done

# sed -i "s|- services_enabled|$SERVICES_LIST_TO_SET|g" $PROD_FILE

# Backend gRPC
sed -i "s|backend_host|$BACKEND_HOST|g" $PROD_FILE
sed -i "s|backend_port|$BACKEND_PORT|g" $PROD_FILE


# Run app
/usr/local/bin/envoy -c /etc/envoy/envoy.yaml
