# Configuration

## You are here: [Home](index.md):[Configure](configure.md)

The ApplicationStarter Verticle reads the system configuration from a JSON file with the name `SFDCOoptions.json` in the root of your project. Additionally credentials are pulled from the environment since this is the prevalent method used on Heroku or other PaaS environments.

## SFDCOptions.json
Copy the `sampleconfig.json` file as a start. The JSON file has the following main elements:

- authConfigurations: Array with Auth providers. Important: the `authName` is used as reference by listners and consumer verticles
- consumerConfigurations: a consumer listens to one internal vert.x Eventbus address and consumes what a listener produces 
- listenerConfigurations: Array with listners. You can listen to multiple different Salesforce instances and topics, you need one entry for each topic. A listener can forward incoming data to multiple addresses on the eventbus
- parameters: parameters for the ApplicationStarter verticle config object
- Port: the localhost port the API listens to, mainly for shutdown or web sockets, typically provided from the environment
- proxy: Proxy IP or address (if any) - global setting
- proxyPort: numeric proxy port (if any) - global setting
- verticlesToLoad: other verticles you want to load

### Common Parameters
- authName: (optional) What authprovider is used, needs to exist
- autoStart: (optional) true/false Should the verticle automatically start listening (or wait for some eventbus to wake it)
- deployAsWorker: (optional) true/false should it run in a worker thread
- enabled: (optional) true/false - to temp disable a verticle
- instanceName: Unique name of a verticle entry used to retrieve configuration parameters from the environment or other config sources
- parameters: (optional) JSON object that gets passed to verticle.config()
- providesRouterExtension: the verticle must be Java, to add routes e.g. Websockets or incoming request (e.g. OBM)
- proxy: Proxy IP or address (if any)
- proxyPort: numeric proxy port (if any)
- verticleInstanceCount: (optional) how many instances should be deployed
- verticleName: the class or identifier to load a verticle - polyglot, any supported language

The proxy settings are provided (optional) for each verticle separately to accommodate different source/destination configurations  

### authConfig Parameters
- instanceName: must be the same as authName, authName takes priority
- serverURL: "login.salesforce.com", "test.salesforce.com", your known SF instance or a local mock server
- sfdcPassword: Password (don't put it here, put it into the environment)
- sfdcUser: UserName - optional here if provided in environment

### listenerConfigurations
- eventBusAddresses: Array of internal addresses to send to. E.g. `["SFDC.SampleEvents2", "SFDC.SampleEvents"]` an implementation could treat this as prefix only
- eventBusDedupAddress: needed when useDedupService = true, EventBus address of the deduplication service
- listenSubject: what to listen to in SFDC e.g. "/event/DataChange__e",
- useDedupService: true/false (optional, default = false) - Use a deduplication service, so incoming messages


### consumerConfigurations
- url: the URL of the consumer destination (REST, SOAP, Websockets)
- in parameters:
    -  `websocketname` : used by the websocket con

## Environment parameters
Mostly for compatibility for the PaaS environments and your security:
- port: Overwrites the listener port for the HTTP admin task
- For each authConfig sdfcUser and sfdcPassword prefixed by the authName

Sample:<br />
When your 2 authConfigs, one has `"authName" : "default"` and the other `"authName" : "system36"` then the following
 variables are pulled from the environment, overwriting values that might be in the JSON file:
 
- port
- default_sfdcUser
- default_sfdcPassword
- system36_sfdcUser
- system36_sfdcPassword

As far as I tested: all is case sensitive
 