import random
import string

files = ["file1","file2", "file3"]
for i in range(len(files)):
	of = open(files[i], "w")
	of.write("".join([random.choice(string.ascii_uppercase) for _ in range(512*1024*(2**i))]))
	of.close()