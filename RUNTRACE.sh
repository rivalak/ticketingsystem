#!/bin/bash 

PACKAGENAME=ticketingsystem
if [ -d ./$PACKAGENAME  ]
then 
    rm -rf ./$PACKAGENAME/*
else 
    mkdir ./$PACKAGENAME
fi

JAVAFILES="TicketingDS.java TicketingSystem.java"
JAVAMAINFILE="Trace.java"

echo "--- main .java file ---"
echo $JAVAMAINFILE
echo "--- other .java file ---"
echo $JAVAFILES

# remove .java
JAVACFILES=$JAVAMAINFILE
for file in $JAVAFILES
do 
    JAVACFILES="$JAVACFILES $file"
done

echo "--- javac file ---"
echo $JAVACFILES

echo "--- start compiling ---"
javac $JAVACFILES -d ./

#java $NAME.class # wrong
echo "--- start running ---"
java $PACKAGENAME.${JAVAMAINFILE%.java}
