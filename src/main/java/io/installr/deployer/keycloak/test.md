curl -isSLX PUT \
    -H 'Content-Type: multipart/form-data' \
    -F 'data=@test.realm.json' \
    -F 'media-type=application/vnd.io.installr.kc-realm' \
    -F 'env={"keycloak.url":"http://localhost:30006/public/sec/auth","keycloak.user":"coadmin","keycloak.password":"coadmin"};type=application/json' \
    -F 'encoding=UTF-8' \
    'http://localhost:8080/Deployer.deploy/test.realm.json'
curl -ifsSLX PUT \
    -H 'Content-Type: multipart/form-data' \
    -F 'data=@test.client.json' \
    -F 'media-type=application/vnd.io.installr.kc-client' \
    -F 'env={"keycloak.url":"http://localhost:30006/public/sec/auth","keycloak.user":"coadmin","keycloak.password":"coadmin"};type=application/json' \
    -F 'encoding=UTF-8' \
    'http://localhost:8080/Deployer.deploy/test.client.json'
curl -isSLX DELETE \
    -H 'Content-Type: multipart/form-data' \
    -F 'data=@test.realm.json' \
    -F 'media-type=application/vnd.io.installr.kc-realm' \
    -F 'env={"keycloak.url":"http://localhost:30006/public/sec/auth","keycloak.user":"coadmin","keycloak.password":"coadmin"};type=application/json' \
    -F 'encoding=UTF-8' \
    'http://localhost:8080/Deployer.undeploy/test.realm.json'
curl -ifsSLX DELETE \
    -H 'Content-Type: multipart/form-data' \
    -F 'data=@test.client.json' \
    -F 'media-type=application/vnd.io.installr.kc-client' \
    -F 'env={"keycloak.url":"http://localhost:30006/public/sec/auth","keycloak.user":"coadmin","keycloak.password":"coadmin"};type=application/json' \
    -F 'encoding=UTF-8' \
    'http://localhost:8080/Deployer.undeploy/test.client.json'
