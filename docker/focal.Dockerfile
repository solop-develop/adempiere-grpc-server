FROM eclipse-temurin:11-jdk-focal

LABEL maintainer="ySenih@erpya.com; EdwinBetanc0urt@outlook.com" \
	description="ADempiere gRPC All In One Server used as ADempiere adempiere-grpc-server"

# Init ENV with default values
ENV \
	SERVER_PORT="50059" \
	IS_ENABLED_ALL_SERVICES="true" \
	SERVICES_ENABLED="bank_statement_match; business; business_partner; core; \
		dashboarding; dictionary; enrollment; express_movement; express_receipt; \
		express_shipment; file_management; general_ledger; import_file_loader; \
		in_out; invoice; issue_management; location_address; log; match_po_receipt_invoice; \
		material_management; notice_management; order; payment; payment_allocation; payment_print_export; \
		payroll_action_notice; pos; product; record_management; report_management; \
		security; store; time_control; time_record; trial_balance_drillable; \
		user_customization; user_interface; workflow;" \
	SERVER_LOG_LEVEL="WARNING" \
	JWT_SECRET_KEY="2C51599F5B1248F945B93E05EFC43B3A15D8EB0707C0F02FD97028786C40976F" \
	JWT_EXPIRATION_TIME=86400000 \
	DB_HOST="localhost" \
	DB_PORT="5432" \
	DB_NAME="adempiere" \
	DB_USER="adempiere" \
	DB_PASSWORD="adempiere" \
	DB_TYPE="PostgreSQL" \
	ADEMPIERE_APPS_TYPE="wildfly" \
	SYSTEM_LOGO_URL="" \
	TZ="America/Caracas"

EXPOSE ${SERVER_PORT}


# Add operative system dependencies
RUN rm -rf /var/lib/apt/lists/* && \
	rm -rf /tmp/* && \
	apt-get update && apt-get install -y \
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
