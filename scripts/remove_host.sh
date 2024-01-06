#!/bin/bash
input_file="./configs/hosts"

# Check if the file exists
if [ ! -f "$input_file" ]; then
    echo "File not found: $input_file"
    exit 1
fi

# Create a temporary file to store all lines except the last one
temp_file="$(mktemp)"
head -n -1 "$input_file" > "$temp_file"

# Replace the original file with the temporary file
mv "$temp_file" "$input_file"
