#!/usr/bin/python
# -*- coding: utf-8 -*-

from random import randint

def randNumber(maxValue):
    return (randint(1, maxValue))

def getHead(head):
    a = ['@ud','@d']
    return a[head]

def randBodyA():
    a = ["if(a == true){","while(b > 5){", "for(x : d){"]
    return a[randNumber(len(a))-1]

def randBodyB():
    a = ['  b = 3;', '  f = 4;']
    return a[randNumber(len(a))-1]

def printResult(head):
    print(getHead(head))
    print(randBodyA())
    print(randBodyB())
    print('}')

def printLines():
    n1 = randNumber(2)-1
    n2 = randNumber(4)
    n3 = randNumber(3)-1
    print("["+str(n1+n3)+","+str(n2)+"]")
    for x in range(1, n1+1):
        printResult(0)
    for x in range(1, n2+1):
        printResult(1)
    for x in range(1, n3+1):
        printResult(0)

printLines()
