#! /usr/bin/env python 
import subprocess
import shlex
import httplib
import time
import base64
import urllib2
import os
from datetime import date
from optparse import OptionParser

USER=""
RPM_PATH=""
LICENSEE=""
LICENSE=""

#change to /var/opt/MARKlogic if solaris
ML_PATH="/var/opt/MarkLogic"

def buildHostname(root, index):
    return root + index

def getStaticHostnames():
    #note that you'll need to use network alias for machines like cluster-020
    hostnames = ["rh5-intel64-3", "rh5-intel64-13"]
    return hostnames

def constructRemoteCommand(user, host, command):
    #return "ssh " + user + "@" + host + " \'" + command + "\'"
    return command

def constructScpCommand(user, host, sourcePath, destinationPath):
    return "scp " + sourcePath + " " + user + "@" + host + ":" + destinationPath

def setupOptions():
    parser = OptionParser()
    parser.add_option("-n", "--nodefile", dest="nodefile", help="path to list of node hostnames")
    parser.add_option("-u", "--username", dest="username", help="username to execute remote ssh commands")
    #parser.add_option("-r", "--rpm", dest="rpm", help="path to MarkLogic installation .rpm")
    parser.add_option("-l", "--license", dest="licensefile", help="path to file containing licensee and license key strings")
    return parser

def main():
    hostnames = []
    parser = setupOptions()
    (options, args) = parser.parse_args()

    if options.nodefile == None:
        print "Missing list of node hostnames"
        return
    else:
        f = open(options.nodefile)
        for line in f:
            hostnames.append(line.strip())

    if options.username == None:
        print "Missing username to execute remote ssh commands"
        return
    else:
        USER = options.username

    #if options.rpm == None:
    #    print "Missing MarkLogic installation .rpm"
    #    return
    #else:
    #    RPM_PATH = os.path.abspath(options.rpm)
    #    print "rpm: " + RPM_PATH

    if options.licensefile == None:
        print "Missiing file containing licensee and license key strings"
        return
    else:
        f = open(options.licensefile)
        LICENSEE = f.readline().strip()
        LICENSE = f.readline().strip()

    #for each host in the cluster
    i = 0
    firsthost = ""
    fullyQualFirsthost = ""
    for h in hostnames:
    #for h in getStaticHostnames():
        fullyQualH = h
    



        print "set -x"
        command = constructRemoteCommand(USER, h, "curl http://localhost:8001/license-go.xqy?license-key=" + LICENSE + "&licensee=" + LICENSEE.replace(" ", "%20") + "&ok")
        command = command.replace("&", "\\&")
        print command
        #subprocess.call(command.split(" "))

        print "sleep 10"

        command = constructRemoteCommand(USER, h, "curl http://localhost:8001/agree-go.xqy?accepted-agreement=development")
        command = command.replace("&", "\\&")
        print command
        #subprocess.call(command.split(" "))

        print "sleep 10"

        command = constructRemoteCommand(USER, h, "curl http://localhost:8001/initialize-go.xqy")
        command = command.replace("&", "\\&")
        print command
        #subprocess.call(command.split(" "))

        print "sleep 10"

                                                                                              

if __name__ == '__main__':
    main()




