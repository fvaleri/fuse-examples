```sh
# run EchoServer implementatons (io, nio, nio2, netty)
# using NIO (non-blocking IO) we have less thread context switch,
# so it scales much better than the traditional thread-per-request model
mvn clean compile exec:java -Pnio2

# test the TCP connection (Ctrl+C to quit)
nc -v localhost 9090

# raise number of clients and observe how it scales
mvn clean compile exec:java -Pclient \
    -Dhost=localhost -Dport=9090 -Dnum=500
```
