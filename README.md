# Proyecto Banco XYZ

Proyecto academico del instituto Duoc UC para la asignatura **Desarrollo Backend III**.

## Tecnologias utilizadas

- Java
- Maven
- Spring Boot
- Spring Cloud
- Spring Cloud Config
- Spring Security
- Spring Authorization Server
- JWT
- OAuth 2.0
- Kafka
- Eureka
- Resilience4j
- Oracle Autonomus Database
- Docker
- Docker Compose

## Como funciona

**Bank-XYZ** es un proyecto backend que simula un sistema bancario, utilizando microservicios para realizar operaciones bancarias de manera segura y resiliente.

El proyecto tiene implementado:

- OAuth2.0: para la autenticacion y autorizacion mediante Spring Authorization server.
- JWT: para la seguridad en la comunicacion y acceso a los servicios.
- Scopes: para controlar los permisos de los diferentes canales.
- Spring: Cloud Config para centralizar la configuracion de los microservicios.
- Eureka Discovery Server: para el registro y descubrimiento de servicios.
- Resilience4j: para la tolerancia a fallos.
- Apache Kafka: para la comunicacion y procesamiento de mensajes asincronicos.
- Docker: para contenerizar  los diferentes componentes.
- Docker Compose: para levantar los servicios al docker.
- HTTPS: para la proteccion de datos en cada BFF.
- Oracle Autonomus Database: para la persistencia de datos.

## Estructura del proyecto

### Backend Principal

Es el backend Principal para el procesamiento de datos y su persistencia con Oracle Autonomous Database.

### BFF Web

Backend para el cliente web que entrega informacion completa de los datos bancarios.

### BFF Mobile

Backend para el cliente movil que entrega informacion reducida de los datos bancarios.

### BFF ATM

Backend para el cliente de un cajero automatico que entrega informacion necesaria y permite realizar retiros.

### Authorization Server

Servidor que entrega tokens **OAuth 2.0**, generando tokens **JWT** que permiten acceder a los recursos protegidos de cada BFF.

### Config Server

Servidor encargado de centralizar la configuracion de los diferentes microservicios mediante **Spring Cloud Config**.

### Discovery Server

Servidor que incluye Eureka para el registro y descubrimiento de servicios. 

## Implementacion de OAuth 2.0

Se implemento **OAuth 2.0** para la autenticacion y autorizacion de los diferentes BFF, utilizando tokens **JWT** y el flujo `client_credentials`.

Definiendo los tres clientes para cada uno de los BFF:

| Cliente | Scope |
|---|---|
| bff-web | WEB |
| bff-mobile | MOBILE |
| bff-atm | ATM |

Con OAuth2 Resource Server que se encuentra en cada BFF se validan los tokens verificando que contengan el scope correspondiente antes de permitir su acceso.

```text
/api/web/**     = SCOPE_WEB
/api/mobile/**  = SCOPE_MOBILE
/api/atm/**     = SCOPE_ATM
```

### Obtencion de un token

Para obtener el token, se debe realizar una peticion POST con el siguiente endpoint:

```text
POST http://localhost:9000/oauth2/token
```

El cuerpo de la peticion debe utilizar el fomato application/x-www-form-urlencoded:.

|| Key | Value |
|---|---|---|
|✓| grant_type | client_credentials |
|✓| scope | WEB |

Ademas, se introduce las credenciales del cliente mediante **HTTP Basic Authentication**.

```text
Username: bff-web
Password: web-secret
```

Al enviar te dara el token que se utilizara en el BFF correspondiente.

### Rechazo del token

Si se intenta acceder a un BFF con un token que tiene un scope que no corresponde, se rechazara dando el codigo **403 Forbidden**

## Implementacion de Resilience4j

Se implemento **Resilience4j** en los BFF para mejorar la tolerancia a fallos en la comunicacion con el backend principal.

Cada BFF utiliza mecanismos de resiliencia que permiten controlar errores cuando un servicio no este disponible.

### Circuit Breaker

Utilizado para evitar las peticiones constantes al Backend principal cuando se presentan varios errores.

### Retry

La implementacion de Retry sirve para volver a intentar una operacion cuando ocurre un error, permitiendo que la peticion pueda completarse correctamente.

### Bulkhead

Bulhead sirve para limitar la cantidad de llamadas concurrentes hacia el backend principal, y asi no se sature el servicio.

### Fallback

Cuando una operacion no se completa correctamente debido a que el backend principal no se encuentra disponible, se utiliza un fallback para entregar una respuesta controlada al cliente.

Ejemplo de fallback:

```text
    "cuentaId": 101,
    "nombreCliente": "Servicio no disponible",
    "edadCliente": 0,
    "tipoCuenta": "N/A",
    "saldoInicial": null,
    "tasaInteres": null,
    "interesGenerado": null,
    "saldoActual": null,
    "estado": "TEMPORALMENTE_NO_DISPONIBLE"
```

## Implementacion de Kafka

La implementacion de **Apache Kafka** sirve para procesar eventos a traves de mensajeria asincrona, desacoplando el procesamiento de las operaciones bancarias.

Para las operaciones de retiro se utiliza el tópico:

```text
retiro-realizado
```

Los consumidores encargados de procesar estos eventos pertenecen al siguiente Consumer Group:

```text
auditoria-bankxyz
```

El tópico retiro-realizado dispone de 3 particiones, permitiendo distribuir el procesamiento de los eventos:

```text
retiro-realizado-0
retiro-realizado-1
retiro-realizado-2
```

Los consumidores que pertenecen al mismo Consumer Group pueden distribuirse las particiones disponibles, permitiendo procesar los eventos de forma paralela.

Por ejemplo, con tres consumidores activos dentro del grupo `auditoria-bankxyz`, Kafka puede asignar una particion para cada consumidor.

```text
retiro-realizado-0 -> Consumer 1
retiro-realizado-1 -> Consumer 2
retiro-realizado-2 -> Consumer 3
```

### Flujo del evento

Cuando se realiza un retiro correctamente, el backend principal publica un evento en Kafka:

```text
BFF ATM
   |
   v
Backend
   |
   v
Kafka Producer
   |
   v
retiro-realizado
   |
   v
Consumer auditoria-bankxyz
```

## Implementacion de Docker y Docker Compose

Para este proyecto se utilizo **Docker** para tener contenerizar  el proyecto en un contenedor. 

Cada servicio tiene su propio `Dockerfile`, para la construccion de la imagen y la ejecucion de la aplicacion dentro del contenedor.

Ademas, se utilizo un **Docker Compose** para configurar, levantar y coordinar los servicios del proyecto en el contenedor.

El archivo `docker-compose.yml` levanta los siguientes servicios para el contenedor.

```text
kafka
backend
config-server
discovery-server
auth-server
bff-web
bff-mobile
bff-atm
```

Para este archivo se utiliza los siguientes comandos:

### Levantar los servicios

Para construir las imagenes e iniciar todos los servicios.

```powershell
docker compose up -d --build
```

### Comprobar el estado:

Para comprobar el estado de los contenedores.

```powershell
docker compose ps
```

### Para detener el contenedor

Para detener y eliminar los contenedores.

```powershell
docker compose down
```

## Implementacion de Oracle Autonomus Database

El backend principal esta implementado con **Oracle Autonomus Database** para la persistencia de datos bancarios mediante una base de datos Oracle.

La conexion entre el backend principal y la base de datos se realiza utilizando el **Oracle Wallet**, proporcionado por Oracle el cual tiene los archivos necesarios para establecer una conexion con la base de datos.

## Implementacion de HTTPS y keystores

Los tres BFF utilizan **HTTPS** para proteger la comunicacion entre los clientes y los servicios mediante **TLS**, permitiendo que los datos transmitidos viajen con un cifrado mas seguro.

Cada BFF dispone de su propio **Keystore**, el cual contiene el certificado y la clave privada para habilitar HTTPS.

```text
bff-web/keystore.p12
bff-mobile/keystore.p12
bff-atm/keystore.p12
```

Estas mismas **Keystores** se pueden generar con `keytool` que viene incluido con JDK.

Por ejemplo:

```text
keytool -genkeypair \
  -alias bff-local \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore keystore.p12 \
  -validity 365
```

Posteriormente, cada BFF configura Spring Boot para utilizar su Keystore:

```text
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${BFF_ATM_SSL_KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=bff-web
```

## Variables de entorno

Para iniciar el proyecto de manera local o contenerizar el proyecto en docker, es necesario configurar las siguientes variables de entorno:

```text
BANKXYZ_DB_PASSWORD=tu_password
ORACLE_WALLET_HOST_PATH=ruta_del_oracle_wallet

AUTH_WEB_CLIENT_SECRET:=secreto_oauth_web
AUTH_MOBILE_CLIENT_SECRET:=secreto_oauth_mobile
AUTH_ATM_CLIENT_SECRET=secreto_oauth_atm

BFF_WEB_SSL_KEYSTORE_PASSWORD=password_keystore_web
BFF_MOBILE_SSL_KEYSTORE_PASSWORD=password_keystore_mobile
BFF_ATM_SSL_KEYSTORE_PASSWORD=password_keystore_atm
```

Para ejecutar el proyecto mediante Docker Compose, se debe crear un archivo `.env` en la raíz del proyecto

Por motivos de seguridad, el archivo `.env` no debe publicarse en el repositorio, ya que puede contener contraseñas, secretos OAuth y otras credenciales sensibles.

Por esta razón, debe estar incluido en el archivo `.gitignore`.

## Ejemplos

### Ejemplo OAuth2

Para solicitar un token JWT:

```powershell
curl.exe -u "bankxyz-web:CLIENT_SECRET" `
  -X POST `
  -H "Content-Type: application/x-www-form-urlencoded" `
  -d "grant_type=client_credentials&scope=WEB" `
  http://localhost:9000/oauth2/token
```

Para utilizar el token:

```powershell
curl.exe -sk `
  -H "Authorization: Bearer TOKEN" `
  https://localhost:8081/api/web/cuentas/101
```

### Ejemplo en Postman

Consultar saldo desde ATM:

```text
GET https://localhost:8083/api/atm/cuentas/101/saldo
```

Se debe enviar el token correspondiente al scope ATM:

```text
Authorization: Bearer TOKEN
```

Resultado esperado:

```json
{
    "cuentaId": 101,
    "saldoDisponible": 7080,
    "estado": "VALIDA"
}
```

Ejemplo con atm con retiro:

```text
POST https://localhost:8083/api/atm/cuentas/101/retiro
```

Se debe enviar el token correspondiente al scope ATM:

```text
Authorization: Bearer TOKEN
```

Body de la peticion:

```json
{
    "monto": 100
}
```

Resultado esperado:

```json
{
    "cuentaId": 101,
    "montoRetirado": 100,
    "saldoDisponible": 6980,
    "mensaje": "Retiro realizado correctamente"
}
```

## Como compilar en Maven

El proyecto utiliza **Maven** para la gestion de dependencias y compilacion de los diferentes servicios.

Para compilar desde la rais se utiliza el comando:

```powershell
.\mvnw.cmd clean package -DskipTests
```

Para compilar individualmente cada uno de los servicios:

```powershell
.\mvnw.cmd -f .\backend\pom.xml clean package -DskipTests
.\mvnw.cmd -f .\discovery-server\pom.xml clean package -DskipTests
.\mvnw.cmd -f .\config-server\pom.xml clean package -DskipTests
.\mvnw.cmd -f .\auth-server\pom.xml clean package -DskipTests
.\mvnw.cmd -f .\bff-web\pom.xml clean package -DskipTests
.\mvnw.cmd -f .\bff-mobile\pom.xml clean package -DskipTests
.\mvnw.cmd -f .\bff-atm\pom.xml clean package -DskipTests
```

## Conclusion

En conclusion el proyecto **Bank-XYZ** permite implementar una arquitectura backend basada en microservicios, integrando diferentes tecnologias para la seguridad, resiliencia, comunicacion y persistencia de datos.

Mediante **OAuth2.0** junto con **JWT** y **HTTPS** se protegio el acceso y comunicacion de los diferentes BFF, teniendo un proyecto seguro para los clientes. Con **Resilience4j** se incorporaron mecanismos de tolerancia a fallos y sea mas controlado, mientras que **Apache Kafka** permitio implementar comunicacion asincrona para el procesamiento de eventos.

Ademas, **Spring Cloud Config** y **Eureka** permiten centralizar la configuracion y facilitar el descubrimiento de servicios. **Oracle Autonomus Database** fue utilizado para la persistencia de los datos bancarios y mediante **Docker** y **Docker Compose**, los diferentes componentes de arquitectura pueden ejecutarse de forma conjunta y aislada.

El proyecto permitio aplicar conceptos de desarrollo backend relacionados con microservicios, seguridar, resiliencia, mensajeria asincrona, persistencia y contenerizacion, construyendo una solucion bancaria modular y preparada para manejar diferentes canales como Web, Mobile y ATM.