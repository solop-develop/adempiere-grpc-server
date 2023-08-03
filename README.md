# ADempiere gRPC Server

<p align="center">
  <a href="https://adoptium.net/es/temurin/releases/?version=11">
    <img src="https://badgen.net/badge/Java/11/orange" alt="Java">
  </a>
  <a href="https://hub.docker.com/r/solopcloud/adempiere-backend">
    <img src="https://img.shields.io/docker/pulls/solopcloud/adempiere-backend.svg" alt="Docker Pulls">
  </a>
  <a href="https://github.com/solop-develop/backend/actions/workflows/publish.yml">
    <img src="https://github.com/solop-develop/backend/actions/workflows/publish.yml/badge.svg" alt="Publish GH Action">
  </a>
  <a href="https://github.com/solop-develop/backend/blob/master/LICENSE">
    <img src="https://img.shields.io/badge/license-GNU/GPL%20(v2)-blue" alt="License">
  </a>
  <a href="https://github.com/solop-develop/backend/releases/latest">
    <img src="https://img.shields.io/github/release/solop-develop/backend.svg" alt="GitHub release">
  </a>
  <a href="https://discord.gg/T6eH6A7PJZ">
    <img src="https://badgen.net/badge/discord/join%20chat" alt="Discord">
  </a>
</p>

English | [Spanish](./README.es.md)

This is a project to publish ADempiere, over gRPC service, decoupling in modules such as Access, Application Dictionary, Business, Point Of Sales, Workflow, Dashboarding, File Management, Logs, Requests (as Issue Management), User Customization, etc.


## Recreate proto stub class
For recreate stub class you must have follow:
- [protobuf](https://github.com/protocolbuffers/protobuf/releases)
- [gradle](https://gradle.org/install)
- [Maven](https://search.maven.org/)
- Also you can see it: [gRPC-Java](https://grpc.io/docs/quickstart/java.html)
- [Complete Java Documentation](https://grpc.io/docs/tutorials/basic/java.html)
After installed it just go to source code folder an run it:

## Runing as development
### Clean
```shell
gradle clean
```

### Execute server with default conection file
``` bash
gradle run
```

### Execute server with custom conection file
```shell
gradle run --args="'resources/standalone.yaml'"
```

## Runing Server
The server can be running as java class. See it: **org.spin.server.AllInOneServices**
Don't forgive that for run server you need set `yaml` file line is `/resources` folder.

- Use latest [release](https://github.com/solop-develop/backend/releases)
- Unzip binary
- Go to bin folder
- Run it

```shell
./adempiere-all-in-one-server "./resources/standalone.yaml"
```


### For all enviroment you should run the follow images:

- ADempiere Postgres: https://hub.docker.com/r/solopcloud/adempiere-postgres
```shell
docker pull solopcloud/adempiere-postgres
```

- ADempiere Backend: https://hub.docker.com/r/solopcloud/adempiere-backend
```shell
docker pull solopcloud/adempiere-backend:experimental
```

- ADempiere Proxy: https://hub.docker.com/r/solopcloud/adempiere-proxy
```shell
docker pull solopcloud/adempiere-proxy:experimental
```

- Frontend-Core: https://hub.docker.com/r/solopcloud/adempiere-vue
```shell
docker pull solopcloud/adempiere-vue:experimental
```

## Run docker container:

### Minimal Docker Requirements
To use this Docker image you must have your Docker engine version greater than or equal to 3.0.

### Environment variables
 * `DB_TYPE`: Database Type (Supported `Oracle` and `PostgreSQL`). Default `PostgreSQL`.
 * `DB_HOST`: Hostname for data base server. Default: `localhost`.
 * `DB_PORT`: Port used by data base server. Default: `5432`.
 * `DB_NAME`: Database name that Adempiere-Backend will use to connect with the database. Default: `adempiere`.
 * `DB_USER`: Database user that Adempiere-Backend will use to connect with the database. Default: `adempiere`.
 * `DB_PASSWORD`: Database password that Adempiere-Backend will use to connect with the database. Default: `adempiere`.
 * `SECRET_KEY`: Secret key, for the encryption hash of the Json Web Token, the default value is `98D8032045502303C1F97FE5A5D40750A6D16D97C20A7BD9C757D2E957F2CA6E` this must be changed for security reasons.
 * `ADEMPIERE_APPS_TYPE`: Application Type for Database Management Connection. Default: `wildfly`.
 * `SERVER_PORT`: Port to access Adempiere-Backend from outside of the container. Default: `50059`.
 * `SERVICES_ENABLED`: Services enabled. Default: `bank_statement_match; business; business_partner; core; dashboarding; dictionary; enrollment; express_movement; express_receipt; express_shipment; file_management; general_ledger; import_file_loader; in_out; invoice; issue_management; log; match_po_receipt_invoice; material_management; order; payment; payment_allocation; payment_print_export; payroll_action_notice; pos; product; record_management; security; store; time_control; time_record; ui; user_customization; workflow;`.
 * `IS_ENABLED_ALL_SERVICES`: If it is true, omit the value of `SERVICES_ENABLED` and start all services, the default value is `true`.
 * `SERVER_LOG_LEVEL`: Log Level. Default: `WARNING`.
 * `TZ`: (Time Zone) Indicates the time zone to set in the nginx-based container, the default value is `America/Caracas` (UTC -4:00).

### Build docker image (for development only):
First compile output files.
``` bash
# Assembles the outputs of this project.
gradle assemble

# Or Assembles and tests this project.
gradle build
```

Build docker image
```shell
docker build -t solopcloud/adempiere-backend:dev -f ./build-docker/development.Dockerfile .
```

### Download docker image:
```shell
docker pull solopcloud/adempiere-backend:alpine
```

### Run container container:
```shell
docker run -it -d \
	--name adempiere-backend \
	-p 50059:50059 \
	-e DB_HOST="your-db-host" \
	-e DB_PORT="5432" \
	-e DB_NAME=\"adempiere\" \
	-e DB_PASSWORD="adempiere" \
	-e TZ="America/Caracas" \
	solopcloud/adempiere-backend:alpine
```

### Run with Docker-Compose
Or easy run container using `docker-compose` with follow command:
```shell
docker compose -f build-docker/docker-compose.yaml up
```
