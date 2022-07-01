#!/bin/sh


echo  `ls`
echo `pwd`
#start cron in the background
./cron_files/crond_process.sh &


#start the java process
./java_files/java_process.sh
