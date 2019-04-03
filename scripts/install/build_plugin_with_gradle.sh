#!/bin/sh

if [ "${PLUGINS_DIRECTORY}" == "" ];
then
	echo "The PLUGINS_DIRECTORY environment variable is not set. Please set it to a writable directory."
	exit 2
fi

if [ ! -e "$PLUGINS_DIRECTORY" -o ! -d "$PLUGINS_DIRECTORY" ];
then
	echo "The PLUGINS_DIRECTORY is either missing or is not a directory. Please create the directory ${PLUGINS_DIRECTORY}."
	exit 2
fi

PLUGIN_NAME=$1

if [ "${PLUGIN_NAME}" == "" ];
then
	echo "There was no PLUGIN_NAME specified on the command line. It is mandatory!"
	echo "Usage: "$0" pluginName [gitBranch]"
	exit 3
fi

GIT_BRANCH=$2

printf "\n********* Start Building %s Plugin *********\n" $PLUGIN_NAME
cd $PLUGINS_DIRECTORY
rm -fR ${PLUGIN_NAME}
git clone https://github.com/i2b2-tranSMART/${PLUGIN_NAME}.git
if [ ! -e "$PLUGIN_NAME" -o ! -d "$PLUGIN_NAME" ];
then
	echo "Error cloning repository. Please check if it exists."
	exit 3
fi
cd ${PLUGIN_NAME}

if [ "${GIT_BRANCH}" != "" ];
then
	# If second parameter is specified, this build will build from
	# that branch, otherwise, it usually just builds from master
	git checkout ${GIT_BRANCH}
fi
# Due to gradle version being behind, this is required for now, otherwise
# warning message will instruct you to do this.
echo "enableFeaturePreview('STABLE_PUBLISHING')" >> settings.gradle

# Do a safe build, and pay attention to the warnings, if any.
gradle build --warning-mode all

# This project has a special task, that will put the compiled plugin in
# the local Maven cache.
gradle publishToMavenLocal

printf "\n********* Finished Building %s Plugin *********\n" $PLUGIN_NAME
