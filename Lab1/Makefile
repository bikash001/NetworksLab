CC = gcc
FLAGS = -o
all: udpserver udpclient tcpserver tcpclient

udpserver: udpserver.c
	$(CC) $(FLAGS) udpserver udpserver.c

tcpserver: tcpserver.c
	$(CC) $(FLAGS) tcpserver tcpserver.c

udpclient: udpclient.c
	$(CC) $(FLAGS) udpclient udpclient.c

tcpclient: tcpclient.c
	$(CC) $(FLAGS) tcpclient tcpclient.c

clean:
	rm udpclient udpserver tcpclient tcpserver