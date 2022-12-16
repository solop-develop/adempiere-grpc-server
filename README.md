# ADempiere gRPC Server
ADempiere gRPC Server example of integration

This is a project for publish Application Dictionary of ADempiere over gRPC service

## Recreate proto stub class
For recreate stub class you must have follow:
- [protobuf](https://github.com/protocolbuffers/protobuf/releases)
- [gradle](https://gradle.org/install)
- Maven
- Also you can see it: [gRPC-Java](https://grpc.io/docs/quickstart/java.html)
- [Complete Java Documentation](https://grpc.io/docs/tutorials/basic/java.html)
After installed it just go to source code folder an run it:

## Include ADempiere dependencies

In the build.gradle you need to include in the next sections the references to jars placed in lib, packages and zkpackages in your ADEMPIERE _HOME

* In the repository section as flatDir
Example:
```
flatDir {
  dirs $ad_path + '/lib'
}
```
* In de dependencies section, Example
```
implementation fileTree(dir: $ad_path + '/lib', include: '*.jar')
```

## Init project
``` bash
gradle wrapper
```

## Clean
``` bash
./gradlew clean
```

## Generate Stups
``` bash
./gradlew installDist
```

## Runing Server
The server can be running as java class. See it: **org.spin.grpc.util.DictionaryServer**
Don't forgive that for run server you need set yaml file line is /resources folder.

- Use latest [release](https://github.com/erpcya/adempiere-gRPC-Server/releases)
- Unzip binary
- go to bin folder
- run it

```bash
./adempiere-all-in-one-server "/tmp/dictionary_connection.yaml"
```


### For all enviroment you should run the follow images:
- ADempiere Backend: https://hub.docker.com/r/solopcloud/adempiere-backend
```shell
docker pull solop/adempiere-backend:experimental
```
- ADempiere Proxy: https://hub.docker.com/r/solopcloud/adempiere-proxy
```shell
docker pull solop/adempiere-proxy:experimental
```
- Frontend-Core: https://hub.docker.com/r/solopcloud/adempiere-vue
```shell
docker pull solop/adempiere-vue:experimental
```

## Run docker container:

### Minimal Docker Requirements

To use this Docker image you must have your Docker engine release number greater
than or equal to 3.0.

Build docker image (for development only):
```shell
docker build -t solop/adempiere-backend:dev -f ./build-docker/develop.Dockerfile .
```

Download docker image:
```shell
docker pull solop/adempiere-backend:experimental
```

Run container container:
```shell
docker run -it -d \
	--name adempiere-backend \
	-p 50059:50059 \
	-e DB_HOST="your-db-host" \
	-e DB_PORT="5432" \
	-e DB_NAME=\"1204\" \
	-e DB_PASSWORD="adempiere" \
	-e TZ="America/Caracas" \
	solopcloud/adempiere-backend:experimental
```

Or easy run container using `docker-compose` with follow command:
```shell
docker-compose up
```

### Environment variables
 * `DB_TYPE`: Database Type (Supported Oracle, PostgreSQL and MariaDB). Default `PostgreSQL`
 * `DB_HOST`: Hostname for data base server. Default: `localhost`
 * `DB_PORT`: Port used by data base server. Default: `5432`
 * `DB_NAME`: Database name that Adempiere-Backend will use to connect with the database. Default: `adempiere`
 * `DB_USER`: Database user that Adempiere-Backend will use to connect with the database. Default: `adempiere`
 * `DB_PASSWORD`: Database password that Adempiere-Backend will use to connect with the database. Default: `adempiere`
 * `ADEMPIERE_APPS_TYPE`: Application Type for Database Management Connection. Default: `wildfly`
 * `SERVER_PORT`: Port to access Adempiere-Backend from outside of the container. Default: `50059`
 * `SERVICES_ENABLED`: Services enabled. Default: `access; business; business_partner; core; dashboarding; dictionary; enrollment; file_management; general_ledger; in_out; invoice; log; material_management; order; payment; payment_print_export; payroll_action_notice; pos; product; store; time_control; ui; workflow;`
 * `SERVER_LOG_LEVEL`: Log Level. Default: `WARNING`
 * `TZ`: (Time Zone) Indicates the time zone to set in the nginx-based container, the default value is `America/Caracas` (UTC -4:00).
