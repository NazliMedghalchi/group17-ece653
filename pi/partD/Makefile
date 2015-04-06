BIN = ./bin
JC = javac
JFLAGS = -d $(BIN)
	
CLASSES = \
	BugDetectionD.java

CLASS_FILES=$(CLASSES:%.java=$(BIN)/%.class)
	
all: classes

classes:
	mkdir -p $(BIN)
	chmod +x pipair
	$(JC) $(JFLAGS) $(CLASSES) 
	
clean:
	rm $(BIN)/*.class
	
run:
	./pipair
