import subprocess

sack 	= [0, 1]
wsize 	= [16*1024, 256*1024]
cong_alg= ["reno", "cubic"]
link_del= [2, 50]
ldp		= [0.5, 5] 
files = ["file1", "file2", "file3"]

iterations = 5

ip = "192.168.0.2"
port = "1081"

# Enable window scaling
subprocess.check_output(["sudo", "sysctl", "-w", "net.ipv4.tcp_window_scaling=" + str(1) + ""])

# Permute Selective ACK
for s in sack:
	subprocess.check_output(["sysctl", "-w", "net.ipv4.tcp_sack=" + str(s) + ""])
	
	# Permute Window Size
	for w in wsize:
		subprocess.check_output(["sysctl", "-w", "net.core.rmem_max=" + str(w) + ""])
		subprocess.check_output(["sysctl", "-w", "net.ipv4.tcp_rmem=" + str(w) + " " + str(w) + " " + str(w) + ""])
		
		# Permute Cong Algo
		for ca in cong_alg:
			subprocess.check_output(["sudo", "echo", str(ca), ">", "/proc/sys/net/ipv4/tcp_congestion_control"])
			
			# Permute Link Delays
			for ld in link_del:
				subprocess.check_output(["sudo", "tc", "qdisc", "replace", "dev", "eth0", "root", "netem", "delay", str(ld) + "ms", "10ms", "25%"])
				
				# Permute Link drop percentage
				for l in ldp:
					subprocess.check_output(["sudo", "tc", "qdisc", "change", "dev", "eth0", "root", "netem", "loss", str(l)+"%", "25%"])

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

					print "Finished a config!"