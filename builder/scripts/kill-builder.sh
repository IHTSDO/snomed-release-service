#!/bin/sh
builderProcessId=`ps -ef | grep builder.ja[r] | awk '{print $2}'`
if [ -n "$builderProcessId" ]; then
	echo "Killing Builder process"
	kill -9 $builderProcessId
else
	echo "No Builder process running"
fi
