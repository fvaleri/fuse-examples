```sh
# start Keycloak standalone (AuthorizationServer)
# crate a test Realm and add a demo-app Client application
# create test-user wit user role and test-admin with admin role

# run both Client applications (products and providers microservices)
mvn clean compile
mvn spring-boot:run -f ./spring-boot-keycloak-jwt-products/pom.xml
mvn spring-boot:run -f ./spring-boot-keycloak-jwt-providers/pom.xml

# unauthorized access to products (requires an access_token)
curl -v http://localhost:8081/products

# get the access_token (JWT) from Keycloak using basic HTTP authentication
# and use it to access the protected resource using the OAuth2 password grant type
SOCKET="localhost:8080"
REALM="test"
CLIENT_ID="demo-app"

jwt() {
    local username="$1"
    local password="$2"
    if [[ -n $SOCKET && -n $REALM && -n $CLIENT_ID && -n $username && -n $password ]]; then
        local token=$(curl -sX POST http://$SOCKET/auth/realms/$REALM/protocol/openid-connect/token --insecure \
            -H "Content-Type: application/x-www-form-urlencoded" \
            -d "grant_type=password" \
            -d "client_id=$CLIENT_ID" \
            -d "username=$username" \
            -d "password=$password" | jq -r '.access_token')
        printf $token;
    fi
}

jwtd() {
    if [[ -x $(command -v jq) ]]; then
         jq -R 'split(".") | .[0],.[1] | @base64d | fromjson' <<< "${1}"
         echo "Signature: $(echo "${1}" | awk -F'.' '{print $3}')"
    fi
}

TOKEN=$(jwt test-user user)
jwtd $TOKEN

curl -v -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" http://localhost:8081/products

# forbidden access to product details (requires admin role)
curl -v -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" http://localhost:8081/products/1

# get the access_token with the admin User and retry
TOKEN=$(jwt test-admin admin)

curl -v -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" http://localhost:8081/products/1
```
