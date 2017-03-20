#include <iostream>
#include <string>
#include <fstream>
#include <math.h>
using namespace std;

int arr1[] = {3,5,7,9,11,13,15,17,19,21,23,25,27,29,31};
int arr2[] = {3,6,7,10,11,14,15,18,19,22,23,26,27,30,31};
int arr4[] = {5,6,7,12,13,14,15,20,21,22,23,28,29,30,31};
int arr8[] = {9,10,11,12,13,14,15,24,25,26,27,28,29,30,31};
int arr16[] = {17,18,19,20,21,22,23,24,25,26,27,28,29,30,31};

void gen();
char getParity1(string str);
char getParity2(string str);
char getParity4(string str);
char getParity8(string str);
char getParity16(string str);
string code(string txt);

int main(int argc, char** argv) {
	if (argc < 2) {
		cout << "Usage: <infile> <outfile>" << endl;
	}
	fstream infile;
	infile.open(argv[1], fstream::in);
	fstream outfile;
	outfile.open(argv[2], fstream::out);
	string str;
	string fstr;
	while (infile >> str) {
		fstr = code(str);
		outfile << fstr << endl;
	}
	outfile.close();
	infile.close();
	return 0;
}

char getParity1(string str) {
	int val = 0;
	for (int j=0; j<15; ++j) {
		if (str[arr1[j]] == '1') {
			++val;
		}
	}
	if (val % 2 == 0) {
		return '0';
	} else {
		return '1';
	}
}

string code(string txt) {
	char data[33];
	data[0] = 'x';
	data[1] = 'x';
	data[2] = 'x';
	data[4] = 'x';
	data[8] = 'x';
	data[16] = 'x';
	data[32] = '\0';
	data[3] = txt[0];
	int i=1;
	for (int j=5; j<8; ++j, ++i) {
		data[j] = txt[i];
	}
	for (int j=9; j<16; ++j, ++i) {
		data[j] = txt[i];
	}
	for (int j=17; j<32; ++j, ++i) {
		data[j] = txt[i];
	}
	data[1] = getParity1(data);
	data[2] = getParity2(data);
	data[4] = getParity4(data);
	data[8] = getParity8(data);
	data[16] = getParity16(data);
	string str(data);
	return str.substr(1);
}

char getParity2(string data) {
	int val = 0;
	for (int j=0; j<15; ++j) {
		if (data[arr2[j]] == '1') {
			++val;
		}
	}
	if (val % 2 == 0) {
		return '0';
	} else {
		return '1';
	}
}
char getParity4(string data) {
	int val = 0;
	for (int j=0; j<15; ++j) {
		if (data[arr4[j]] == '1') {
			++val;
		}
	}
	if (val % 2 == 0) {
		return '0';
	} else {
		return '1';
	}
}
char getParity8(string data) {
	int val = 0;
	for (int j=0; j<15; ++j) {
		if (data[arr8[j]] == '1') {
			++val;
		}
	}
	if (val % 2 == 0) {
		return '0';
	} else {
		return '1';
	}
}
char getParity16(string data) {
	int val = 0;
	for (int j=0; j<15; ++j) {
		if (data[arr16[j]] == '1') {
			++val;
		}
	}
	if (val % 2 == 0) {
		return '0';
	} else {
		return '1';
	}
}

// Sample input generator
/*
void gen()
{
	char arr[27];
	arr[26] = '\0';
	for (int i=0; i<50; ++i) {
		for (int j=0; j<26; ++j) {
			if (random() % 2 == 0) {
				arr[j] = '0';
			} else {
				arr[j] = '1';
			}
		}
		cout << arr << endl;
	}

}
*/