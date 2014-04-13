SRCS=MiniParser.java MiniLexer.java TypeCheck.java
FILES=$(addprefix src/, $(SRCS))
CLASSPATH=lib/antlr-3.3-complete.jar

bin/Mini.class : src/*.java gen/antlr.generated
	javac -classpath $(CLASSPATH) -d bin/ src/*.java

gen/antlr.generated: gen/antlr.generated.mini gen/antlr.generated.type
	touch gen/antlr.generated

gen/antlr.generated.mini : src/Mini.g
	java -classpath $(CLASSPATH) org.antlr.Tool $^
	mv Mini.tokens gen/
	touch gen/antlr.generated.mini

gen/antlr.generated.type : src/TypeCheck.g
	java -classpath $(CLASSPATH) org.antlr.Tool -lib gen/ $^
	mv TypeCheck.tokens gen/
	touch gen/antlr.generated.type

clean:
	rm -f $(FILES)
	rm -f bin/*.class
	rm -f gen/*
