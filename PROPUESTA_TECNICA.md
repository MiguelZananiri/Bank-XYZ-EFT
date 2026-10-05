# Propuesta Tecnica

## 1. Introduccion

La presente propuesta plantea una modernizacion al proyecto backend de **Bank XYZ**, un sistema bancario basada en una arquitectura de microservicios.

La solucion busca separar las responsabilidades del sistema, proporcionando servicios especializados para los diferentes clientes como Web, Mobile y ATM, mejorando la seguridad de las comunicaciones e incorporando mecanismos de resiliencia y procesamiento asincrono.

Se propone utilizar las siguientes tecnologias para lograr el ecosistema que se desarrolla. Spring, junto con OAuth 2.0, JWT, Apache Kafka, Resilience4j, Oracle Autonomus Database, Docker y Docker Compose.

## 2. Objetivo general

El objetivo es diseñar e implementar una arquitectura backend distribuida para Bank XYZ que permita realizar operaciones bancarias de manera segura, resiliente y desacoplada, proporcionando servicios adaptados a diferentes tipos de clientes.

## 3. Objetivos especificos

- Implementar BFF independientes para los canales Web, Mobile y ATM.
- Centralizar la configuracion de los servicios mediante Spring Cloud Config.
- Implementar registro y descubrimiento de servicios mediante Eureka.
- Proteger los recursos mediante OAuth 2.0 y tokens JWT.
- Restringir el acceso a los diferentes BFF mediante scopes.
- Proteger las comunicaciones mediante HTTPS.
- Implementar tolerancia a fallos mediante Resilience4j.
- Incorporar mensajeria asincrona mediante Apache Kafka.
- Utilizar Oracle Autonomus Database para la persistencia de datos.
- Contenerizar los componentes mediante Docker y coordinarlos mediante Docker Compose.

## 4. Problematica

Para Bank XYZ se requiere una arquitectura que permita atender diferentes canales de acceso sin exponer el backend principal.

Los canales poseen diferentes necesidades. Por ejemplo, el cliente web puede requerir informacion bancaria mas completa, mientras que un ATM necesita unicamente informacion relacionada con operaciones especificas, como la consulta de saldo y la realizacion de retiros.

Ademas, una arquitectura distribuida debe considerar problemas como la indisponibilidad temporal de servicios, seguridad de los endpoints, comunicacion entre componentes y procesamiento de operaciones que no necesitan ejecutarse de manera sincrona.

## 5. Solucion propuesta

Se propone implementar una arquitectura basada en diferentes servicios independientes.

```text
                       +----------------------+
                       | Authorization Server |
                       |   OAuth 2.0 / JWT    |
                       +----------+-----------+
                                  |
                                  |
               +------------------+------------------+
               |                  |                  |
               v                  v                  v
        +-------------+    +-------------+    +-------------+
        |   BFF Web   |    | BFF Mobile  |    |   BFF ATM   |
        +------+------+    +------+------+    +------+------+
               |                  |                  |
               +------------------+------------------+
                                  |
                                  v
                        +-------------------+
                        | Backend Principal |
                        +---------+---------+
                                 |
                    +------------+-------------+
                    |                          |
                    v                          v
        +----------------------+      +------------------+
        | Oracle Autonomous DB |      |   Apache Kafka   |
        |                      |      |     Eventos      |
        +----------------------+      +---------+--------+
```

Los BFF actuaran como intermediarios entre los clientes y el Backend Principal, permitiendo entregar informacion especifica dependiendo del canal utilizado.

## 6. Seguridad

La solucion utilizara Spring Authorization Server para implementar OAuth 2.0.

utilizando el flujo `client_credentials`, donde cada BFF dispondra de sus propias credenciales y scope:

| Cliente | Scope |
|---|---|
| bff-web | WEB |
| bff-mobile | MOBILE |
| bff-atm | ATM |

El Authorization Server generara tokens JWT y cada BFF funcionara como OAuth 2.0 Resource Server validando el token y los permisos antes de permitir el acceso a sus endpoints.

Adicionalmente, los BFF utilizaran HTTPS mediante certificados almacenados en Keystores.

## 7. Resiliencia

Para evitar problemas del Backend Principal afecten a los clientes, los BFF implementaran mecanismos de tolerancia a fallos mediante Resilience4j.

Se utilizaran:

- **Circuit Breaker**: interrupe temporalmente las llamadas cuando se detecta una cantidad determinada de errores.
- **Retry**: permite reintentar operaciones ante fallos temporales.
- **Bulkhead**: limita las llamadas concurrentes para evitar la saturacion de los servicios.
- **Fallback**: Proporciona respuestas controladas cuando una operacion no se completa correctamente.

Esto reducira la propagacion de errores.

## 8. Mensajeria asincrona

Se utilizara Apache Kafka para el procesamiento asincrono de eventos.

Cuando el Backend Principal complete una operacion de retiro, publicara un evento en el topico: `retiro_realizado`

El evento sera procesado posteriormente por consumidores pertenecientes al Consumer Group: `auditoria-bankxyz`

El topico contara con tres particiones para permitir distribuir y paralelizar el procesamiento de los eventos.

Esta estrategia permite desacoplar la operacion bancaria principal de procesos secundarios que no necesitan completarse.

## 9. Persistencia

El Backend Principal utilizara Oracle Autonomous Database para almacenar los datos bancarios.

La conexion se realizara mediante Oracle Wallet, cuya ubicacion y credenciales seran proporcionadas mediante variables de entorno.

Esto permite mantener las credenciales sensibles separadas del codigo y facilita la configuracion del sistema en diferentes entornos.

## 10. Contenerizacion y despliegue

Los diferentes componentes seran contenerizados utilizando Docker.

Cada servicio contara con su propio `Dockerfile`, mientras que Docker Compose sera utilizado para coordinar la ejecucion de la arquitectura.

entre los servicios se encuentran:

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

Las credenciales y parametros sensibles seran proporcionados mediante variables de entorno y un archivo `.env`, el cual no sera almacenado en el repositorio.

## 11. Beneficios esperados

La arquitectura propuesta proporciona los siguientes beneficios:

- Separacion de responsabilidades entre los componentes.
- Adaptacion de las respuestas segun el canal utilizado
- Proteccion de endpoints mediante OAuth 2.0 y JWT
- Comunicacion cifrada mediante HTTPS.
- Mayor tolerancia ante fallos temporales.
- Procesamiento asincrono y desacoplado mediante Kafka.
- Persistencia externa mediante Oracle Autonomous Database.
- Configuracion centralizada de los microservicios.
- Descubrimiento dinamico de servicios.
- Ejecucion reproducible mediante Docker Compose.

## 12. Conclusion

La propuesta tecnica busca modernizar el backend de Bank XYZ mediante una arquitectura distribuida que combine seguridad, resiliencia, mensajeria asincrona y contenerizacion.

La separacion mediante BFF permite atender las necesidades especificas de los canales Web, Mobile y ATM, mientras que OAuth 2.0, JWT y HTTPS proporcionan mecanismos de proteccion para el acceso a los servicios.

Finalmente, la utilizacion de Resilience4j, Apache Kafka, Oracle Autonomous Database y Docker permite construir una solucion modular que demuestra la aplicacion practica de diferentes concesptos asociados al desarrollo de sistemas backend modernos.