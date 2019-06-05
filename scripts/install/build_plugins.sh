#!/bin/sh

mkdir -p ~/plugins
export PLUGINS_DIRECTORY=~/plugins

./build_plugin_with_gradle.sh transmart-core-api

PLUGINS="transmart-java
transmart-shared
biomart-domain
search-domain
transmart-core-db
transmart-core-db-tests
transmart-custom
transmart-legacy-db
folder-management-plugin
dalliance-plugin
blend4j-plugin
spring-security-auth0
Rmodules
transmart-gnome
transmart-mydas
transmart-fractalis
transmart-metacore-plugin
transmart-gwas-plugin
transmart-rest-api
transmart-xnat-importer-plugin
transmart-xnat-viewer"

for PLUGIN_NAME in $PLUGINS
do
	./build_plugin_with_grails.sh ${PLUGIN_NAME}
done
echo "Finished building all plugins."
exit
