FROM openjdk:8-jdk-alpine

WORKDIR /home/ec2-user
ENV VOL=/home/ec2-user/vol


RUN apk upgrade --available && apk add nano && apk add sqlite && apk add openrc && apk add --no-cache tini openrc busybox-initscripts


ARG home=/home/ec2-user 
COPY ./to_copy/ $home/


RUN chmod 755 $home/cron_files/script.sh $home/entry.sh $home/cron_files/crond_process.sh $home/java_files/java_process.sh
RUN /usr/bin/crontab $home/cron_files/crontab.txt

ENTRYPOINT .$home/entry.sh
