#!/bin/bash
rm -f /virtual/a1group08/backup.db 

# chmod +x find_index_and_nextHost.sh
# chmod +x /virtual/a1group08/backup.db 
result=$(./scripts/find_index_and_nextHost.sh)
read -r index remote_host <<< "$result"
echo "$result" > debug.txt
echo "result $result" >> debug.txt
NODE=$((index + 1))
SCHEMA_FILE="./javaSQLite/col3.sql"

# Specify the paths and filenames
# TODO: need to be renamed to main
# TODO: Maybe make the path an argument 
DB_path="/virtual/a1group08/backup.db"

echo "$remote_host  $NODE" >> debug.txt 

ssh $remote_host "sqlite3 '$DB_path' 'SELECT * FROM stuff WHERE node = $NODE;'" > output.txt

sqlite3 "$DB_path" < "$SCHEMA_FILE"
while IFS='|' read -r short long node; do
    sqlite3 $DB_path "INSERT INTO stuff (short, long, node) VALUES ('$short', '$long', '$node');"
done < output.txt
