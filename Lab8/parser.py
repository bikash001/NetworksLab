#!/usr/bin/python
import sys
import string
from os import listdir

if (len(sys.argv) < 2):
	print "Usage: <path to input files>"
	sys.exit()

base_path = sys.argv[1]
range_val = [16,26,46]
filelist = listdir(base_path)

for input_file in filelist:
	file = open(base_path+input_file,"r")
	print input_file

	for i in range(3):
		val = 0.0
		bwidth = 0.0
		for j in range(5):
			for k in range(range_val[i]):
				file.readline()
			line = str(file.readline())
			index = string.rfind(line,"=")
			if index != -1:
				val += float(line[index+1:-2])
			else:
				raise Exception("= not found")
			file.readline()
			line = str(file.readline())
			temp = string.index(line,"(")
			bwidth += float(line[temp+1:string.index(line," ",temp)])
			file.readline()


		print "Throughput: "+str(bwidth/5)+" MB/s \tLatency: "+str(val/5)+" s"
	print ""
	file.close()

