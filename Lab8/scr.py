#!/usr/bin/python
import random
import string

files = ["file4","file5", "file6"]
for i in range(len(files)):
	of = open(files[i], "w")
	of.write("".join([random.choice(string.ascii_uppercase) for _ in range(10240*1024*(i+1))]))
	of.close()