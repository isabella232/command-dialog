# Cytoscape Command Dialog Scripting enhancements

## Introduction

Cytoscape is a general purpose, open source software platform for complex network data integration, analysis and visualization. Cytoscape’s core implements generic network integration and visualization capabilities through its core apps and can be extended to suite specific use-cases by developing Cystoscape apps using its extensible plugin architecture.  

While Cytoscape’s graphical user interface is very useful for interactive network data integration and visualization, it may not be the best choice for workflows with hundreds if not thousands of network data files. Cytoscape includes the commandTool app to provide a command-line interface to Cytoscape using the Commands API. Cytoscape apps can register commands using annotations, and these commands will then be available in commandTool. Users can execute commands (eg importing networks from file or importing tables) using Command Line Dialog and can see the result in the Reply Log. The commandTool can also execute scripts (each line is a set of commands) and provide help about command classes and namespaces using the Command API.


## How to build

```bash
git clone https://github.com/cytoscape/command-dialog.git
mvn clean install
```
After the project build is complete, please install the newly generated command-dialog<version>-SNAPSHOT.jar into Cytoscape.

## Design 
![alt tag](https://github.com/ashishtiwarigsoc/command-dialog/blob/develop/Command-Dialog%20interactions.jpg)

## Sample script with variables, conditions and looping  
```
$var1 := 1
$nodeNamePrefix := "node_"

while $var1 < 10 loop
	command echo variableName=var1
	$nodeName := $nodeNamePrefix + $var1
	network add node name=$nodeName network=current
	$var1 := $var1 + 1
end while

if $var1 == 10 then
	command echo variableName="*"
else 
	command echo variableName=var1
end if

```
1. Custom variables are defined by using $ before the variable name
2. := is used as assignment operator
3. Nesting of loops and conditions is not permitted in the current version. We are working on adding those abilities
4. Conditions can be built similar to any programming language; using arithmatic, logical, increment/decrement and relational operators.
5. We have added a new command ``echo`` to display value of any variable.
6. Variables can also contain the result of any cytoscape command. The type of the variable will be determined by the value that it contains. Eg.  
  ``currentNetwork := network get network=current``
