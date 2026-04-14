# imagej-plugin-template
Simple project template for creating ImageJ Plugins.

The aim is to provide a quick way to start a plugin for ImageJ, using the "legacy" plugin style. 

The project is based on maven. It uses sci-java as parent configuration. The parent configuration 
is somewhat old (1.126), but I encountered configuration troubles with more recent ones. 

The base configuration has few dependencies:

* ImageJ
* JUnit

## Installation


Follow the steps:

1. Use the "Use this template" button to create a new project within GitHub. Give it the name your choice.
2. Clone the new project into the directory of your choice. 
3. Edit the `pom.xml` file with the information specific to the project. 
In particular:
    - the name of the project is used by the Eclipse IDE to index the project
    - the `artifactId` tag is used to generate the name of the jar file
5. You can start changing code and committing changes.

A sample plugin file is provided in `src/main/java/net.ijt/DemoPlugin.java` file.
The plugin configuration file is in `src/main/resources/plugins.config`.
