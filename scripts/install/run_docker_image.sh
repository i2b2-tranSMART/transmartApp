#!/bin/sh

#
# This is a sample script to run the generated i2b2transmart docker image
# on the current machine. This is a not a full test run, but rather a
# preliminary attempt at validating the image.
#

CURRENT_BRANCH=$(git branch | grep "*" | cut -d " " -f 2)
CURRENT_COMMIT=$(git log | head -1 | cut -d " " -f 2 | cut -c1-10)
DOCKER_IMAGE_TAG="${CURRENT_BRANCH}.${CURRENT_COMMIT}"
CONFIG_DIR="${HOME}/.grails/transmartConfig"

# --port 12345:8080 Should have no conflict with other containers
# --name transmart-test To signal that this is for testing purposes only
# --network transmart-net To isolate the container from others, while testing
# --rm To delete the container when it quits, for saving space.

docker run \
  --detach \
  --publish 12345:8080 \
  --name transmart-test \
  --rm \
  --volume ${CONFIG_DIR}:/root/.grails/transmartConfig \
  dbmi/i2b2transmart:${DOCKER_IMAGE_TAG}
