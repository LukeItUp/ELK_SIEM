#!/bin/bash

/usr/local/spark/bin/spark-submit --driver-class-path=/usr/local/elasticsearch-hadoop/dist/elasticsearch-spark-20_2.11-6.3.1.jar --class webserver_report /usr/local/siem_jar/webserver_report.jar collector.e5.ijs.si 80
