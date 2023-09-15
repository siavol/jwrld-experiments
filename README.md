# Spring Boot Kotlin

Built with articles:
- https://spring.io/guides/tutorials/spring-boot-kotlin/
- https://spring.io/guides/gs/spring-boot-kubernetes/
- https://spring.io/guides/topicals/spring-on-kubernetes/

## Pre-requisites

Kubernetes cluster, f.e. minikube:
1. Run minukube
    ```shell
    minikube start
    ```
2. Switch docker env
    ```shell
    eval $(minikube docker-env)
    ```

Helm.

## Setup Kafka

Follow [Strimzi Kafka](https://strimzi.io/quickstarts/)
to install kafka cluster into `kafka` namespace.

Install Kafka-ui:
1. Add helm repo
   ```shell
   helm repo add kafka-ui https://provectus.github.io/kafka-ui-charts
   ```
2. Deploy config map for kafka ui
   ```shell
   kubectl apply -f ./k8s/kafka-ui.yaml
   ```
3. Install kafka-ui
   ```shell
   helm install kafka-ui kafka-ui/kafka-ui --set existingConfigMap="kafka-ui-configmap"
   ```


## Build and run in kube:

1. Build image
    ```shell
    ./gradlew bootBuildImage
    ```
2. Deploy
    ```shell
    kubectl apply -f deployment.yaml
    ```
3. Forward port for service
4. Open in browser
