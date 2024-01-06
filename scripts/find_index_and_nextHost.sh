#!/bin/bash
hosts_file="./configs/hosts"
current_host=$(hostname)
mapfile -t all_hosts < "$hosts_file"

i=0
for host in "${all_hosts[@]}"; do
#   echo "$host"
      if [[ "$host" == "$current_host" ]]; then
        index=$i
        break
    fi
    ((i++))
done
# echo "number of All Hosts: ${#all_hosts[@]}"

remote_index=$(( (index + 1) % ${#all_hosts[@]} ))
# echo "Remote index: $remote_index"
remote_host="${all_hosts[$remote_index]}"
# echo "Curr Host: $current_host"
echo "$index $remote_host"
