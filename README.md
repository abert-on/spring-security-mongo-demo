# Spring Security MongoDB Demo
A SpringBoot web app running with a MongoDB database.

Pages populated using Thymeleaf.

## Security Features Implemented ##

* User Registration
* User confirmation from email
* Zxcvbn passwordd strength validation
* user login
* User Details
* Change existing password
* Reset forgotten password

## Running ##
Update MongoConfiguration to point to an instance of a MongoDB database. 
This could be a [Docker instance](https://hub.docker.com/_/mongo/). Database and collections will be created on first login/registration attempt.

The following will build and run the application at localhost:8080
```shell
mvn clean install spring-boot:run
```

## TODO ##
* Replace zxcvbn messages with a strength bar
* Add SpringBootTests for endpoints
* Change user details
* Change email address

