# VERSION 0.0.1
# Ĭ��ubuntu server����֧�ְ汾����ǰ��12.04
FROM ubuntu
# ǩ����
MAINTAINER zhanglin "zhanglin@puxtech.com"

# ���orache java7Դ��һ���԰�װvim��wget��curl��java7��tomcat7�ȱر����
RUN apt-get install python-software-properties
RUN add-apt-repository ppa:webupd8team/java
RUN apt-get update
RUN apt-get install -y oracle-java8-installer tomcat7

# ����JAVA_HOME��������
RUN update-alternatives --display java
RUN echo "JAVA_HOME=/usr/lib/jvm/java-8-oracle">> /etc/environment
RUN echo "JAVA_HOME=/usr/lib/jvm/java-8-oracle">> /etc/default/tomcat7