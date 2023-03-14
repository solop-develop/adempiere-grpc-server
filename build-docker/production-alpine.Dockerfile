ARG JDK_RELEASE $JDK_RELEASE
FROM eclipse-temurin:$JDK_RELEASE

LABEL	maintainer="ysenih@erpya.com; EdwinBetanc0urt@outlook.com" \
	description="ADempiere gRPC All In One Server used as ADempiere backend"

# Init ENV with default values
ENV \
	SERVER_PORT="50059" \
	SERVICES_ENABLED="access; business; business_partner; core; dashboarding; dictionary; enrollment; file_management; general_ledger; in_out; invoice; issue_management; log; material_management; order; payment; payment_print_export; payroll_action_notice; pos; product; store; time_control; time_record; ui; user_customization; workflow;" \
	SERVER_LOG_LEVEL="WARNING" \
	DB_HOST="localhost" \
	DB_PORT="5432" \
	DB_NAME="adempiere" \
	DB_USER="adempiere" \
	DB_PASSWORD="adempiere" \
	DB_TYPE="PostgreSQL" \
	ADEMPIERE_APPS_TYPE="wildfly" \
	TZ="America/Caracas"

WORKDIR /opt/Apps/backend/bin/

EXPOSE ${SERVER_PORT}

# Add connection template and start script files
ADD dist/backend /opt/Apps/backend/
ADD "build-docker/all_in_one_connection.yaml" "build-docker/start.sh" "/opt/Apps/backend/bin/"

RUN apk add --no-cache bash fontconfig ttf-dejavu && \
	addgroup adempiere && \
	adduser --disabled-password --gecos "" --ingroup adempiere --no-create-home adempiere && \
	chown -R adempiere /opt/Apps/backend/ && \
	chmod +x start.sh

USER adempiere

# Start app
ENTRYPOINT ["sh" , "start.sh"]
