#!/bin/bash

#Java executable for standard Linux environment
export JAVA_EXE=java
#Java executable for MinGW environment
#export JAVA_EXE=/c/jdk9/bin/java.exe

$JAVA_EXE -jar ./target/benchmarks.jar ParallelStreamBenchmark
