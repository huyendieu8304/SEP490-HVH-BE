***
# HVH - Hanoi Volunteer Hub - Backend


## Tech stack
* Build tool: maven >= 3.9.0
* Java: 21
* Framework: Spring boot 3.5.9
* DBMS: PostgreSQL v17.6
* Authentication Server: Supabase
* Storage Service: Supabase
* Cache: Redis v8.2
* Message Queue: RabbitMQ 4.1.6

## Start application
To run the application, first you have to provide following environment variables:
### Common variables

| #  | Environment Variable    | What is it used for                                                          |
|----|-------------------------|------------------------------------------------------------------------------|
| 01 | DB_URL                  | The url of the datasource                                                    |
| 02 | SP_API_SECRET_KEY       | The API secrete key of your Supabase project                                 |
| 03 | SP_BUCKET_NAME          | The name of your Supabase project bucket                                     |
| 04 | SP_JWT_ISSUER_URI       | The URI represent your Supabase Authentication Server                        |
| 05 | SP_URL                  | The URL of your Supabase project                                             | 
| 06 | RBMQ_AMQP_URL           | The URL to connect to Rabbit MQ                                              |
| 07 | RD_HOST                 | The host of Redis                                                            |
| 08 | RD_PASSWORD             | The password of Redis                                                        |
| 09 | RD_PORT                 | The port of Redis                                                            |
| 10 | FE_WEB_BASE_URL         | The base URL of front end web                                                |
| 11 | FIREBASE_SA_KEY_PATH    | The path of the file contain service account key of Firebase Cloud Messaging |
| 12 | FACEAUTHEN_API_BASE_URL | The base URL of face authentication server                                   |

### Profile dev variables

| #  |  Environment Variable | What is it used for              |
|----|-----------------------|----------------------------------|
| 01 | MAILTRAP_PASSWORD     | The password of Mailtrap sandbox |
| 02 | MAILTRAP_USERNAME     | The username of Mailtrap sandbox |

### Profile prod variables

| #  |  Environment Variable | What is it used for                                                  |
|----|-----------------------|----------------------------------------------------------------------|
| 01 | EMAIL_PASSWORD        | The password of the application's email, this is used in Spring mail |
| 02 | EMAIL_USERNAME        | The username of the application's email                              |


After having necessary environment variables, you could open terminal and run `mvn spring-boot:run`
or if you want to run the application with a specific profile `-Dspring-boot.run.profiles=prod`

## APIDocument
This application has already implemented Spring doc, Swagger API, to get file .yml of this application, please do following steps:
1. Run the application in your device
2. Open browser and access http://localhost:8080/swagger-ui.html
3. Access http://localhost:8080/v3/api-docs to see the JSON format
4. Access http://localhost:8080/v3/api-docs.yaml to download swagger yaml file
5. Save the file to your device

## Build application
`mvn clean package `
Or you want to build the project but skip testing
`mvn clean package -DskipTests `

If your computer doesn't have Maven, you can replace `mvn` with `./mvnw`, which reside in the source root.