#!/bin/sh

#
# This script will create a new docker image, labelled dbmi/i2b2transmart and
# tagged with the current branch/commit.
#
# For example, if the current branch is `master` and the latest commit
# is `03a2f8ad2`, the image generated will be
# tagged as `dbmi/i2b2transmart:master.03a2f8ad2`

# Get the freshly built .war file
cp ../../target/transmart.war ./

CURRENT_BRANCH=$(git branch | grep "*" | cut -d " " -f 2)
CURRENT_COMMIT=$(git log | head -1 | cut -d " " -f 2 | cut -c1-10)

DOCKER_IMAGE_TAG="${CURRENT_BRANCH}.${CURRENT_COMMIT}"
docker build \
  --rm \
  --file Dockerfile-transmart-runner \
  --tag dbmi/i2b2transmart:${DOCKER_IMAGE_TAG} \
  .
