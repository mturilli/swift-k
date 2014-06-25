/*
 * RemoteCoasterException.cpp
 *
 *  Created on: Sep 11, 2012
 *      Author: mike
 */

#include "RemoteCoasterException.h"
#include <stdio.h>
#include <iostream>

using namespace Coaster;

using std::exception;
using std::string;

void expect(const char* s, int len, int& ptr, const char* e);
void skip(const char* s, int len, int& ptr, int n);
int getShort(const char* s, int len, int& ptr);
string* getString(const char* s, int len, int& ptr, int n);

RemoteCoasterException::RemoteCoasterException(const char* data, int len) {
	// These are actual serialized java exceptions
	/*int cnt = 1;
	char t[8];
	for (int i = 0; i < len; i++) {
		snprintf(t, 7, "%02hhx", data[i]);
		cout << t << " ";
		if ((cnt % 16) == 0) {
			cout << endl;
		}
		cnt++;
	}
	cout << endl;*/
	try {
		int ptr = 0;
		expect(data, len, ptr, "\xac\xed"); // magic
		skip(data, len, ptr, 2);
		expect(data, len, ptr, "\x73"); // new object
		expect(data, len, ptr, "\x72"); // new cls desc
		int clsNameLen = getShort(data, len, ptr);
		className = getString(data, len, ptr, clsNameLen);
		skip(data, len, ptr, 8); // serial version UID
		skip(data, len, ptr, 1); // flags
		// ....
		this->data.append(*className);
	}
	catch (...) {
		this->data.append("<unparsed data>");
	}
}

RemoteCoasterException::~RemoteCoasterException() {
	delete className;
}

string& RemoteCoasterException::str() {
	return data;
}

void expect(const char* s, int len, int& ptr, const char* e) {
	while (*e) {
		if (s[ptr] != *e) {
			throw exception();
		}
		ptr++;
		e++;
	}
}

void skip(const char* s, int len, int& ptr, int n) {
	ptr += n;
}

int getShort(const char* s, int len, int& ptr) {
	int v = (s[ptr] << 8) + s[ptr + 1];
	ptr += 2;
	return v;
}

string* getString(const char* s, int len, int& ptr, int n) {
	string* ss = new string(&(s[ptr]), n);
	ptr += len;
	return ss;
}

