all: app

clean:
	\rm -f *.class

app:
	$(info Compiling classes. Run with 'java Standalone [image]'...)
	javac *.java
