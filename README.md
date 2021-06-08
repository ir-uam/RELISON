![Java CI with Maven](https://github.com/ir-uam/RELISON/workflows/Java%20CI%20with%20Maven/badge.svg)
[![GitHub license](https://img.shields.io/badge/license-MPL--2.0-orange)](https://www.mozilla.org/en-US/MPL/)

# RELISON

Relison is a Java library devised for the implementation and evaluation of different social analysis and network mining techniques.
This framework has been created as the result of the research documented in several publications on contact recommendation and
 link prediction in social networks (which we refer **here**) and a [PhD thesis](http://javiersanzcruza.github.io/jsanzcruzado-phdthesis.pdf). 
 In addition to contact recommendation and link prediction functionalities, Relison can be used to perform multiple
 social network analysis and network science tasks: analyzing structural properties of social networks (the library includes a wide
 variety of metrics, from the most classical metrics to novel ones targeting the structural diversity of the network),
 community detection, functionalities for indexing user-generated contents, and for performing information
 diffusion simulations over social networks.
 
## Introduction
RELISON is a new Java framework for the implementation and evaluation of techniques for social network analysis and mining. These framework has been created as the result of the research documented in several publications on contact recommendation in social networks (see here) and a . But, in addition to those contact recommendation and link prediction functionalities, SoNALiRe can be used to perform multiple social network analysis and network science tasks: analyzing structural properties of social networks (including the most classical metrics, along with several new ones, targeting the structural diversity of the network), community detection and performing information diffusion simulations over social networks. 

The framework has been programmed targetting the Java 14 version, one of the most recent versions of the popular programming language. Built upon the [RankSys library](http://ranksys.github.io), we take advantage of the many features provided by the last versions of Java. The code is licensed under the [MPL 2.0](https://www.mozilla.org/en-US/MPL/2.0/).

## Packages
Up to date, the following packages have been published:
- **Relison-core:** Basic graph definitions and generators.
- **Relison-sna:** Social network analysis metrics and community detection.
- **Relison-content:** Classes and definitions for user-generated contents in social networks.
- **Relison-linkpred:** Link prediction and contact recommendation functionalities.
- **Relison-diffusion:** Simulation of information diffusion dynamics.
- **Relison-examples:** Examples showcasing the functionality of the library.

## System Requirements
- **Java JDK:** Java 14 or above.
- **Maven:** Tested with version 3.6.0.
- **R:** Tested with version 3.6.3.
- **GLIBC:** In Linux, to execute path-based approaches, this library uses JBLAS 1.2.5, which requires
  GLIBC version.
- **Matlab:** For some matrix-based approaches, the fastest version is coded in Matlab. It has been tested with version R2020a 

## Instalation
In order to install this framework, you need to have Maven [http://maven.apache.org](http://maven.apache.org) installed 
on your system. Then, download the files into a directory. If you want to obtain a .jar containing all the packages,
go to the Relison-examples directory and execute the following command:
```
mvn compile assembly::single
```

If you just want to install and add all the packages to your Maven library, in the main directory, execute the command:
```
mvn install
```

Once it is installed in your Maven library, you can import the package you are interested to use in your project 
adding the following dependence.
```
    <artifactId>Relison-[package-name]</artifactId>
    <groupId>es.uam.eps.ir</groupId>
    <version>1.0-SNAPSHOT</version>
```
We plan to add the library to Maven Central in the future, but, as of now, it is not available there. 

In case you do not want to use Maven, it is still possible to compile the code using any Java compiler.
In this case, you will need the following libraries:
- RankSys v0.4.3.: [http://ranksys.github.io](http://ranksys.github.io)
- Fastutils v8.5.2.: [http://fastutil.di.unimi.it/](http://fastutil.di.unimi.it/)
- Colt version v1.2.0: [https://dst.lbl.gov/ACSSoftware/colt](https://dst.lbl.gov/ACSSoftware/colt)
- Google MTJ version v1.0.4: [https://github.com/fommil/matrix-toolkits-java](https://github.com/fommil/matrix-toolkits-java)
- Jung v2.1.1.: [http://jung.sourceforge.net/](http://jung.sourceforge.net/) (Obtained from MavenCentral)
- Apache Lucene v8.4.1.: [https://lucene.apache.org/](https://lucene.apache.org/)
- JUnit v4.13.1.: [https://junit.org/junit4/](https://junit.org/junit4/)
- Apache Commons CSV v1.5: [https://commons.apache.org/proper/commons-csv/](https://commons.apache.org/proper/commons-csv/)
- Terrier v5.1: [http://terrier.org/](http://terrier.org/)
- Weka v3.6.6.: [https://www.cs.waikato.ac.nz/ml/weka/](https://www.cs.waikato.ac.nz/ml/weka/)
- Cloning v1.9.2.: [https://github.com/kostaskougios/cloning](https://github.com/kostaskougios/cloning)
- JBLAS v.1.2.5.: [http://jblas.org/](http://jblas.org/)
## More information
