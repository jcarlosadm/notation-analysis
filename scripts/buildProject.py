#!/usr/bin/python
# -*- coding: utf-8 -*-

import os
import subprocess
from git import Repo

parentPath = os.path.abspath("..")
libDirectory = os.path.abspath(parentPath+"/libs/")
srcmlDirectory = os.path.abspath(parentPath+"/srcML/")
gitCommitStatisticsDir = os.path.abspath(libDirectory+"/git-commit-statistics/")
gitCommitStatisticsRepo = "https://github.com/jcarlosadm/git-commit-statistics"
gitCommitStatisticsPomPath = os.path.abspath(gitCommitStatisticsDir+"/pom.xml")
generalPropertiesPath = os.path.abspath(parentPath+"/general.properties")
generalPropertiesString = ("path = results\n"
                            "path.repolist = REPOS\n"
                            "path.src2srcml = srcML/src2srcml\n"
                            "path.srcml2src = srcML/srcml2src\n"
                            "path.dmacros = scripts/dmacros.py\n"
                            "number.of.workers = 5\n"
                            "number.of.interns = 3\n"
                            "comparator.tolerance.level = 0.5\n"
                            "\n"
                            "# debug\n"
                            "recreate.backup.folder = false\n"
                            "reuse.dmacros.data = true")
repolistPath = os.path.abspath(parentPath+"/REPOS")
repolistString = "https://github.com/GNOME/dia"
repolistOldPath = os.path.abspath(parentPath+"/REPOS.old")
repolistOldString = ("https://github.com/GNOME/dia\n"
                        "https://github.com/psas/av3-fc\n"
                        "https://github.com/glennrp/libpng\n"
                        "https://github.com/Cronus-Emulator/Cronus\n"
                        "https://github.com/credosemi/smartsnmp\n"
                        "https://github.com/p4lang/p4factory\n"
                        "https://github.com/glennrp/libpng\n")

def runMaven(pomPath, command):
    args = ['mvn',command,'-f',pomPath]
    process = subprocess.Popen(args, stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    output, errors = process.communicate()
    print output
    print errors

def makeDir(directory):
    if not os.path.exists(directory):
        os.makedirs(directory)

def makeFile(filepath, string):
    if not os.path.exists(filepath):
        file = open(filepath, "wb")
        file.write(string)
        file.close()

makeDir(libDirectory)
makeDir(gitCommitStatisticsDir)

if not os.path.isdir(os.path.abspath(gitCommitStatisticsDir+"/.git/")):
    Repo.clone_from(gitCommitStatisticsRepo, gitCommitStatisticsDir)

runMaven(gitCommitStatisticsPomPath, 'compile')
runMaven(gitCommitStatisticsPomPath, 'install')

makeDir(srcmlDirectory)

makeFile(generalPropertiesPath, generalPropertiesString)
makeFile(repolistPath, repolistString)
makeFile(repolistOldPath, repolistOldString)
