# i2b2/tranSMART build process

# build locally

It might be required to build the application on the local machine. The requiremenst to set up the environment is slightly more involved. You need to install Java 8u111, Grails 2.5.4 and Groovy 4.9 before any of the scripts can be run. Recommend using `sdkman` for easier installation, but you can also check the commands in the `Dockerfile`, for an example on how to set up your own environment.

```
# In case there are missing plugins, or need to reinstall them in the local maven cache
./build_plugins.sh

# If all plugins are installed, the build_app.sh is sufficient
# to just create a fresh .war file locally.

# Note, if this script is ran from inside a repo, it will clone
# the repo again! in the current directory, which might not be
# the desired outcome.
./build_app.sh i2b2tm-130_pic-sure-auth

```

# build with `docker`

The repo includes a Dockerfile, in case you want to build on a machine with Docker enabled. The `Dockerfile` sets up an isolated environment where it automatically installs the required software components. It will also run the steps above, in the specific order they are required.

The container will create all the plugins, and will generate a `transmart.war` file in the `/root/transmartApp/target` directory.

```
docker build --tag transmart-builder . > build.log

```

After the build (which might take up to 20 minutes) completes. You can either run the container and configure/execute the application directly from it, or just copy the .war file out of it into the host environment.

```
docker run --it -p 80:8080 transmart-build sh
open http://localhost/transmart

```

# create runnable docker image locally

To create an docker image, that runs the generated .war file, in Tomcat, and can be included in a stack with other components (for example, in the [pic-sure reference stack](https://github.com/hms-dbmi/pic-sure-i2b2-transmart)), you can create a `transmart-runner` image (the name is just for example purposes).

```
docker build --tag transmart-runner --file Dockerfile-transmart-runner .

```

Now, you should have a docker image in your local registry, called `transmart-runner:latest`

```
[install]$ docker images transmart-runner
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
transmart-runner    latest              a5b8e1416c01        16 minutes ago      320MB


```

# publish the local image to Docker Hub
Later, if desired, you can `push` this image from your host machine to the DockerHub, to share your version of the application with others.

```
DOCKER_TAG=$(git branch | grep "*" | cut -d " " -f 2)_$(git log | head -1 | cut -d " " -f 2 | cut -c 1-10)
docker tag transmart-runner:latest dbmi/i2b2transmart:${DOCKER_TAG}
