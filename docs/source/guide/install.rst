Install RELISON
==================

RELISON is a library working with the following operating systems:

* Linux
* Windows 10

It requires Java version 14 or later.

Install from source
~~~~~~~~~~~~~~~~~~~~

MAVEN
^^^^^^

The recommended way to use RELISON is through Maven. This will allow you to automatically
download all the necessary dependencies to work with this framework. There are two ways to
do this:

Maven Central
-------------
A first option allows to download the project from Maven Central. In order to add the library
to your project, just add the following dependency to the .pom file.

.. code:: xml

    <dependency>
      <groupId>io.github.ir-uam</groupId>
      <artifactId>RELISON-[module-name]</artifactId>
      <version>1.0.0</version>
    </dependency>

where [module-name] indicates the name of the module you want to import.

Manual installation
-------------------

In order to download the sources and install the library in your Maven repository, you just have to execute
the following command: 

.. code:: bash
	
	git clone https://github.com/JavierSanzCruza/RELISON
	cd RELISON
	mvn install

Afterwards, if you just want to use the main programs provided with the code, you have to 
compile the examples package. For this, do the following:

.. code:: bash

	cd Relison-examples
	mvn clean compile assembly:single

This will create an executable JAR which can be used to access any of the provided programs.
If, instead, you want to integrate a RELISON module in your project, you will need to add it as a
dependency as follows:

.. code:: xml

    <dependency>
      <groupId>io.github.ir-uam</groupId>
      <artifactId>RELISON-[module-name]</artifactId>
      <version>1.0.0</version>
    </dependency>

where [module-name] indicates the name of the module you want to import.

JAVA
^^^^^

In case you do not want to use Maven, you just need to compile the library into JAR files. In order 
to do that, the following libraries (and their dependencies) are needed:

* FastUtil v.8.5.2. (https://fastutil.di.unimi.it/)
* Matrix Toolkits Java v.1.0.4. (https://github.com/fommil/matrix-toolkits-java)
* RankSys v.0.4.3. (http://ranksys.github.io/)
	* RankSys-core
	* RankSys-fast
	* RankSys-formats
	* RankSys-rec
	* RankSys-nn
	* RankSys-novdiv
	* RankSys-novelty
	* RankSys-mf
* Jung v.2.1.1. (http://jung.sourceforge.net/)
* Apache Lucene v.8.4.1. (https://lucene.apache.org/)
	* Lucene-core
* Terrier v.5.1. (http://terrier.org/)
	* terrier-core
	* terrier-realtime
	* terrier-learning
* Weka v.3.6.6. (https://www.cs.waikato.ac.nz/ml/weka/)
* Cloning v.1.9.2. (https://mvnrepository.com/artifact/uk.com.robust-it/cloning/1.9.2)
* Yaml Beans v.1.06 (https://github.com/EsotericSoftware/yamlbeans)
* JUnit v. 4.13.1.
