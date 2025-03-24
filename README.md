## Create Docker Spring Image File

```
docker build -t {docker-username}/{app-image-name}:{custom-version} -t {docker-username}/{app-image-name}:latest .
```
```
docker push {docker-username}/{app-image-name}:{custom-version}
docker push {docker-username}/{app-image-name}:latest
```


## Local Docker Testing

1. (Optional) Run PostgreSQL in a Separate Docker Container and Network:
```
docker network create {network_name}
```

2. Run PostgreSQL Container:
```
docker run --name {db-container-name} \
--network {network_name} \
-e POSTGRES_USER={db_user} \
-e POSTGRES_PASSWORD={db_password} \
-e POSTGRES_DB={db_name} \
-p {db_port}:5432 \
-d postgres
```
```
docker run --name postgres-db \
--network {network_name} \
-e POSTGRES_DB=test \
-e POSTGRES_USER=postgres \
-e POSTGRES_PASSWORD=password \
-p 5432:5432 \
-d postgres
```

3. Run Spring Container:
```
docker run --name {app-container-name} \
--network {network_name} \
-e DB_URL=host.docker.internal \
-e DB_PORT={db_port} \
-e DB_NAME={db_name} \
-e DB_USERNAME={db_user} \
-e DB_PASSWORD={db_password} \
-e ALLOWED_ORIGINS=* \
-e PROFILE=dev \
-p {app_port}:8080 \
-d {app-image-name}
```
```
docker run --name {app-container-name} \
--network {network_name} \
-e DB_URL=host.docker.internal \
-e DB_PORT=5432 \
-e DB_NAME=test \
-e DB_USERNAME=postgres \
-e DB_PASSWORD=password \
-e ALLOWED_ORIGINS=* \
-e PROFILE=dev \
-p 8080:8080 \
-d {app-image-name}
```