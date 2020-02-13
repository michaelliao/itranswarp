#!/bin/sh
requirepass=$(grep requirepass /etc/redis.conf | sed '/^#.*/d')
if [ -z $requirepass ];then
	echo requirepass {{ env.REDIS_PASSWORD }} >> /etc/redis.conf
else
	sed -i '/requirepass/d' /etc/redis.conf
	echo requirepass {{ env.REDIS_PASSWORD }} >> /etc/redis.conf
fi
