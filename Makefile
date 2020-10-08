all:			AtomicMulticast.class

AtomicMulticast.class:	AtomicMulticast.java
			@javac AtomicMulticast.java

clean:
			@rm -rf *.class *~
