# Docker file for creating the image which runs the QA bot

FROM debian:7

RUN apt-get update
RUN apt-get install -y git openjdk-7-jdk maven