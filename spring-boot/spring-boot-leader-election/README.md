```sh
mvn clean spring-boot:run

ARTIFACT_ID="spring-boot-leader-election"

oc create -f - <<EOF
apiVersion: v1
kind: ServiceAccount
metadata:
  name: $ARTIFACT_ID
---
apiVersion: v1
kind: RoleBinding
metadata:
  name: $ARTIFACT_ID
roleRef:
  # enable resources edit from whithin the pod
  name: edit
subjects:
  - kind: ServiceAccount
    name: $ARTIFACT_ID
EOF

mvn clean k8s:deploy -Pcloud
#mvn k8s:undeploy -Pcloud

# scale the service and observe that only one is active (singleton)
oc scale dc $ARTIFACT_ID --replicas=2
```
