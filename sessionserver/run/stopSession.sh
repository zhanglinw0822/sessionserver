#!/bin/sh


dir=`pwd`
pid_num=`ps aux | grep "$dir" | grep -v grep | awk {'print $2'} | wc -l`
pid=`ps aux | grep "$dir" | grep -v grep | awk {'print $2'}`
if [ $pid_num -eq 1 ]; then
        kill -9 $pid
else
        echo -e "当前进程数不等于1,本次不做处理,请手动操作, 进程PID列表为\n $pid"
fi;