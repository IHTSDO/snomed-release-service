#!/bin/sh
properties="data-service/src/main/resources/data-service.properties"
# Using temp file because inline edit syntax is platform dependant
sed 's/offlineMode.*/offlineMode = true/' ${properties} > ${properties}.edit
mv ${properties}.edit ${properties}
echo 'OFFLINE mode set, please redeploy.'
