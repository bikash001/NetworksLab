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
unit = None

for input_file in filelist:
	file = open(base_path+input_file,"r")
	print input_file

	for i in range(3):
		val = 0.0
		bwidth = 0.0
		count = 0
		for j in range(5):
			for k in range(range_val[i]):
				file.readline()
			indexline = str(file.readline())
			index = string.rfind(indexline,"=")
			if index == -1:
				raise Exception("= not found")
			file.readline()
			line = str(file.readline())
			temp = string.index(line,"(")
			end_index = string.index(line," ",temp)
			unit = line[end_index+1:end_index+5]
			if (unit == "MB/s"):
				val += float(indexline[index+1:-2])
				bwidth += float(line[temp+1:end_index])
				count += 1
			file.readline()


		print "Throughput: "+str(bwidth/count)+" "+str(unit)+"\tLatency: "+str(val/count)+" s"
	print ""
	file.close()

