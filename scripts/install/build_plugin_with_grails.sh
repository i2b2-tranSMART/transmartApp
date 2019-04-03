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

printf "\n********* Start Building %s Plugin *********\n" ${PLUGIN_NAME}
cd $PLUGINS_DIRECTORY
rm -fR ${PLUGIN_NAME}

git clone https://github.com/i2b2-tranSMART/${PLUGIN_NAME}.git
if [ ! -e "$PLUGIN_NAME" -o ! -d "$PLUGIN_NAME" ];
then
	echo "Error cloning the ${PLUGIN_NAME} repository. Please verify the input parameter."
	exit 2
fi
cd ${PLUGIN_NAME}

if [ "${GIT_BRANCH}" != "" ];
then
	git checkout ${GIT_BRANCH}
	RC=$?
	if [ $RC -ne 0 ];
	then
		echo "Error checking out ${GIT_BRANCH} branch. Please verify the input parameter."
		exit 2
	fi
fi
# This is a specific issue, until we update all master branches. The artifactory repo at Harvard
# is no longer operational. Remove it from all the BuildConfig.groovy script.
sed -i  s/.*ec2-35-170-59-132.compute-1.amazonaws.com:8080.*//g grails-app/conf/BuildConfig.groovy

grails compile
grails maven-install

printf "\n********* Finished Building %s Pluign *********\n" ${PLUGIN_NAME}
