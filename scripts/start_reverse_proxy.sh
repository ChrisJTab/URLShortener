if [ $# -eq 3 ]; then
		echo "Usage: start_reverse_proxy ProxyPort URLServerPort"
		exit 1
fi

REVERSE_PROXY_PORT=$1
SERVER_PORT=$2

# Compile and execute the Java code
javac -cp . ./myclass/ReverseProxy.java
if [ $? -eq 0 ]; then
    java -cp . myclass.ReverseProxy "$REVERSE_PROXY_PORT" "$SERVER_PORT"
else
    echo "Compilation failed. Please fix any Java code errors."
fi