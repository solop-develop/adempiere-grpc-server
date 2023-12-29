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

Español | [Inglés](./README.md)

Este es un proyecto para publicar ADempiere, sobre servicio gRPC, desacoplando en módulos como Acceso, Diccionario de Aplicaciones, Negocios, Punto de Ventas, Flujos de Trabajos, Tableros, Manejo de Archivos, Logs, Solicitudes (como Gestión de Incidencias), Personalización de Usuarios, etc.

## Recrear clase proto stub
Para recrear la clase stub debes tener lo siguiente:
- [protobuf](https://github.com/protocolbuffers/protobuf/releases)
- [gradle](https://gradle.org/install)
- [Maven](https://search.maven.org/)
- También se puede ver: [gRPC-Java](https://grpc.io/docs/quickstart/java.html)
- [Complete Java Documentation](https://grpc.io/docs/tutorials/basic/java.html)

Después de instalarlo sólo tienes que ir a la carpeta de código fuente y ejecutarlo:

## Ejecutando como desarrollo
### Limpiar
``` bash
gradle clean
```

### Ejecutar servidor con archivo de conexión por defecto
``` bash
gradle run
```

### Ejecutar servidor con archivo de conexión personalizado
``` bash
gradle run --args="'resources/standalone.yaml'"
```

## Servidor en ejecución
El servidor se puede ejecutar como clase java. Ver: **org.spin.server.AllInOneServices**
No olvide que para ejecutar el servidor necesita establecer la línea del archivo `yaml` en la carpeta `/resources`.

- Utilizar la última [liberación](https://github.com/solop-develop/backend/releases)
- Descomprimir binario
- Ir a la carpeta bin
- Ejecutar

```bash
./adempiere-all-in-one-server "./resources/standalone.yaml"
```


### Para todos los entornos debe ejecutar las siguientes imágenes:

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

## Ejecutar contenedor docker:

### Requisitos mínimos de Docker
Para utilizar esta imagen Docker debe tener su motor Docker versión mayor o igual a 3.0.

### Variables de entorno
 * `DB_TYPE`: Tipo de base de datos (compatible con `Oracle` y `PostgreSQL`). Por defecto `PostgreSQL`.
 * `DB_HOST`: Nombre de host para el servidor de la base de datos. Predeterminado: `localhost`.
 * `DB_PORT`: Puerto utilizado por el servidor de base de datos. Predeterminado: `5432`.
 * `DB_NAME`: Nombre de la base de datos que Adempiere-Backend utilizará para conectarse con la base de datos. Por defecto: `adempiere`.
 * `DB_USER`: Usuario de base de datos que Adempiere-Backend utilizará para conectarse con la base de datos. Por defecto: `adempiere`.
 * `DB_PASSWORD`: Contraseña de la base de datos que Adempiere-Backend utilizará para conectarse con la base de datos. Por defecto: `adempiere`.
 * `JWT_SECRET_KEY`: Clave secreta, para el hash de encriptado del JSON Web Token, el valor por defecto es `2C51599F5B1248F945B93E05EFC43B3A15D8EB0707C0F02FD97028786C40976F` esta debe ser cambiada obligatoriamente por seguridad.
 * `JWT_EXPIRATION_TIME`: Tiempo de expiración en milisegundos, vida util del JSON Web Token desde que se genera, el valor por defecto es `86400000` milisegundos, es decir 24 horas.
 * `ADEMPIERE_APPS_TYPE`: Tipo de aplicación para la conexión de gestión de bases de datos. Por defecto: `wildfly`.
 * `SERVER_PORT`: Puerto para acceder a Adempiere-Backend desde fuera del contenedor. Por defecto: `50059`.
 * `SERVICES_ENABLED`: Servicios habilitados. Por defecto: `bank_statement_match; business; business_partner; core; dashboarding; dictionary; enrollment; express_movement; express_receipt; express_shipment; file_management; general_ledger; import_file_loader; in_out; invoice; issue_management; location_address; log; match_po_receipt_invoice; material_management; notice_management; order; payment; payment_allocation; payment_print_export; payroll_action_notice; pos; product; record_management; report_management; security; store; time_control; time_record; trial_balance_drillable; user_customization; user_interface; workflow;`.
 * `IS_ENABLED_ALL_SERVICES`: Si esta en verdadero, omite el valor de `SERVICES_ENABLED` y arranca todos los servicios, el valor por defecto es `true`.
 * `SERVER_LOG_LEVEL`: Nivel de Bitácora. Por defecto: `WARNING`.
 * `TZ`: (Time Zone) Indica la zona horaria a establecer en el contenedor basado en nginx, el valor por defecto es `America/Caracas` (UTC -4:00).
 * `SYSTEM_LOGO_URL`: Logo de la imagen principal del sistema, mostrada en la pantalla del login.

### Construir imagen docker (sólo para desarrollo):
Primero compile los archivos de salida.
```shell
# Monta los resultados de este proyecto.
gradle assemble

# O Monta y prueba este proyecto.
gradle build
```

Construir imagen docker
```shell
docker build -t solopcloud/adempiere-backend:dev -f ./build-docker/development.Dockerfile .
```

### Descargar imagen docker:
```shell
docker pull solopcloud/adempiere-backend:alpine
```

### Ejecutar contenedor contenedor:
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

### Ejecutar con Docker-Compose
O ejecute fácilmente el contenedor usando `docker-compose` con el siguiente comando:
```shell
docker compose -f build-docker/docker-compose.yaml up
```
