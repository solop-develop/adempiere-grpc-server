FROM gradle:8.0.2 as builder
ARG BUILD_ARTIFACT \
	GITHUB_DEPLOY_USER \
	GITHUB_DEPLOY_TOKEN \
	GITHUB_DEPLOY_REPOSITORY

LABEL maintainer="ysenih@erpya.com; EdwinBetanc0urt@outlook.com" \
	description="ADempiere gRPC All In One Server used as ADempiere adempiere-grpc-server"

WORKDIR /opt/apps/server

COPY . . /opt/apps/server/

EXPOSE ${SERVER_PORT}

ENV GITHUB_DEPLOY_USER=$GITHUB_DEPLOY_USER \
	GITHUB_DEPLOY_TOKEN=$GITHUB_DEPLOY_TOKEN \ 
	GITHUB_DEPLOY_REPOSITORY=$GITHUB_DEPLOY_REPOSITORY

RUN gradle build && \
	unzip -d /opt/apps build/distributions/adempiere-grpc-server.zip && \
	mv docker/env.yaml /opt/apps/adempiere-grpc-server && \
	mv docker/start.sh /opt/apps/adempiere-grpc-server

FROM eclipse-temurin:11-jdk-alpine

# Init ENV with default values
ENV \
	SERVER_PORT="50059" \
	SERVICES_ENABLED="business; business_partner; core; dashboarding; dictionary; enrollment; express_receipt; express_shipment; file_management; general_ledger; in_out; invoice; issue_management; log; material_management; order; payment; payment_print_export; payroll_action_notice; pos; product; security; store; time_control; time_record; ui; user_customization; workflow;" \
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

WORKDIR /opt/apps/server

# Add operative system dependencies
RUN	rm -rf /var/cache/apk/* && \
	rm -rf /tmp/* && \
	apk update && \
	apk add --no-cache \
		bash \
	 	fontconfig \
		ttf-dejavu && \
		echo "Set Timezone..." && \
	 	echo $TZ > /etc/timezone && \
                apk add --no-cache \
		tzdata


COPY --from=builder /opt/apps/adempiere-grpc-server/ /opt/apps/server/

RUN addgroup adempiere && \
	adduser --disabled-password --gecos "" --ingroup adempiere --no-create-home adempiere && \
	chown -R adempiere /opt/apps/server/ && \
	chmod +x start.sh

USER adempiere

# Start app
ENTRYPOINT ["sh" , "start.sh"]
