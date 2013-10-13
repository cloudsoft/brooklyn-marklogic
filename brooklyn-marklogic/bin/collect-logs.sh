#!/bin/bash

set -e

if [ $# -gt 0 ]; then
    HOSTS=$@
else
    echo "Asking Brooklyn for hosts"
    HOSTS=`python find-marklogic-nodes.py`
fi


for host in $HOSTS
do
    echo $host
    ssh -o "StrictHostKeyChecking no" $USER@$host \
        'mkdir -p logs && ' \
        'cd logs && ' \
        'sudo cp /var/log/messages . && sudo chmod 644 messages &&' \
        'cp /var/opt/MarkLogic/Logs/* . && ' \
        'tar czf ~/logs.tar.gz *'
    TGZ=$host.logs.tar.gz
    scp $USER@$host:logs.tar.gz $TGZ
    mkdir -p $host
    tar xzf $TGZ -C $host
    rm $TGZ
done
