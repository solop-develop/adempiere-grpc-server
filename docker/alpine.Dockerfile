FROM eclipse-temurin:11-jdk-alpine

LABEL maintainer="ysenih@erpya.com; EdwinBetanc0urt@outlook.com" \
	description="ADempiere gRPC All In One Server used as ADempiere adempiere-grpc-server"

# Init ENV with default values
ENV \
	SERVER_PORT="50059" \
	IS_ENABLED_ALL_SERVICES="true" \
	SERVICES_ENABLED="bank_statement_match; business; business_partner; core; dashboarding; dictionary; enrollment; express_movement; express_receipt; express_shipment; file_management; general_ledger; import_file_loader; in_out; invoice; issue_management; log; match_po_receipt_invoice; material_management; order; payment; payment_allocation; payment_print_export; payroll_action_notice; pos; product; record_management; security; store; time_control; time_record; ui; user_customization; workflow;" \
	SERVER_LOG_LEVEL="WARNING" \
	SECRET_KEY="A42CF908019918B1D9D9E04E596658345D162D4C0127A4C8365E8BDF6B015CC7" \
	DB_HOST="localhost" \
	DB_PORT="5432" \
	DB_NAME="adempiere" \
	DB_USER="adempiere" \
	DB_PASSWORD="adempiere" \
	DB_TYPE="PostgreSQL" \
	ADEMPIERE_APPS_TYPE="wildfly" \
	TZ="America/Caracas"

EXPOSE ${SERVER_PORT}


# Add operative system dependencies
RUN rm -rf /tmp/* && \
	apk update && apk add --no-cache \
		tzdata \
		bash \
		fontconfig \
		ttf-dejavu && \
	echo "Set Timezone..." && \
	echo $TZ > /etc/timezone


WORKDIR /opt/apps/server

# Copy src files
COPY docker/adempiere-grpc-server /opt/apps/server
COPY docker/env.yaml /opt/apps/server/env.yaml
COPY docker/start.sh /opt/apps/server/start.sh


RUN addgroup adempiere && \
	adduser --disabled-password --gecos "" --ingroup adempiere --no-create-home adempiere && \
	chown -R adempiere /opt/apps/server/ && \
	chmod +x start.sh

USER adempiere

# Start app
ENTRYPOINT ["sh" , "start.sh"]
