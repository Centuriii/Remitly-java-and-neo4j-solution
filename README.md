# **Remitly-java-and-neo4j-solution**

A service using Java and Neo4j. It exposes 4 simple RESTful api endpoints which are using Neo4j database.

## **How to run**

The app is meant to be run as a docker container so you need to have Docker installed in order to use it.
After downloading or cloning the repository simply run the following commands in the terminal:

~~~ bash
cd path/to/your/folder
docker-compose up --build
~~~

After the command is executed docker will start to build up proper containers. After the process is finished and database is initialized you can start using the service.

## **Usage**

In total there are 4 RESTful API endpoints exposed.

### Endpoint 1
* GET: /v1/swift-codes/{swift-code}
    * Returns details for a single SWIFT code
    * If the code is reffering to the headquarters it will also return all the branches for that headquarters
### Endpoint 2
* GET: /v1/swift-codes/country/{countryISO2code}:
    * Returns all the SWIFT codes with details for a specific country
### Endpoint 3
* POST: /v1/swift-codes:
    * Adds new SWIFT code entry into the database
    * Requires the following structure:
        {
        "address": string,
        "bankName": string,
        "countryISO2": string,
        "countryName": string,
        “isHeadquarter”: bool,
        "swiftCode": string
        }
### Endpoint 4
* DELETE: /v1/swift-codes/{swift-code}
    * Deletes a SWIFT code from the database if it's there


