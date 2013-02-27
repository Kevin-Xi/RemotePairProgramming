JAVAC = javac
JAVA = java

all:
	$(JAVAC) RemotePairProgramming.java

run:
	$(JAVA) RemotePairProgramming

clean:
	rm -rf *.class
