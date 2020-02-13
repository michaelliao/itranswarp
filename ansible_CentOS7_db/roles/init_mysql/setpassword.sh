mysqlinitpasswd=`grep "password is generated" /var/log/mysqld.log | awk '{print $NF}'`
mysql -uroot -p${mysqlinitpasswd} -e "alter user 'root'@'localhost' identified by '{{env.DB_ROOTPASSWORD}}';" --connect-expired-password
#CREATE DATABASE {{env.DB_NAME}};grant {{env.DB_NAME}} on *.* to '{{env.DB_USERNAME}}'@'%'identified by '{{env.DB_PASSWORD}}';flush privileges" --connect-expired-password
#if [ ! $0 == 0 ];then
#  mysql -uroot -p{{env.DB_ROOTPASSWORD}} -e "alter user 'root'@'localhost' identified by '{{env.DB_ROOTPASSWORD}}';CREATE DATABASE {{env.DB_NAME}};grant {{env.DB_NAME}} on *.* to '{{env.DB_USERNAME}}'@'%'identified by '{{env.DB_PASSWORD}}';flush privileges" --connect-expired-password
#fi
#
