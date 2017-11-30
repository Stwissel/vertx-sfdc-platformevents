# Configuration

The ApplicationStarter Verticle reads the system configuration from a JSON file with the name `SFDCOoptions.json` in the root of your project. Additionally credentials are pulled from the environment since this is the prevalent method used on Heroku or other PaaS environments.

## SFDCOptions.json
Copy the `sampleconfig.json` file as a start. The JSON file has the following main elements:

- authConfigurations: Array with Auth providers. Important: the `authName` is used as reference by listners and consumer verticles
- listenerConfigurations: Array with listners. You can listen to multiple different Salesforce instances and topics, you need one entry for each topic. A listener can forward incoming data to multiple addresses on the eventbus
- consumerConfigurations: a consumer listens to one internal vert.x Eventbus address and consumes what a listener produces 
- verticlesToLoad: other verticles you want to load
- parameters: parameters for the ApplicationStarter verticle config object
- Port: the localhost port the API listens to, mainly for shutdown or web sockets 

### Common Parameters
- verticleName: the class or identifier to load a verticle - polyglot, any supported language
- verticleInstanceCount: (optional) how many instances should be deployed
- deployAsWorker: (optional) true/false should it run in a worker thread
- autoStart: (optional) true/false Should the verticle automatically start listening (or wait for some eventbus to wake it)
- enabled: (optional) true/false - to temp disable a verticle
- proxy: Proxy IP or address (if any)
- proxyPort: numeric proxy port (if any)
- parameters: (optional) JSON object that gets passed to verticle.config()
- authName: What authprovider is used, needs to exist

### authConfig Parameters
- serverURL: "login.salesforce.com", "test.salesforce.com", your known SF instance or a local mock server
- sfdcUser: UserName - optional here if provided in environment
- sfdcPassword: Password (don't put it here, put it into the environment)

### listenerConfigurations
- listenSubject: what to listen to in SFDC e.g. "/event/DataChange__e",
- eventBusAddresses: Array of internal addresses to send to. E.g. `["SFDC.SampleEvents2", "SFDC.SampleEvents"]` an implementation could treat this as prefix only


### consumerConfigurations
- providesRouterExtension: the verticle must be Java, to add routes e.g. Websockets
- url: the URL of the consumer destination (REST, SOAP, Websockets)
- in parameters for the websockets: `websocketname`

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
 