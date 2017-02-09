/* 
 * tcpclient.c - A simple TCP client
 * usage: tcpclient <host> <port>
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h> 
#include <sys/time.h>

#define BUFSIZE 1024

int timeval_subtract (struct timeval *result, struct timeval *x, struct timeval *y);

/* 
 * error - wrapper for perror
 */
void error(char *msg) {
    perror(msg);
    exit(0);
}

int main(int argc, char **argv) {
    int sockfd, portno, n;
    struct sockaddr_in serveraddr;
    struct hostent *server;
    char *hostname;
    char buf[BUFSIZE];
    int serverlen;
    struct timeval btime, etime, result;

    /* check command line arguments */
    if (argc != 3) {
       fprintf(stderr,"usage: %s <hostname> <port>\n", argv[0]);
       exit(0);
    }
    hostname = argv[1];
    portno = atoi(argv[2]);


    int i=0;
    if (gettimeofday(&btime, NULL) < 0)
        error("ERROR gettimeofday error.");

    for (i=0; i<100; ++i) {
        /* socket: create the socket */
        sockfd = socket(AF_INET, SOCK_DGRAM, 0);
        if (sockfd < 0) 
            error("ERROR opening socket");

        /* gethostbyname: get the server's DNS entry */
        server = gethostbyname(hostname);
        if (server == NULL) {
            fprintf(stderr,"ERROR, no such host as %s\n", hostname);
            exit(0);
        }

        /* build the server's Internet address */
        bzero((char *) &serveraddr, sizeof(serveraddr));
        serveraddr.sin_family = AF_INET;
        bcopy((char *)server->h_addr, 
          (char *)&serveraddr.sin_addr.s_addr, server->h_length);
        serveraddr.sin_port = htons(portno);

        /* get message line from the user */
            // printf("Please enter msg: ");
            bzero(buf, BUFSIZE);
            // fgets(buf, BUFSIZE, stdin);

        serverlen = sizeof(serveraddr);
        n = sendto(sockfd, "hello world", 12, 0, &serveraddr, serverlen);
        if (n < 0) 
          error("ERROR in sendto");
        
        n = recvfrom(sockfd, buf, strlen(buf), 0, &serveraddr, &serverlen);
        if (n < 0) 
          error("ERROR in recvfrom");
        printf("Echo from server: %s\n", buf);
        printf("server address: %s:%d\n", inet_ntoa(serveraddr.sin_addr), serveraddr.sin_port);
      close(sockfd);
    }
    // time(&etime);

    if (gettimeofday(&etime, NULL) < 0)
        error("ERROR gettimeofday error.");
    timeval_subtract(&result, &etime, &btime);
    double avg = (result.tv_sec*1000000 + result.tv_usec)/100.0;
    printf("average echo time in udp = %f\n", avg);
    return 0;
}

int timeval_subtract (struct timeval *result, struct timeval *x, struct timeval *y)
{
  /* Perform the carry for the later subtraction by updating y. */
  if (x->tv_usec < y->tv_usec) {
    int nsec = (y->tv_usec - x->tv_usec) / 1000000 + 1;
    y->tv_usec -= 1000000 * nsec;
    y->tv_sec += nsec;
  }
  if (x->tv_usec - y->tv_usec > 1000000) {
    int nsec = (x->tv_usec - y->tv_usec) / 1000000;
    y->tv_usec += 1000000 * nsec;
    y->tv_sec -= nsec;
  }

  /* Compute the time remaining to wait.
     tv_usec is certainly positive. */
  result->tv_sec = x->tv_sec - y->tv_sec;
  result->tv_usec = x->tv_usec - y->tv_usec;

  /* Return 1 if result is negative. */
  return x->tv_sec < y->tv_sec;
}