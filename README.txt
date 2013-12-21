Image manipulation programming language:
An academic project to write a compiler of a language that can manipulate images with single line such as rotating an image, changing colors, pixel by pixel manipulation. The language can be used to write complex programs to create images or even animations. The compiler is written in Java and uses ASM 4.2 library to generate Java Bytecode that can be run on stack based JVM.
All stages in compilation of a proram are included: 
1. Scanner - Lexical analysis
2. Parsing - building abstract syntax tree with Type checking.
3. Error handling with recovery from an error.
4. Bytecode generation - ASM 4.2 is used to generate bytecode of the program. 
The code generator and Type checker use visitor pattern to traverse the abstract syntax tree.

For extra details, see the desc folder for description of each part of the project.

How to run:
The command line input to java program Compiler.java is the name of text file that contains user program. output created is the class file in working directory of the compiler and the user proram is run immediately.