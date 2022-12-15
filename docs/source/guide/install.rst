Install RELISON
==================

RELISON is a library working with the following operating systems:

* Linux
* Windows 10

It requires Java version 14 or later.

Install from source
~~~~~~~~~~~~~~~~~~~~

There are many ways you can install and use RELISON on your system. We provide information here on
how to do this:

* `Executable`_
* `Docker`_
* `Maven`_
* `Java`_

Executable
^^^^^^^^^^
If you want to use the default command line programs, we recommend to download the JAR file containing all the dependencies.
This file is available from the following link:

https://github.com/ir-uam/RELISON/releases/download/v1.0.0-maven/relison.jar

You can download it with the following command:

.. code:: bash

    curl –L https://github.com/ir-uam/RELISON/releases/download/v1.0.0-maven/relison.jar --output relison.jar

Then, you can use any of the available programs by executing the following line on your terminal:

.. code:: bash

    java [VM_OPTIONS] -jar relison.jar PROGRAM_CODE [arguments]
    
where

- [VM_OPTIONS] represent the list of configuration parameters for the Java virtual machine.
- PROGRAM_CODE is the name of the program to execute.
- [arguments] are the list of arguments of program.


Docker
^^^^^^
A Dockerized version of the command line RELISON program is available on Docker Hub. In order to run a container,
execute the following two docker commands:

.. code:: bash

    docker pull javiersanzcruza/relison:latest
    docker run -p 8888:8888 javiersanzcruza/relison:latest

This will create a Docker container with a Jupyter notebook acessible from http://localhost:8888.
The executable .jar for RELISON will be available on the /tmp/notebooks/RELISON/ directory.

Maven
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

Java
^^^^^

In case you do not want to use Maven, you just need to compile the library into JAR files, or obtain them from Maven Central: https://search.maven.org/search?q=RELISON%20io.github.ir-uam

In addition, you might need to obtain the following libraries (and their dependencies):

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
