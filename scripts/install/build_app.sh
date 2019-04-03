#!/bin/sh

GIT_BRANCH=$1

if [ -d "transmartApp" ];
then
	rm -fR transmartApp
fi

git clone https://github.com/i2b2-tranSMART/transmartApp.git
cd transmartApp

if [ "${GIT_BRANCH}" != "" ];
then
	git checkout $GIT_BRANCH
fi

grails war
