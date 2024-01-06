#!/bin/bash
arg1=$1
hosts_file="../configs/hosts"
mapfile -t all_hosts < "$hosts_file"
target_number=$(( ($arg1 - 1) % ${#all_hosts[@]} ))
DB_path="/virtual/a1group08/backup.db"
target_host="${all_hosts[$target_number]}"

while IFS="|" read -r shortURL longURL targetNumber; do
    sql_statements+="INSERT INTO stuff (short, long, node) VALUES ('$shortURL', '$longURL', $targetNumber);"
done < "../myclass/logs.txt"

ssh "$target_host" "sqlite3 "$DB_path" <<EOF
$sql_statements
EOF"
