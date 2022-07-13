# Websocket container client

![Diagram](/resources/diagram.PNG)

## Introduction
The client is comprised of three parts, as shown on the image above:
- EC2-Instance with docker daemon 
- API Gateway + Lambda function

To establish a websocket connection with a certain platform it is needed to call the endpoint exposed by API Gateway providing in the headers all three parameters required for the connection (internalID, interWorkingInterface, platformID) and valid ASAPA credentials.
For each websocket connection a container is created in the EC2-instance. For each message recieved via websocket a post call is sent to API II to persist the message on the data lake. 
## EC2-Instance with docker daemon 
A docker container is created for each websocket connection requested. The image to create such container is defined in the Dockerfile. Appart from the Dockerfile, the following files are required to create the container:
- entry.sh --> set of instructions to create the crond process and the java client for the websocket
- cron_files --> all the files needed to run the crond job.
- java_files --> the compiled jar files and command to invoke the jar client with the required inputs

All such files are contained under module/container/to_copy folder.

#### database
There is a volume mounted in all running containers where the SQLite database is stored. The purpose of such database is to keep track of the state of each websocket connection, and the primary key is formed passing the three connection parameters as input to `sha1` hash function. The definition of the db can be found at module/container/to_copy/db_files.

#### cron job
The cron job runs the following command every minute to ensure that the websocket connection is healthy:

`netstat -tnp | grep ESTABLISHED | grep java | wc -l`

If it returns 1 or greater it considers that the connection is up and will refresh the database with the results. In case the result is 0, it will increase by 1 `count_err` column in the db. When `count_err` is greater than 2 the container will be stopped and the db updated setting the column `is_active` to 0 (False).

#### java files
An additional class called DEIntegration.java has been added to the java client in order to send each message recieved via websocket to the Data Engine datalake using a POST request.


## API Gateway + Lambda function

There are three methods available under the resource `/websocket` for this endpoint. Note that all require the same headers, three connections parameters (internalID, interWorkingInterface, platformID) and ASAPA credentials (asapa_email, asapa_pass). These methods are:
- POST

Invokes the method open_socket in the lambda function which runs the Docker container as follows:

`docker run -itd --rm -v volume_name:/home/ec2-user/vol --name websocketId \
                -e user='{}' \
                -e pass='{}' \
                -e internalID='{}' \
                -e interWorkingInterface='{}' \
                -e platformID='{}' \
                -e websocketId='{}' \
                image_name `

All the environment variables can be found at the headers of the POST request, but the websocketId which is calculated as explained before.

- GET

For the given headers returns whether or not the websocket connection is active.

- DELETE

Invokes the remove_socket method in the lambda function which stops the specified container by running:

`docker stop websocketId`

And then updates the database with the new state for that websocket.


## Terraform

## Requirements
- Gradle 4.5
- Python 3.9
- openjdk 1.8
- Terraform >= v1.2.3