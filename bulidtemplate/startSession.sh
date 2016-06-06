#!/bin/sh
baseDir=$(cd "$(dirname "$0")"; pwd)
cp=.
for file in $baseDir/libs/*.jar
do
   cp=$cp:$file
done
java -server -Xmx5120m -Xss256k -cp $cp tpme.PMES.timebargain.server.ServerShell >> $baseDir/stdout.out 2>&1 &
