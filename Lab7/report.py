import subprocess
import matplotlib.pyplot as plt
import os

N = 50
p = [0.01,0.02,0.03,0.05,0.1]
W = [2,4]
M = 10000
thpt = []
file = open("temp.tmp","w")

for wsize in W:
	for prob in p:
		subprocess.call("java Main -N 50 -M 10000 "+str("-p ")+str(prob)+" -W "+str(wsize), stdout=file, shell=True)

file.close()
file = open("temp.tmp","r")

for i in W:
	vals = []
	for j in p:
		data = file.readline()
		data = data.split()
		vals.append(float(data[3]))
	thpt.append(vals)

file.close()
os.remove("temp.tmp")
# print thpt
plt.plot(p,thpt[0],"ro")
plt.plot(p,thpt[0])
plt.plot(p,thpt[1],"ro")
plt.plot(p,thpt[1])
plt.axis([0,0.2,0,1])
plt.show()