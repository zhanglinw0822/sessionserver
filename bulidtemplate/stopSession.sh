#!/bin/sh


dir=`pwd`
pid_num=`ps aux | grep "$dir" | grep -v grep | awk {'print $2'} | wc -l`
pid=`ps aux | grep "$dir" | grep -v grep | awk {'print $2'}`
if [ $pid_num -eq 1 ]; then
        kill -9 $pid
else
        echo -e "��ǰ������������1,���β�������,���ֶ�����, ����PID�б�Ϊ\n $pid"
fi;