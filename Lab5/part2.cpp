#include <iostream>
#include <string>
#include <fstream>
#include <vector>
#include <math.h>
using namespace std;

int arr1[] = {3,5,7,9,11,13,15,17,19,21,23,25,27,29,31};
int arr2[] = {3,7,10,11,14,15,18,19,22,23,26,27,30,31};
int arr4[] = {5,7,12,13,14,15,20,21,22,23,28,29,30,31};
int arr8[] = {9,10,11,12,13,14,15,24,25,26,27,28,29,30,31};
int arr16[] = {17,18,19,20,21,22,23,24,25,26,27,28,29,30,31};

char getParity1(string str);
char getParity2(string str);
char getParity4(string str);
char getParity8(string str);
char getParity16(string str);
string code(string txt);
void error_detection(string, fstream&);
void gen_error(string str, fstream &fs);

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
		error_detection(str, outfile);
		outfile << endl;
		for (int i=0; i<10; ++i) {
			gen_error(str, outfile);
			outfile << endl;
		}
		outfile << endl << endl;
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
	for (int j=0; j<14; ++j) {
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
	for (int j=0; j<14; ++j) {
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

int receiver(string str)
{
	string rcv = "x"+str;
	int index = 0;
	if (getParity16(rcv) != rcv[16]) {
		index += 16;
	}
	if (getParity8(rcv) != rcv[8]) {
		index += 8;
	}
	if (getParity4(rcv) != rcv[4]) {
		index += 4;
	}
	if (getParity2(rcv) != rcv[2]) {
		index += 2;
	}
	if (getParity1(rcv) != rcv[1]) {
		index += 1;
	}
	return index;
}

void error_detection(string str, fstream &fs)
{
	string pstr = code(str);
	fs << "Original String: " << str << endl;
	fs << "Original String with Parity: " << pstr << endl;
	int p = random() % 32;
	if (pstr[p] == '0') {
		pstr[p] = '1';
	} else {
		pstr[p] = '0';
	}
	fs << "Corrupted String: " << pstr << endl;
	fs << "Error Location is: " << p << endl;
	fs << "Number of Errors Introduced: 1" << endl;
	fs << "Error Location computed by receiver algorithm is: " << (receiver(pstr)-1) << endl;
}

int recv_data(string str, std::vector<int> &v) 
{
	string rcv = "x"+str;
	int index = 0;
	if (getParity1(rcv) != rcv[1]) {
		index += 1;
		v.push_back(1);
	}
	if (getParity2(rcv) != rcv[2]) {
		index += 2;
		v.push_back(2);
	}
	if (getParity4(rcv) != rcv[4]) {
		index += 4;
		v.push_back(3);
	}
	if (getParity8(rcv) != rcv[8]) {
		index += 8;
		v.push_back(4);
	}
	if (getParity16(rcv) != rcv[16]) {
		index += 16;
		v.push_back(5);
	}
	return index;
}

void gen_error(string str, fstream &fs)
{
	string pstr = code(str);
	fs << "Original String: " << str << endl;
	fs << "Original String with Parity: " << pstr << endl;
	int p, q;
	p = random() % 32;
	do {
		q = random() % 32;
	} while (p == q);
	if (pstr[p] == '0') {
		pstr[p] = '1';
	} else {
		pstr[p] = '0';
	}
	if (pstr[q] == '0') {
		pstr[q] = '1';
	} else {
		pstr[q] = '0';
	}
	fs << "Corrupted String: " << pstr << endl;
	fs << "Error Locations are: " << p << " and " << q << endl;
	fs << "Number of Errors Introduced: 2" << endl;
	fs << "Error Detected: ";
	std::vector<int> v;
	if (recv_data(pstr, v) != 0) {
		fs << "Yes" << endl;
	} else {
		fs << "No" << endl;
	}
	fs << "Parity bits that failed are at location: " ;
	for (vector<int>::iterator itr=v.begin(), end=v.end(); itr!=end; ++itr) {
		fs << *itr << " ";
	}
	fs << endl;
}