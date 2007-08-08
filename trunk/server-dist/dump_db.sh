#!/bin/sh

# Called with time/date as the argument

DUMPEXE=`which mysqldump`
DBUSER="sa"
DBPASS="master"
DBDATABASE="mekwars"

TIME=$1

OUTPUTFILE="./campaign/backup/db_$TIME.sql"

echo dumpexe: $DUMPEXE
echo Output file: $OUTPUTFILE

$DUMPEXE -u$DBUSER -p$DBPASS $DBDATABASE > $OUTPUTFILE

