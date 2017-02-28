Let say d1, d2, d3, ......, d26 are data bits and
p1, p2, p4, p8, p16 are parity bits.
After including parity pits, the message will become
m1, m2, ....., m31
Here,
	m1 = p1
	m2 = p2
	m3 = d1
	m4 = p4
	m5 = d2
	.
	.
	m15 = d11
	m16 = p16
	m17 = d12
	.
	.
	m31 = d26

Now for calculating p1 include all the message bits
excluding m1(as it is p1 which we are calculating) whose binary representation
has 1 at the 2^0 th position.
So p1 = {m3, m5, 7, 9, .. all odd bits}

Now for calculating p16 include all the message bits
excluding m16(as it is p16 which we are calculating) whose binary representation
has 1 at the 2^4 th position.
So p16 = {m17, m18, m19, ..... till m31}


Following are the codes you have asked me to send.

int arr1[] = {3,5,7,9,11,13,15,17,19,21,23,25,27,29,31};
int arr2[] = {3,6,7,10,11,14,15,18,19,22,23,26,27,30,31};
int arr4[] = {5,6,7,12,13,14,15,20,21,22,23,28,29,30,31};
int arr8[] = {9,10,11,12,13,14,15,24,25,26,27,28,29,30,31};
int arr16[] = {17,18,19,20,21,22,23,24,25,26,27,28,29,30,31};

Here I have taken the position of message bits not data bits.