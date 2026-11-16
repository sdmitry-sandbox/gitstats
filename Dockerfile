FROM hbr-1.int.ops.cldx.ru/cx-os/jdk:17.0.7 AS build
WORKDIR /workspace

RUN apt-get update && apt-get install -y nodejs npm && \
    npm install -g npm@10 && \
    rm -rf /var/lib/apt/lists/*

COPY . .
RUN --mount=type=cache,target=/root/.gradle,sharing=locked \
    ./gradlew :app:bootJar --no-daemon -x test -Pvaadin.productionMode=true

# Runtime-стадия
FROM hbr-1.int.ops.cldx.ru/cx-os/jdk:17.0.7
WORKDIR /app
COPY --from=build /workspace/app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]