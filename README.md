MiniCompiler
============

Compiler for the Mini Language

Build Instructions
==================

The program comes packaged with a Makefile. To build the compiler just run `make`. This will compile all of the java classes into the `bin` folder.

Running the Compiler
====================

There is an included file `run.sh` which will properly call the java program with the correct classpath. You pass it the path to the .mini source file. Assembly files are saved to the `asm` folder. ILOC files are saved to the `iloc` folder. Control Flow Graphs for each compiled functions are saved as a .dot file in the `dot` folder.

Usage: `./run.sh [-no_out] [options] sourceFile [options]`
* `-no_out` : Hides debug developer messages during the compile process
* `dumpIL` : Saves the ILOC instructions to a file
* `-keepDead` : Doesn't remove "dead" instructions
* `-noCopyProp` : Doesn't perform copy propogation
* `-preserveConst` : Loads all constants into registers, rather than performing constant folding and constant propogation
* `-spillAll` : Spills all values to memory

This will produce a .s file in the `asm` folder that you can then use `gcc` to compile into an executable.

Currently the only tested options for the benchmarks are to keep all of the optimizations enabled.

Testing Benchmarks
==================

There is an included file that will test the benchmarks.

Usage: `./testBenchmarks.sh [options]`
* `-q` : Run all the benchmarks with the `-no_out` options
* `-s` : Run with `no_out` option and also hide all compiler warnings
* `-t` : Time the run time of the compiler generate code, as well as the run time for gcc optimizations levels -O0 through -O3

All benchmarks are currently passing.
