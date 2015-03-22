This is an [Ant](http://ant.apache.org/) filter to compute [Mustache](http://mustache.github.io/) templates based on [JMustache](https://github.com/samskivert/jmustache).

It is to be used in a [Filterchain](http://ant.apache.org/manual/Types/filterchain.html) - [TokenFilter](http://ant.apache.org/manual/Types/filterchain.html#tokenfilter) using the [FileTokenizer](http://ant.apache.org/manual/Types/filterchain.html#filetokenizer).

Installation
============

Download the JMustache jar file and store it somewhere accessible.
You can then define the mustache filter in your ant script as follows:

	<typedef name="mustache" classname="com.samskivert.mustache.ant.MustacheFilter">
		<classpath>
			<path location="${jmustache.jar}" />
		</classpath>
	</typedef>

Usage
=====

This filter must be used inside a filetokenizer tokenfilter as it needs to parse the whole file at once:

	<filterchain>
		<tokenfilter>
			<filetokenizer />
			<mustache />
		</tokenfilter>
	</filterchain>

As this is a filter, it can be used in any Ant task supporting filterchains, like
* Concat
* Copy
* LoadFile
* LoadProperties
* LoadResource
* Move

Parameters
==========

All parameters are optional.

| Parameter         | Description                                                                   | Default        |
|-------------------|-------------------------------------------------------------------------------|----------------|
| projectProperties | Boolean (true or false): should project properties be added to the data model | true           |
| prefix            | Only project properties starting with this prefix will be used                | No prefix used |
| removePrefix      | Boolean: should we remove the prefix (if specified) from the property name?   | false          |
| supportLists      | Boolean. Adds list support (see below)                                        | true           |
| listRegex         | The regex pattern to use to defined lists (see below)                   | (.+)\\.(\\d+)\\.(.+) |
| listIdName        | The name of the list id to be generated (see below)                           | \__id__         |
| dataFile          | A property file containing datamodel key and values                           | None           |
| defaultValue      | As JMustache defaultValue(), provides a default to non-defined keys | No default, fails on missing|
| strictSections    | As JMustache strictSections(), defines if section referring to a non-defined value should fail | false |
| escapeHTML        | As JMustache escapeHTML(), defines if outputed HTML should be escaped         | false          |

Lists support
=============

Provided property names can be parsed to generate lists. The default Regexp pattern for such property is

	(.+)\\.(\\d+)\\.(.+)

This pattern means that any property containing a number between two dots would be translated into a list.
The list name is the first part.
The id in the list is the number. It can be accessed using the value of listIdName ("\__id__" by default).
The remaining part is then used as a key inside the list.

An example may help here. Consider the following properties:

	mylist.01.prop1 = value-1-1
	mylist.01.prop2 = value-1-2
	mylist.02.prop1 = value-2-1
	mylist.02.prop2 = value-2-2
	
And this template

	mylist = {{mylist}}
	{{#mylist}}
	{{__id__}}.prop1 = {{prop1}}
	{{__id__}}.prop2 = {{prop2}}
	{{/mylist}}
	
The output would be:

	mylist = [{prop2=value-1-2, prop1=value-1-1, __id__=01}, {prop2=value-2-2, prop1=value-2-1, __id__=02}]
	01.prop1 = value-1-1
	01.prop2 = value-1-2
	02.prop1 = value-2-1
	02.prop2 = value-2-2
   
Note that you can override the default pattern. For example, you may prefer to use a notation with square brackets:

	listRegex="(.+)\[(\d+)\]\.(.+)"

With such regex, the previous list would be written

	mylist[01].prop1 = value 01-1
	mylist[01].prop2 = value 01-2
	mylist[02].prop1 = value 02-1
	mylist[02].prop2 = value 02-2

Boolean values
==============

As the string "false" is not usually considered as actually False, a special treatment is needed for booleans.
Properties ending by a question mark are treated as Booleans, specifically to be used as tests inside the templates.

	mytrue? = true
	myfalse? = false
	
In the template:

	mytrue? = {{mytrue?}}
	{{#mytrue?}}
	mytrue is valid (not false nor empty list), showing this!
	{{/mytrue?}}
	{{^mytrue?}}
	mytrue is NOT valid (false or empty list), showing that!
	{{/mytrue?}}
	
	myfalse? = {{myfalse?}}
	{{#myfalse?}}
	myfalse is valid (not false nor empty list), showing this!
	{{/myfalse?}}
	{{^myfalse?}}
	myfalse is NOT valid (false or empty list), showing that!
	{{/myfalse?}}

Which outputs:

	mytrue? = true
	mytrue? is valid (not false nor empty list), showing this!
	myfalse? = false
	myfalse? is NOT valid (false or empty list), showing that!


