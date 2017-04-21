import subprocess
import os
import json as simplejson
import requests

class logs:
	
	def __init__(self) :
		self.filecontent = ""
		self.previouscontent=""

	def getDetails(self) :
		file =open("logdetails","r")
		self.previouscontent=file.read()
		filein = subprocess.call("cat /var/log/dpkg.log >logdetails", shell = True , stdout = subprocess.PIPE)
		file = open("logdetails","r")
		self.filecontent = file.read()

	def getDictionary(self) :
		y=self.filecontent.splitlines()
		z=self.previouscontent.splitlines()
		x=[a for a in y if a not in z]
		if(len(x)>100):
			x=x[-100:]
		tmpdict = {"log" : x}
		return tmpdict

class NetworkDetails:

	def __init__(self) :
		self.ip_addr = ""
		self.mac_addr = ""

	def getDetails(self):
		try :
			filein = subprocess.call("/sbin/ifconfig eth0 >networkdetails",shell = True , stdout = subprocess.PIPE)
			file = open("networkdetails","r")
			details = file.read()
			tmp = details.find("inet addr")
			tmp = tmp + len("inet addr") + 1
			while(details[tmp] != " ") :
				self.ip_addr += details[tmp]
				tmp = tmp + 1
			tmp = details.find("HWaddr")
			tmp = tmp + len("HWaddr") + 1
			while(details[tmp] != " ") :
				self.mac_addr += details[tmp]
				tmp = tmp + 1
		except :
			print("Error occured while gathering Network details")
		return 

	def getDictionary(self):
		tmpdict = {"mac" : self.mac_addr}
		return tmpdict 

clientlog = logs()
clientlog.getDetails()
net = NetworkDetails()
net.getDetails() 
logsdata=dict(net.getDictionary().items()+clientlog.getDictionary().items())
print logsdata
url='http://127.0.0.1:8000/postlogs'
logsdata = simplejson.dumps(logsdata)
r = requests.post(url, data=logsdata)
r.text
r.status_code
