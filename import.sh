docker exec -it oracle-db bash

sqlplus system/oracle

CREATE USER socobis IDENTIFIED BY socobis;
GRANT CONNECT, RESOURCE, DBA TO socobis;
ALTER USER socobis DEFAULT TABLESPACE USERS;
ALTER USER socobis TEMPORARY TABLESPACE TEMP;
exit;
exit;




docker cp /home/antonio/ITU/S5/mr-tahina/socobis/socobis_20251107/socobis_20251107.dmp oracle-db:/home/oracle/
docker exec -it oracle-db bash
imp socobis/socobis file=/home/oracle/socobis_20251107.dmp full=yes ignore=yes
