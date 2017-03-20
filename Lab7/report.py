import subprocess
import matplotlib.pyplot as plt
import os

subprocess.call("make", shell=True)

N = 50
p = [0.01,0.02,0.03,0.05,0.1]
W = [2,4]
M = 100000
thpt = []
file = open("temp.tmp","w")

for wsize in W:
	for prob in p:
		subprocess.call("java Main -N "+str(N)+" -M "+str(M)+str(" -p ")+str(prob)+" -W "+str(wsize), stdout=file, shell=True)

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
plt.plot(p,thpt[0],color="red",label="window size = 2")
plt.plot(p,thpt[1],"bo")
plt.plot(p,thpt[1],color="blue",label="window size = 4")
plt.axis([0,0.15,0.3,0.45])
plt.ylabel("throughput")
plt.xlabel("probability")
plt.legend(loc="upper right")
plt.show()