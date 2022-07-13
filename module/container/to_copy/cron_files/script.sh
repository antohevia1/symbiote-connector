#!/bin/sh

n_connections=`netstat -tnp | grep ESTABLISHED | grep java | wc -l`
db_path=/home/ec2-user/vol/websockets.db

if [ $n_connections -gt 0 ]
then
        echo 'Websocket connection is up'
        sqlite3 $db_path  'UPDATE websockets set is_active=1, count_err=0 where websocket_id ="'${websocketId}'"'
else
        sqlite3 $db_path 'UPDATE websockets set count_err=count_err+1 where websocket_id ="'${websocketId}'"'
        retrys=`sqlite3 $db_path 'select count_err from websockets where websocket_id="'${websocketId}'"'`
        echo 'Websocket connection id down, retry n:'$retrys
        if [ $retrys -gt 2 ]
        then
                sqlite3 $db_path  'UPDATE websockets set is_active=0,count_err=0 where websocket_id ="'${websocketId}'"'
                kill $( pidof java )
        fi
fi

