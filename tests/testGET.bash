#!/bin/bash

# Loop 100 times
for ((i=1; i<=100; i++)); do
    # Execute cURL GET requests in the background
    curl -X GET "http://dh2026pc01:8800/sid1" &
    curl -X GET "http://dh2026pc01:8800/sid2" &
    curl -X GET "http://dh2026pc01:8800/sid3" &
    curl -X GET "http://dh2026pc01:8800/sid4" &
    curl -X GET "http://dh2026pc01:8800/sid5" &
    curl -X GET "http://dh2026pc01:8800/sid6" &
    curl -X GET "http://dh2026pc01:8800/sid7" &
    curl -X GET "http://dh2026pc01:8800/sid8" &
    curl -X GET "http://dh2026pc01:8800/sid9" &
    curl -X GET "http://dh2026pc01:8800/sid10" &
    curl -X GET "http://dh2026pc01:8800/sid11" &
    curl -X GET "http://dh2026pc01:8800/sid12" &
    curl -X GET "http://dh2026pc01:8800/sid13" &
    curl -X GET "http://dh2026pc01:8800/sid14" &
    curl -X GET "http://dh2026pc01:8800/sid15" &
    curl -X GET "http://dh2026pc01:8800/sid16" &
    curl -X GET "http://dh2026pc01:8800/sid17" &
    curl -X GET "http://dh2026pc01:8800/sid18" &
    curl -X GET "http://dh2026pc01:8800/sid19" &
    curl -X GET "http://dh2026pc01:8800/sid20" &
    curl -X GET "http://dh2026pc01:8800/sid21" &
    curl -X GET "http://dh2026pc01:8800/sid22" &
    curl -X GET "http://dh2026pc01:8800/sid23" &
    curl -X GET "http://dh2026pc01:8800/sid24" &
    curl -X GET "http://dh2026pc01:8800/sid25" &
    curl -X GET "http://dh2026pc01:8800/sid26" &
    curl -X GET "http://dh2026pc01:8800/sid27" &
    curl -X GET "http://dh2026pc01:8800/sid28" &
    curl -X GET "http://dh2026pc01:8800/sid29" &
    curl -X GET "http://dh2026pc01:8800/sid30" &
    curl -X GET "http://dh2026pc01:8800/sid31" &
    curl -X GET "http://dh2026pc01:8800/sid32" &
    curl -X GET "http://dh2026pc01:8800/sid33" &
    curl -X GET "http://dh2026pc01:8800/sid34" &
    curl -X GET "http://dh2026pc01:8800/sid35" &
    curl -X GET "http://dh2026pc01:8800/sid36" &
    curl -X GET "http://dh2026pc01:8800/sid37" &
    curl -X GET "http://dh2026pc01:8800/sid38" &
    curl -X GET "http://dh2026pc01:8800/sid39" &
    curl -X GET "http://dh2026pc01:8800/sid40" &
    curl -X GET "http://dh2026pc01:8800/sid41" &
    curl -X GET "http://dh2026pc01:8800/sid42" &
    curl -X GET "http://dh2026pc01:8800/sid43" &
    curl -X GET "http://dh2026pc01:8800/sid44" &
    curl -X GET "http://dh2026pc01:8800/sid45" &
    curl -X GET "http://dh2026pc01:8800/sid46" &
    curl -X GET "http://dh2026pc01:8800/sid47" &
    curl -X GET "http://dh2026pc01:8800/sid48" &
    curl -X GET "http://dh2026pc01:8800/sid49" &
    curl -X GET "http://dh2026pc01:8800/sid50" &
    
    # Wait for all background processes to finish before starting the next loop
    wait
done