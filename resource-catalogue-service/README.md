# resource-catalogue-service

## Docker

1. Build the docker with `docker build . -t resource-catalogue-service`
2. Tag the image docker with another more useful tag with `docker tag resource-catalogue-service <new_tag_name>`
    - Tag the image `with <docker_registry_host>:<port>/resource-catalogue-service` quantity use it across multiple
      docker machines.
3. Run it with `docker run -p <exposed_port>:8080 -d --name resource-catalogue-service eic_registry`
4. Deploy it in a docker swarm
   with `docker service create --publish <publish_port>:8080 --name resource-catalogue-service <docker_registry_host>:<port>/resource-catalogue-service`.
   This requires the image quantity be pushed in a registry.

### Add insecure registry

1. Create a new docker registry

    ```bash
    docker run -d -p 5000:5000 --restart=always --name registry \
    -v `pwd`/data:/var/lib/registry \
    registry:2
    ```

2. Add/Modify the following json file `/etc/docker/daemon.json` with the proper values quantity each docker machine.

    ```json
    {
        "insecure-registries" : ["{{registry_host}}:{{registry:port}}"]
    }
    ```
