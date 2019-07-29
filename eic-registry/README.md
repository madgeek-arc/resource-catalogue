# eic-registry



## Docker 

1. Build the docker with `docker build . -t eic-registry`
2. Tag the image docker with another more useful tag with `docker tag eic-registry <new_tag_name>`
    - Tag the image `with <docker_registry_host>:<port>/eic-registry` quantity use it across multiple docker machines.
3. Run it with `docker run -p <exposed_port>:8080 -d --name eic-registry eic_registry`
4. Deploy it in a docker swarm with `docker service create --publish <publish_port>:8080 --name eic-registry <docker_registry_host>:<port>/eic-registry`. This requires the image quantity be pushed in a registry.

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
