import subprocess
import sys

sack 	= [0, 1]
wsize 	= [16*1024, 256*1024]
cong_alg= ["reno", "cubic"]
link_del= [2, 50]
ldp		= [0.5, 5] 
files = ["file1", "file2", "file3"]

iterations = 5

if (len(sys.argv) < 3):
	print "Usage: <ip> <port>"

ip = sys.argv[1]
port = sys.argv[2]

# Enable window scaling
subprocess.check_output(["sudo", "sysctl", "-w", "net.ipv4.tcp_window_scaling=" + str(1) + ""])

# Permute Selective ACK
i = 0
for s in sack:
	print subprocess.check_output(["sudo","sysctl", "-w", "net.ipv4.tcp_sack=" + str(s) + ""])
	
	# Permute Window Size
	for w in wsize:
		print subprocess.check_output(["sudo","sysctl", "-w", "net.core.rmem_max=" + str(w) + ""])
		print subprocess.check_output(["sudo","sysctl", "-w", "net.ipv4.tcp_rmem=" + str(w) + " " + str(w) + " " + str(w) + ""])
		
		# Permute Cong Algo
		for ca in cong_alg:
			print subprocess.check_output(["sudo", "echo", str(ca), ">", "/proc/sys/net/ipv4/tcp_congestion_control"])
			
			# Permute Link Delays
			for ld in link_del:
				print subprocess.check_output(["sudo", "tc", "qdisc", "replace", "dev", "eth0", "root", "netem", "delay", str(ld) + "ms", "0.2ms", "25%"])
				
				# Permute Link drop percentage
				for l in ldp:
					print subprocess.check_output(["sudo", "tc", "qdisc", "change", "dev", "eth0", "root", "netem", "loss", str(l)+"%", "25%"])

					# Open file for writing res
					fil = open(str((s,w,ca,ld,l)), "w")

					# For all files
					for f in files:

						# For iteration times
						for i in range(iterations):
							temp_op = subprocess.check_output(["wget", ip+":"+port+"/"+f], stderr=fil)
							subprocess.check_output(["rm", "-rf", "file*"])

					# Close file
					fil.close()
					i += 1
					print "Finished a config! "+str(i)
