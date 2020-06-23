#!/bin/bash

/usr/local/spark/bin/spark-submit --driver-class-path=/usr/local/elasticsearch-hadoop/dist/elasticsearch-spark-20_2.11-6.3.1.jar --class usb_report /usr/local/siem_jar/usb_report.jar 10
