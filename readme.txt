jfvm
====

JFVM (JavaForce Virtual Machine) is an AOT Java Compiler featuring:
 - ARC (Automatic Reference Counting) instead of a GC (Garbage Collector)
   - predictable real-time performance - no more stop-the-world
   - programmer must safe guard against circular references
 - compiles java bytecode to c code (requires gcc 4.7+)
   - should run on any platform : windows, linux, mac, android, iphone
 - includes bare-bones classpath
 - not compatible with JNI

TODO:
 - improve on classpath
 - optimize bytecode compiler
 - support more debugging info
 - support annotations
 - compiler tweaking with annotations

Install:
  - you need to define one environment variable
    jfvm = install_path
      Windows : set jfvm=%cd%
      Linux : export jfvm=`pwd`

Compiling:
  - cd compiler
  - ant jar
  - cd vm
  - ant win32|win64|lnx32|lnx64
  - cd classpath
  - ant win32|win64|lnx32|lnx64

Notes:
 - currently the classpath supports Threads, Strings and System.out.println()
 - objects have a vtable similar to C++
 - most bytecode conversion is complete except
   - invokedynamic (for lambda expressions) are a little hack-ish since I don't quite understand them (they do work)
     but anything else that may use invokedynamic will not work
 - exception handling is working
 - thread sync is working
 - compiling the compiler itself to native code is not working yet (soon)

Future:
 - I only plan to support basic file I/O and Sockets.  No plans to implement nio, AWT/Swing, etc.

See docs/help.txt for more info.

http://github.com/pquiring/jfvm

Version 0.1

Nov 22, 2016

Peter Quiring
pquiring@gmail.com
