# Installr Deployer Interface for keycloak-config-cli
This commit adds a Installr (installr.io) compatible deployer interface to keycloak-config-cli.

## Build
```
mvn clean package -DskipTests -Dkeycloak.version=24.0.5 -Dkeycloak.client.version=24.0.5 -Ppre-keycloak26
```

## Run
```
java -Dspring.main.web-application-type=SERVLET -jar ./target/keycloak-config-cli.jar \
    --logging.level.root=warn \
    --keycloak.ssl-verify=false \
    --keycloak.url= \
    --keycloak.user= \
    --keycloak.password= \
    --import.files.locations= \
    --import.managed.authentication-flow=no-delete \
    --import.managed.identity-provider=no-delete \
    --import.managed.client=no-delete
```

## Test
### deploy
```
curl -isSLX PUT \
    -H 'Content-Type: multipart/form-data' \
    -F 'data=@test.realm.json' \
    -F 'media-type=application/vnd.io.installr.kc-realm' \
    -F 'env={"keycloak.url":"<KEYCLOAK_URL>","keycloak.user":"<KEYCLOAK_USER>","keycloak.password":"<KEYCLOAK_PASSWORD>"};type=application/json' \
    -F 'encoding=UTF-8' \
    'http://localhost:8080/Deployer.deploy/test.realm.json'
```

```
curl -isSLX PUT \
    -H 'Content-Type: multipart/form-data' \
    -F 'data=@test.client.json' \
    -F 'media-type=application/vnd.io.installr.kc-client' \
    -F 'env={"keycloak.url":"<KEYCLOAK_URL>","keycloak.user":"<KEYCLOAK_USER>","keycloak.password":"<KEYCLOAK_PASSWORD>"};type=application/json' \
    -F 'encoding=UTF-8' \
    'http://localhost:8080/Deployer.deploy/test.client.json'
```
### undeploy
```
curl -isSLX DELETE \
    -H 'Content-Type: multipart/form-data' \
    -F 'data=@test.realm.json' \
    -F 'media-type=application/vnd.io.installr.kc-realm' \
    -F 'env={"keycloak.url":"<KEYCLOAK_URL>","keycloak.user":"<KEYCLOAK_USER>","keycloak.password":"<KEYCLOAK_PASSWORD>"};type=application/json' \
    -F 'encoding=UTF-8' \
    'http://localhost:8080/Deployer.undeploy/test.realm.json'
```

```
curl -isSLX DELETE \
    -H 'Content-Type: multipart/form-data' \
    -F 'data=@test.client.json' \
    -F 'media-type=application/vnd.io.installr.kc-client' \
    -F 'env={"keycloak.url":"<KEYCLOAK_URL>","keycloak.user":"<KEYCLOAK_USER>","keycloak.password":"<KEYCLOAK_PASSWORD>"};type=application/json' \
    -F 'encoding=UTF-8' \
    'http://localhost:8080/Deployer.undeploy/test.client.json'
```

## Make patch
```
./make-patch
```
