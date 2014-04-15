#!/bin/sh
echo "Tailing Builder process log and all Tomcat logs."
echo "Ctrl+C to quit."
echo
vagrant ssh -c "tail -n0 -f /vagrant/builder/target/builder.log /var/log/tomcat7/*"
