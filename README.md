ScalaJavaMapper
===============

Mapper to convert Scala case classes from/to Java mutable classes (i.e. with getters and setters for the properties). 
A scala converter type class can be generated via a macro to create a Converter for each combination of case class 
and Java class. It is expected and validated that all the case class fields can be mapped to the Java object, there 
is no such expectation or validation the other way around.
