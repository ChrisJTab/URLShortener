#!/bin/bash
CWD="`pwd`";

HOST_FILE="./configs/hosts";
COMMAND_FILE="./configs/commands";

# Check if the hosts file exists
if [ ! -f "$HOST_FILE" ]; then
    echo "Hosts file not found: $HOST_FILE"
    exit 1
fi

# Check if there are no arguments
if [ $# -eq 0 ]; then
    echo "No arguments provided. Please provide a port number"
    exit 1
fi
PORT_NUMBER=$1

JAVA_COMMAND="javac myclass/URL1.java; java -cp .:./javaSQLite/sqlite-jdbc-3.39.3.0.jar myclass.URL1 $PORT_NUMBER"
LOG_FILE_NAME="URLShortener"

rm -r -f $COMMAND_FILE
touch $COMMAND_FILE

# Set up a trap to kill the Java process when the script is terminated
trap 'kill $java_pid' EXIT

while IFS= read -r host; do
    # Add your custom processing logic here
    echo "cd ~/Documents/CSC409/a1group08/; HOST=\$(uname -n); $JAVA_COMMAND >\"./logs/$LOG_FILE_NAME \$HOST.stdout\" 2>\"./logs/$LOG_FILE_NAME \$HOST.stderr\"" >> $COMMAND_FILE
done < "$HOST_FILE"

# Run the commands in parallel
parallel --sshloginfile $HOST_FILE < $COMMAND_FILE &
java_pid=$!

# Wait for the parallel process to finish
wait

# Remove the command file
rm -r -f $COMMAND_FILE

# Remove the trap so the Java process isn't killed again
trap - EXIT