# CONTEXT.md — adempiere-grpc-server

## Identidad
- **Nombre:** adempiere-grpc-server
- **Propósito:** Expone el ERP ADempiere como una API gRPC/REST moderna, desacoplando la lógica de negocio en servicios especializados para clientes web, móviles y sistemas de terceros.
- **Tipo:** API (gRPC + REST via Envoy proxy)

---

## Stack
- **Lenguaje/Runtime:** Java 17 (source compat 11)
- **Framework principal:** gRPC (grpc-netty 1.63.0) + Protobuf 3.25.3
- **Base de datos:** PostgreSQL (primario) / Oracle (secundario) — acceso vía ADempiere ORM (PO model) y `DB` utility
- **Cola/Mensajería:** No aplica
- **Testing:** Gradle `test` task; JUnit disponible. No se detectaron fixtures de prueba relevantes. Comando: `./gradlew test`

---

## Modelo de dominio

- `Session` — Token de sesión autenticada del usuario
  - contiene → `UserInfo` (1:1), `Role` (1:1), `Organization` (1:1), `Warehouse` (1:1)
- `UserInfo` — Perfil del usuario (id, name, image, timeout)
- `Role` — Rol con permisos (can_report, can_export, personal_lock, access flags)
- `Organization` — Unidad organizacional (id, name, tax_id, duns)
- `Warehouse` — Almacén físico (id, name, description)
- `Entity` — Wrapper genérico para cualquier registro ERP (id, uuid, table_name, values como Struct)
  - base de → `BusinessPartner`, `Product`, `Order`, `Invoice`, `InOut` (polimorfismo via table_name)
- `BusinessPartner` — Cliente/proveedor/empleado (value, tax_id, duns)
- `Product` — Artículo de catálogo (value, name, uom, category, price)
- `Order` — Documento de compra/venta con líneas y totales
  - tiene → `BusinessPartner` (N:1), `Product` por línea (N:M)
- `Invoice` — Documento de facturación
  - relacionado con → `Order` (N:1), `InOut` (N:M) via 3-way match
- `InOut` — Movimiento de inventario (recepción/despacho)
- `WorkflowDefinition` — Definición de proceso de negocio
  - tiene → `WorkflowNode` (1:N) → `WorkflowTransition` (1:N)
- `WorkflowActivity` — Tarea pendiente de usuario en un workflow
- `Window` / `Tab` / `Field` — Metadatos UI del diccionario de datos
  - `Window` tiene → `Tab` (1:N) → `Field` (1:N)
- `Process` — Operación batch ejecutable con parámetros
- `Browser` — Smart browser/consulta configurable con columnas de selección
- `ProcessLog` — Log de ejecución de proceso (is_error, summary, logs)

---

## Dependencias internas

- `solop-develop/adempiere-solop` → Core ADempiere fork: ORM (PO model), `DB`, `Env`, `CLogger`, `CCache`, `MUser`, `MRole`, `MWindow`, `MTable`, etc.
- `solop-develop/adempiere-base` → Librerías base compartidas (1.5.2)
- `solop-develop/display-definition` → Definición de campos personalizados en UI (1.0.0)
- `solop-develop/time-control` → Módulo de control de tiempo (1.4.22)
- `solop-develop/performance-analysis` → Análisis de rendimiento (1.2.6)
- `solop-develop/electronic-invoicing` → Facturación electrónica (1.5.2)
- `solop-develop/payment-processor` → Procesador de pagos (1.4.9)
- `solop-develop/warehouse-management-light` → WMS ligero (1.4.20)
- `solop-develop/payroll-light` → Nómina ligera (1.5.2)
- `solop-develop/webhook-support` → Webhooks (1.5.2)
- `solop-develop/external-store` → Tienda externa / eCommerce (1.5.2)

---

## Interfaz pública

El servidor expone **48 servicios gRPC** registrados en un único proceso. A continuación los grupos principales:

### Seguridad y Sesión (`security.Security`)
- `RunLogin` — Autenticación con credenciales; retorna `Session`
- `RunLoginOpenID` — Autenticación vía OpenID
- `RunLogout` — Cierra sesión
- `GetUserInfo` — Datos del usuario activo
- `GetMenu` — Árbol de menú según rol
- `RunChangeRole` — Cambia rol/organización dentro de la sesión
- `SetSessionAttribute` — Modifica atributos de sesión
- `ListRoles / ListOrganizations / ListWarehouses / ListServices` — Catálogos de acceso

### Datos Genéricos (`business.BusinessData`)
- `GetEntity` — Lee un registro por id/uuid/tabla
- `CreateEntity / UpdateEntity / DeleteEntity` — CRUD genérico
- `DeleteEntitiesBatch` — Borrado masivo
- `ListEntities` — Consulta con filtros (WHERE conditions)
- `RunBusinessProcess` — Ejecuta un proceso de negocio

### Diccionario UI (`dictionary.Dictionary`)
- `GetWindow / GetTab / GetField` — Metadatos de ventanas
- `GetProcess / ListProcesses` — Definiciones de procesos
- `GetBrowser` — Smart browsers
- `GetForm` — Formularios customizados
- `ListIdentifiersColumns / ListSelectionColumns / ListSearchFields` — Columnas de referencia

### Funcionalidad Base (`core_functionality.CoreFunctionality`)
- `GetSystemInfo` — Info del sistema (no requiere auth)
- `ListLanguages` — Idiomas disponibles (no requiere auth)
- `GetCurrency / GetCountry / GetConversionRate` — Datos maestros
- `GetPriceList / GetUnitOfMeasure / ListProductConversion`

### Workflow (`workflow.Workflow`)
- `GetWorkflow / ListWorkflows` — Definiciones
- `ListDocumentActions / ListDocumentStatuses` — Acciones disponibles por documento
- `RunDocumentAction` — Ejecuta acción (Complete, Void, Post, etc.)
- `ListWorkflowActivities / Process / Forward` — Gestión de actividades

### Punto de Venta (`point_of_sales.PointOfSales`)
- Crear/actualizar/eliminar órdenes POS
- Gestión de pagos, devoluciones, retiros de caja
- Consulta de precios y productos

### Formularios Especializados
- `BankStatementMatch` — Conciliación bancaria
- `PaymentAllocation` — Asignación de pagos
- `MatchPOReceiptInvoice` — Matching 3 vías (OC/Recepción/Factura)
- `TrialBalanceDrillable` — Balance de saldos con drill-down
- `OutBoundOrder` — Órdenes de salida (WMS)
- `ImportFileLoader` — Carga masiva de datos
- `IssueManagement / TaskManagement` — Gestión de incidencias y tareas
- `ExpressMovement / ExpressReceipt / ExpressShipment` — Operaciones express de inventario

### Otros Servicios
- `MaterialManagement` — Atributos y almacenamiento de productos
- `ReportManagement` — Ejecución y exportación de reportes
- `LogsInfo` — Logs de procesos, entidades y chat
- `FileManagement` — Subida/descarga de archivos
- `Dashboarding` — Widgets de dashboard
- `TimeControl / TimeRecord` — Control de tiempo y registros
- `NoticeManagement / SendNotifications` — Notificaciones
- `PreferenceManagement / UserCustomization` — Preferencias de usuario
- `RecordManagement` — Historial de registros
- `UpdateManagement` — Centro de actualizaciones
- `Enrollment` — Registro de nuevos usuarios
- `WebStore` — Integración eCommerce
- `LocationAddress` — Gestión de direcciones
- `PaymentPrintExport` — Impresión de documentos de pago

---

## Flujos principales

**Autenticación y obtención de sesión**
1. Cliente llama `Security/RunLogin` con usuario, contraseña, idioma, organización
2. El servidor valida credenciales contra ADempiere (`MUser`)
3. Se genera token JWT firmado con `JWT_SECRET_KEY`
4. Retorna `Session` con token, info de usuario, rol, org, almacén y moneda
5. Todas las llamadas subsecuentes incluyen el token en el header `Authorization`; `AuthorizationServerInterceptor` lo valida

**CRUD genérico sobre entidades ERP**
1. Cliente llama `BusinessData/GetEntity` con `table_name` + `id` o `uuid`
2. El servidor resuelve la tabla en ADempiere ORM
3. Carga el PO (Persistent Object) y serializa sus columnas como `google.protobuf.Struct`
4. Retorna `Entity` con todos los campos del registro
5. Para escritura: `CreateEntity/UpdateEntity` deserializa el Struct y persiste vía `save()`

**Ejecución de acción de documento**
1. Cliente llama `Workflow/RunDocumentAction` con `table_name`, `record_id`, `document_action`
2. El servidor localiza el workflow asociado al documento
3. Valida que la transición sea válida para el estado actual
4. Ejecuta la acción de negocio (e.g., Complete → genera asientos contables)
5. Retorna `ProcessLog` con resultado (éxito/error, summary, logs detallados)

**Consulta de menú y navegación UI**
1. Tras login, cliente llama `Security/GetMenu` con el token de sesión
2. El servidor construye el árbol de menú filtrado por rol y organización
3. Retorna la jerarquía de nodos (ventanas, procesos, reportes, browsers, forms)
4. Cliente llama `Dictionary/GetWindow` para obtener los metadatos completos de una ventana (tabs, fields, lógicas de display/mandatory/readOnly)

---

## Estructura relevante

```
src/main/java/org/spin/
├── server/AllInOneServices.java     # Entry point: registra ~45 servicios gRPC
├── grpc/service/                    # Implementaciones de servicios
│   ├── Security.java                # Auth, sesión, menú
│   ├── BusinessData.java            # CRUD genérico
│   ├── PointOfSalesForm.java        # Servicio más grande (167KB)
│   ├── core_functionality/          # Patrón: Service + ServiceLogic + Convert
│   ├── field/                       # Lookups especializados por entidad
│   ├── form/                        # Formularios complejos
│   └── ui/                          # Lógica de renderizado UI
├── base/                            # Utilidades base (util/, db/, workflow/)
src/main/proto/                      # 48 archivos .proto (contratos gRPC)
resources/
├── standalone.yaml                  # Config DB + puerto servidor
└── envoy.yaml                       # Config proxy gRPC-Web
docker/
├── alpine.Dockerfile / ubuntu.Dockerfile
├── env.yaml                         # Template variables de entorno Docker
└── start.sh                         # Script arranque contenedor
```

---

## Patrones y convenciones

- **Arquitectónico:** Monolito modular. Un único proceso JVM con ~45 servicios gRPC registrados. No hay microservicios separados; la modularidad es por clases de servicio.
- **Patrón de servicio en 3 capas** (visible en `core_functionality/`):
  - `XxxService.java` — Stub gRPC, manejo de errores, delega a Logic
  - `XxxServiceLogic.java` — Lógica de negocio pura
  - `XxxConvert.java` — Conversión PO ↔ Protobuf
- **Manejo de errores:** Cada método RPC tiene try/catch; las excepciones se convierten en `Status.INTERNAL.withDescription(e.getMessage()).asException()` pasado a `observer.onError()`.
- **Autenticación:** `AuthorizationServerInterceptor` intercepta todas las llamadas. Endpoints sin auth declarados explícitamente en `AllInOneServices` (whitelist).
- **ORM:** ADempiere PO model. Clases como `MUser`, `MOrder`, `MInvoice` extienden `PO`. Queries con `new Query(ctx, table, whereClause, trxName)`.
- **Contexto:** `Env.getCtx()` provee el contexto de ADempiere (Properties) que contiene sesión, idioma, moneda y org activos.
- **Caché:** `CCache<K, V>` para diccionario y metadatos. Evitar accesos repetidos a BD para datos estáticos.
- **Naming de protos:** snake_case para archivos y campos; servicios en PascalCase. Archivos de formularios: `form.nombre_form.proto`. Lookups: `field.entidad.proto`.
- **Datos dinámicos:** Se usa `google.protobuf.Struct` para serializar registros ERP con campos variables, evitando generar un proto por cada tabla.

---

## Configuración y despliegue

- **Despliegue:** Docker (Alpine/Ubuntu). Imagen: `solopcloud/adempiere-backend`. También ejecutable como JAR standalone.
- **Comando de arranque:**
  ```bash
  java -DPropertyFile=Adempiere.properties \
       -Dorg.adempiere.server.embedded=true \
       -jar adempiere-grpc-server.jar resources/standalone.yaml
  ```
- **Variables de entorno críticas:**
  - `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` (o `DB_PASSWORD_FILE`)
  - `DB_TYPE` — `PostgreSQL` | `Oracle`
  - `SERVER_PORT` — Puerto gRPC (default: 50059)
  - `JWT_SECRET_KEY` — Clave firma JWT (256-bit hex recomendado)
  - `JWT_EXPIRATION_TIME` — TTL del token en ms (default: 86400000)
  - `JAVA_OPTIONS` — JVM args (default: `-Xms64M -Xmx1512M`)
  - `MAXIMUM_POOL_SIZE`, `MINIMUM_IDLE`, `CONNECTION_TIMEOUT` — Pool HikariCP
  - `TZ` — Timezone (default: `America/Caracas`)
- **TLS:** Opcional; se configura con rutas a cert chain y private key.
- **gRPC-Web:** Envoy proxy en `resources/envoy.yaml` convierte HTTP/1.1 → gRPC para clientes browser.
- **Build:** `./gradlew build` genera JAR + ZIP/TAR de distribución. Descriptor gRPC: `adempiere-grpc-server.dsc`.
- **CI:** GitHub Actions (`.github/`); publica Docker images y Maven packages a GitHub Packages.

---

## Decisiones importantes y deuda técnica

**Decisiones:**
1. **Monolito gRPC en lugar de microservicios:** Simplifica el despliegue y mantiene transacciones ACID del ERP sin coordinación distribuida. El costo es un JAR grande (~150+ MB con dependencias).
2. **`google.protobuf.Struct` para entidades genéricas:** Permite exponer cualquier tabla ADempiere sin generar un proto por entidad. Sacrifica type-safety a nivel de contrato a favor de flexibilidad.
3. **ADempiere embebido (`-Dorg.adempiere.server.embedded=true`):** El servidor gRPC no es un cliente del ERP; lo *contiene*. Esto da acceso directo al ORM pero acopla fuertemente al core de ADempiere.
4. **Módulos opcionales como dependencias Maven:** Payroll, WMS, Electronic Invoicing, etc. se inyectan como JARs externos desde GitHub Packages. Permite builds lite vs. full sin condicionales en el código.
5. **Whitelist de endpoints sin auth en código:** Los endpoints públicos (`GetSystemInfo`, `RunLogin`, etc.) se declaran como constantes en `AllInOneServices.java`. Cambiar visibilidad requiere modificar y redesplegar el servidor.

**Zonas frágiles / deuda técnica:**
- **`PointOfSalesForm.java` (167KB):** Servicio monolítico que concentra toda la lógica POS. Difícil de mantener; no refactorizar sin entender el dominio POS completo. Candidato a la refactorización en 3 capas como `core_functionality/`.
- **Contexto ADempiere (`Env.getCtx()`):** El contexto es un `Properties` global con estado por thread. Mutarlo incorrectamente puede corromper la sesión de otros threads concurrentes.
- **Transacciones:** El manejo de `trxName` (nombre de transacción ADempiere) es manual y propenso a leaks si no se hace rollback/close en paths de error. Revisar siempre bloques finally.
- **`DB_PASSWORD_FILE` vs `DB_PASSWORD`:** El override por archivo secreto existe pero no todos los paths de código lo respetan uniformemente. Verificar al configurar en entornos Kubernetes con secrets.
- **Descriptor `.dsc`:** `adempiere-grpc-server.dsc` se genera en build y debe estar en sync con los protos. Un proto añadido sin rebuilding causa que el descriptor quede desactualizado, rompiendo el gRPC reflection.
