# Authentication / Authorization

## You are here: [Home](index.md):[Authentication](auth.md)

Authentication / Authorization is provided by individual verticles. This isolates data operation from obtaining permissions.

An auth verticle is expected to return a [AuthInfo](https://github.com/Stwissel/vertx-sfdc-platformevents/blob/master/sfdc-core/src/main/java/net/wissel/salesforce/vertx/auth/AuthInfo.java) object that provides an authorization header (e.g. JWT Token, Session token, Basic Auth) and the server address to interact with.

This allows to have a different server for authentication and interaction. Salesforce works like that: you login to https://login.salesforce.com or https://test.salesforce.com, but interact with your instance name.

## Available Auth providers

### SOAP
used to authenticate with the Salesforce API, needs username and password (and token appended to password for Salesforce). The server URL is either `login.salesforce.com` or `test.salesforce.com` or your instance URL. No protocol specification required, only https is supported

```
{
    "authName": "default",
    "instanceName": "default",
    "verticleName": "net.wissel.salesforce.vertx.auth.SoapApi",
    "serverURL": "login.salesforce.com",
    "sfdcUser": "User",
    "sfdcPassword": "pwd"
}
```


### BASIC
Just returns username and password encoded for use in authentication header for HTTP. Typically used in consumers that post the data somewhere.

```
{
    "authName": "someService",
    "instanceName": "someService",
    "verticleName": "net.wissel.salesforce.vertx.auth.Basic",
    "serverURL": "login.myService.com",
    "sfdcUser": "User",
    "sfdcPassword": "pwd"
}
```

Note: even for other services, the variable name is `sfdcUser` and `sfdcPassword`

### OAUTH

- Work in progress