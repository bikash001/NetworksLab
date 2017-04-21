import subprocess
import matplotlib.pyplot as plt

inp = ['1','1','1','0.1','0.01','200','outfile']
i = ['1','4']
m = ['1', '1.5']
n = ['0.5', '1']
f = ['0.1', '0.3']
s = ['0.01', '0.0001']

for a in i:
	for b in m:
		for c in n:
			for d in f:
				for e in s:
					subprocess.call("java Main -i "+a+' -m '+b+' -n '+c+' -f '+d+' -s '+e+' -T 1000 -o '+inp[6], shell = True)
					file = open(inp[6])
					x_val = []
					y_val = []
					i = 0;
					for line in file:
						x_val.append(i)
						i += 1
						y_val.append(float(line))
					file.close()
					# print thpt
					plt.plot(x_val,y_val)
					# plt.axis([0,0.15,0.3,0.45])
					param = (a,b,c,d,e)
					plt.title('parameters: '+str(param))
					plt.ylabel("congestion window (KB)")
					plt.xlabel("round number")
					plt.savefig(str(param)+'.png')
					plt.gcf().clear()