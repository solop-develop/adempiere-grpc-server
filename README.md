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
 * `IDLE_TIMEOUT`: It sets the maximum time a connection can sit around without being used before it gets closed to free up resources. Default: `300`.
 * `MINIMUM_IDLE`: It sets the minimum number of connections that should be kept open and ready to use, even if they're not currently being used. This helps improve performance by reducing the time it takes to get a connection. Default: `1`.
 * `MAXIMUM_POOL_SIZE`: It sets the maximum number of connections that can be open at the same time. This helps prevent the pool from getting too big and using up too much memory. Default: `10`.
 * `CONNECTION_TIMEOUT`: it sets the maximum time HikariCP will wait to get a connection from the pool before giving up and throwing an error. Default: `5000`.
 * `MAXIMUM_LIFETIME`: It sets the maximum amount of time a connection can stay open in the pool before it's automatically closed. This helps keep the pool clean and prevents problems. Default: `6000`.
 * `KEEPALIVE_TIME`: It sets a test query that HikariCP will run on connections to make sure they're still working properly. Default: `360000`.
 * `CONNECTION_TEST_QUERY`: It sets how often HikariCP will check if a connection is still working properly. This helps prevent problems with connections that might become inactive. Default: `SELECT 1`
 * `SERVER_PORT`: Port to access Adempiere-Backend from outside of the container. Default: `50059`.
 * `SERVER_LOG_LEVEL`: Log Level. Default: `WARNING`.
 * `TZ`: (Time Zone) Indicates the time zone to set in the nginx-based container, the default value is `America/Caracas` (UTC -4:00).
 * `SYSTEM_LOGO_URL`: Logo of the main image of the system, shown in the login screen.

### Build docker image (for development only):
First compile output files.
``` bash
# Assembles the outputs of this project.
gradle assemble

# Or Assembles and tests this project.
gradle build
```

Build docker image (alpine)
```shell
docker build -t solopcloud/adempiere-backend:alpine-dev -f ./docker/alpine.Dockerfile .
```

Build docker image (multi-arch)
```shell
docker build -t solopcloud/adempiere-backend:dev -f ./docker/focal.Dockerfile .
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
	-e JWT_SECRET_KEY="your-json-web-token-secret-key" \
	-e TZ="America/Caracas" \
	solopcloud/adempiere-backend:alpine
```

### Run with Docker-Compose
Or easy run container using `docker-compose` with follow command:
```shell
docker compose -f build-docker/docker-compose.yaml up
```
