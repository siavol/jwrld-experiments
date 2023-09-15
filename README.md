# Spring Boot Kotlin

Built with articles:
- https://spring.io/guides/tutorials/spring-boot-kotlin/
- https://spring.io/guides/gs/spring-boot-kubernetes/
- https://spring.io/guides/topicals/spring-on-kubernetes/

Run in kube:
1. Run minukube
    ```shell
    minikube start
    ```
2. Switch docker env
    ```shell
    eval $(minikube docker-env)
    ```
3. Build image
    ```shell
    ./gradlew bootBuildImage
    ```
4. Deploy
    ```shell
    kubectl apply -f deployment.yaml
    ```
5. Forward port for service
6. Open in browser
