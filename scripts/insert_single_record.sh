#!/bin/bash
DB_path="/virtual/a1group08/backup.db"

IFS='|' read -r short long node <<< "$1"
echo "$short $long $node"
sqlite3 "$DB_path" "INSERT INTO stuff (short, long, node) VALUES ('$short', '$long', '$node');"

echo "done"