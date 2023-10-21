#!/usr/bin/env sh
# @autor Yamel Senih <ysenih@erpya.com>

PROD_FILE=/etc/envoy/envoy.yaml

# copy template.json file to local.json
cp /etc/envoy/envoy_template.yaml $PROD_FILE


# Set server values
sed -i "s|port_value: 5555|$SERVER_PORT|g" $PROD_FILE
# sed -i "s|fill_is_is_enabled_all_services|$IS_ENABLED_ALL_SERVICES|g" $PROD_FILE

export DEFAULT_JAVA_OPTIONS='"-Xms64M" "-Xmx1512M"'

# create array to iterate
SERVICES_LIST=$(echo $SERVICES_ENABLED | tr "; " "\n")

SERVICES_LIST_TO_SET=""
for SERVICE_ITEM in $SERVICES_LIST
do
	# Service to lower case
	SERVICE_LOWER_CASE=$(echo $SERVICE_ITEM | tr '[:upper:]' '[:lower:]')

	NEW_LINE="\n"
	PREFIX="                - "
	if [ -z "$SERVICES_LIST_TO_SET" ]
	then
		NEW_LINE=""
		PREFIX="- "
	fi

	# Add to the list of services
	SERVICES_LIST_TO_SET="${SERVICES_LIST_TO_SET}${NEW_LINE}${PREFIX}${SERVICE_LOWER_CASE}"
done

sed -i "s|- services_enabled|$SERVICES_LIST_TO_SET|g" $PROD_FILE

# Backend gRPC
sed -i "s|backend_host|$BACKEND_HOST|g" $PROD_FILE
sed -i "s|backend_port|$BACKEND_PORT|g" $PROD_FILE

# Run app
bin/adempiere-all-in-one-server $PROD_FILE
