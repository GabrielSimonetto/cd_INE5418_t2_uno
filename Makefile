.PHONY: all
all: rebuild run

.PHONY: build
build:
	javac Carta.java Jogador.java
	javac -d . Carta.java Jogador.java

.PHONY: rebuild
rebuild: clean build

.PHONY: clean
clean:
	rm *.class
	rm -rf uno

run:
	java -classpath uno:.:jgroups-5.0.0.Final.jar Jogador.java
