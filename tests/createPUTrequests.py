with open('../database.txt', 'r') as input_file, open('PUTcommands.txt', 'w') as output_file:
    for line in input_file:
        parts = line.strip().split('\t')
        if len(parts) == 2:
            first_part, second_part = parts
            curl_command = f'curl -X PUT "http://localhost:8080/?short={first_part}&long={second_part}" -H "Content-Length:0"'
            output_file.write(curl_command + '\n')
