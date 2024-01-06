# Define the database file
database_file="/virtual/a1group08/backup.db"
# sqlite3 /virtual/a1group08/backup.db < col3.sql
chmod 777 /virtual/a1group08/backup.db

# Insert 10 random rows into the database
for i in {1..10}; do
    col1=$(head /dev/urandom | tr -dc A-Za-z0-9 | head -c 5)
    col2=$(head /dev/urandom | tr -dc A-Za-z0-9 | head -c 10)
    col3=$((RANDOM % 2 + 3))

    # Insert the row into the database
    sqlite3 "$database_file" <<EOF
INSERT INTO stuff (short, long, node) VALUES ('$col1', '$col2', $col3);
EOF
done