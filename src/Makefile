JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $*.java

CLASSES = \
	BugDetection.java \

default: classes

classes: $(CLASSES:.java=.class)

clean:
	-rm -f *.class
	-rm -f callgraph.txt
