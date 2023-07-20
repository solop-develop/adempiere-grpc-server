#!/usr/bin/env sh
# @autor Yamel Senih <ysenih@erpya.com>

# Set server values
sed -i "s|50059|$SERVER_PORT|g" env.yaml
sed -i "s|WARNING|$SERVER_LOG_LEVEL|g" env.yaml
sed -i "s|fill_secret_key|$SECRET_KEY|g" env.yaml
sed -i "s|fill_is_is_enabled_all_services|$IS_ENABLED_ALL_SERVICES|g" env.yaml

export DEFAULT_JAVA_OPTIONS='"-Xms64M" "-Xmx1512M"'

# create array to iterate
SERVICES_LIST=$(echo $SERVICES_ENABLED | tr "; " "\n")

SERVICES_LIST_TO_SET=""
for SERVICE_ITEM in $SERVICES_LIST
do
    # Service to lower case
    SERVICE_LOWER_CASE=$(echo $SERVICE_ITEM | tr '[:upper:]' '[:lower:]')

    NEW_LINE="\n"
    PREFIX="        - "
    if [ -z "$SERVICES_LIST_TO_SET" ]
    then
        NEW_LINE=""
        PREFIX="- "
    fi

    # Add to the list of services
    SERVICES_LIST_TO_SET="${SERVICES_LIST_TO_SET}${NEW_LINE}${PREFIX}${SERVICE_LOWER_CASE}"
done

sed -i "s|- services_enabled|$SERVICES_LIST_TO_SET|g" env.yaml

# Set data base conection values
sed -i "s|localhost|$DB_HOST|g" env.yaml
sed -i "s|5432|$DB_PORT|g" env.yaml
sed -i "s|adempiere_token_value|$SERVER_PRIVATE_KEY|g" env.yaml
sed -i "s|adempiere_database_value|$DB_NAME|g" env.yaml
sed -i "s|adempiere_user_value|$DB_USER|g" env.yaml
sed -i "s|adempiere_pass_value|$DB_PASSWORD|g" env.yaml
sed -i "s|PostgreSQL|$DB_TYPE|g" env.yaml
sed -i "s|$DEFAULT_JAVA_OPTIONS|$GRPC_JAVA_OPTIONS|g" bin/adempiere-all-in-one-server

# Run app
bin/adempiere-all-in-one-server env.yaml
