 SIEM sistem z uporabo odprtokodnih orodij
*******************************************

Uporabljena orodja:
- Kibana
- Elasticsearch
- Logstash
- Apache Spark
- Hadoop
- Cron
- Nginx
- Winlogbeat
- Rsyslog


***Vzpostavitev izvorne naprave Windows:
Na racunalnik namestimo program Winlogbeat.
Konfiguracijska datoteka za posiljanje belezenj se nahaja v direktoriju:
"./izvorne_naprave/winlogbeat/winlogbeat.yml"
Belezenja, ki se zapisuejo ob prikljucitvi USB naprave je potrebno se dodatno vkljuciti.
To storimo v programu EventViewer z desnim klikom na "Applications and Services Logs/Microsoft/Windows/
DriverFrameworks-UserMode/Operational", izberemo "Properties" in obkljukamo "Enable logging".

***Vzpostavitev izvorne naprave Linux:
Za zapisovanje SSH belezenj je najprej potrebno povisati stopnjo belezenja. To storimo v datoteki "sshd_config".
Primer taksne datoteke je "./izvorne_naprave/linux/sshd_config". 
Prav tako je potrebno programu Rsyslog dolociti, kam naj posilja belezenja.
Primer taksne datoteke je "./izvorne_naprave/linux/50-default.conf"

***SIEM sistem
Za vzpostavitev SIEM sistema je potrebno namestiti Elasticsearch, Kibano, Logstash, Apache Spark in Hadoop.
Logstash normalizira posredovane podatke glede na konfiguracijske datoteke v direktoriju "/etc/logstash/conf.d/".
Konfiguracijske datoteke se nahajajo v direktoriju "./siem/normalizacija/".
Prozenje pravil za zaznavo incidentov ali varnostnih grozenj izvaja program Cron.
Primer Cron nastavitev je v datoteki "./siem/pravila/crontab".
Cron poganja skripte, katere pozenejo Java program na Apache Sparku.
Skripte se nahajajo v direktoriju "./siem/pravila/script/".
Java programi, ki predstavljajo pravila za zaznavo incidentov in varnosnih grozenj se nahajajo v datoteki "./siem/pravila/".
Njihove izvorne kode se nahajajo v direktoriju "./siem/pravila/src".
Ce zelimo, da je Kibana dostopna tudi na vratih 80, namestimo program Nginx.
Datoteka, ki preusmeri promet iz vrat 80 na privzeta vrata Kibane se nahaja v direktoriju "/etc/nginx/sites-available/".
Primer taksne datoteke je "./siem/nginx/default".
  
