# Write your own Auth

## You are here: [Home](../index.md):Write your own:[Auth](auth.md)

See also:
- Write your own [Listener](listener.md)
- Write your own [Deduplication](dedup.md)
- Write your own [Consumer](consumer.md) 

## Why you want to do that
You need a different method of authentication

## How to implement
Extend [AbstractAuth](https://github.com/Stwissel/vertx-sfdc-platformevents/blob/master/sfdc-core/src/main/java/net/wissel/salesforce/vertx/auth/AbstractAuth.java) class and implement the login function

## Sample code

```java
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import io.vertx.core.Future;
import net.wissel.salesforce.vertx.config.AuthConfig;

public class Basic extends AbstractAuth{

    @Override
    protected Future<AuthInfo> login() {
        AuthConfig ac = this.getAuthConfig();
        String userCredentials = String.valueOf(ac.getSfdcUser())+":"+String.valueOf(ac.getSfdcPassword());
        String token = "Basic " + Base64.getEncoder().encodeToString(userCredentials.getBytes(StandardCharsets.UTF_8));
        AuthInfo ai = new AuthInfo(ac.getServerURL(), token);
        this.setCachedAuthInfo(ai);
        return Future.succeededFuture(ai);
    }

}
```

An asynchronous login (like the SOAP API) returns the composed future chain
instead, e.g. `return client.post(...).sendBuffer(body).compose(this::extractAuthInfo);`