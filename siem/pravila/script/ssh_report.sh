#!/bin/bash

/usr/local/spark/bin/spark-submit --driver-class-path=/usr/local/elasticsearch-hadoop/dist/elasticsearch-spark-20_2.11-6.3.1.jar --class ssh_report /usr/local/siem_jar/ssh_report.jar 10
