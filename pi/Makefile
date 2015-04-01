SRC = ./src
BIN = ./bin
JC = javac
JFLAGS = -d $(BIN)
	
#Add source files here. Make sure to prepend with $(SRC)
CLASSES = \
	$(SRC)/BugDetection.java

CLASS_FILES=$(CLASSES:$(SRC)/%.java=$(BIN)/%.class)
	
all: classes

classes:
	mkdir -p $(BIN)
	$(JC) $(JFLAGS) $(CLASSES) 
	
clean:
	$(RM) $(BIN)/*.class
	
run:
	./pipair