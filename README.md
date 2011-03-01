This is a Java implementation of the [Mustache template
language](http://mustache.github.com/). There exists [another Java
implementation of Mustache](http://github.com/spullara/mustache.java), but the
motivations for this version are sufficiently different as to justify (in the
author's mind, anyhow) the duplication.

Motivations
===========

  * Zero dependencies. You can include this single tiny library in your project
    and start making use of templates.
  * Usability on a variety of target platforms. The other Java Mustache
    implementation requires that a Java compiler be available to compile
    templates into Java classes. This implementation makes no such requirements
    and as a result is usable on Android, or other exciting places where a Java
    compiler is not available. It is even possible to avoid the use of
    reflection and provide all of your data as a series of nested Maps, if
    desired.

  * [Proguard](http://proguard.sourceforge.net/) and
    [JarJar](http://code.google.com/p/jarjar/) friendly. Though the library
    will reflectively access your data (if you desire it), the library makes no
    other internal use of reflection or by name instantiation of classes. So
    you can embed it using Proguard or JarJar without any annoying surprises.

  * Minimal API footprint. There are really only two methods you need to know
    about: `compile` and `execute`. You can even chain them together in cases
    where performance is of no consequence.

Its existence justified by the above motivations, this implementation then
strives to provide additional benefits:

  * It is available via Maven Central, see below for details.
  * It is reasonably performant. Templates are parsed separately from
    execution. A template will specialize its variables on (class of context,
    name) pairs so that if a variable is first resolved to be (for example) a
    field of the context object, that will be attempted directly on subsequent
    template invocations, and the slower full resolution will only be tried if
    accessing the variable as a field fails.

Get It
======

JMustache is available via Maven Central and can thus be easily added to your
Maven, Ivy, etc. projects by adding a dependency on
`com.samskivert:jmustache:1.0`. Or download the [pre-built jar
file](http://repo1.maven.org/maven2/com/samskivert/jmustache/1.0/jmustache-1.0.jar).

Usage
=====

Using JMustache is very simple. Supply your template as a `String` or a
`Reader` and get back a `Template` that you can execute on any context:

    String text = "One, two, {{three}}. Three sir!";
    Template tmpl = Mustache.compiler().compile(text);
    Map<String, String> data = new HashMap<String, String>();
    data.put("three", "five");
    System.out.println(tmpl.execute(data));
    // result: "One, two, five. Three sir!"

Use `Reader` and `Writer` if you're doing something more serious:

    void executeTemplate (Reader template, Writer out, Map<String, String> data) {
       Mustache.compiler().compile(template).execute(data, out);
    }

The execution context can be any Java object. Variables will be resolved via
the following mechanisms:

  * If the context is a `Map`, `Map.get` will be used.
  * If a non-void method with the same name as the variable exists, it will be called.
  * If a non-void method named (for variable `foo`) `getFoo` exists, it will be called.
  * If a field with the same name as the variable exists, its contents will be used.

Example:

    class Person {
        public final String name;
        public Person (String name, int age) {
            this.name = name;
            _age = age;
        }
        public int getAge () {
            return _age;
        }
        protected int _age;
    }

    String tmpl = "{{#persons}}{{name}}: {{age}}{{/persons}}\n";
    Mustache.compiler().compile(tmpl).execute(new Object() {
        Object persons = Arrays.asList(new Person("Elvis", 75), new Person("Madonna", 52));
    });

    // result:
    // Elvis: 75
    // Madonna: 52

As you can see from the example, the fields (and methods) need not be public.
The `persons` field in the anonymous class created to act as a context is
accessible. Note that the use of non-public fields will not work in a sandboxed
security environment.

Sections behave as you would expect:

 * `Boolean` values enable or disable the section.
 * Array, `Iterator`, or `Iterable` values repeatedly execute the section with each element used as the context for each iteration. Empty collections result in zero instances of the section being included in the template.
 * Any other object results in a single execution of the section with that object as a context.

See the code in
[MustacheTest.java](http://github.com/samskivert/jmustache/blob/master/src/test/java/com/samskivert/mustache/MustacheTest.java)
for concrete examples. See also the [Mustache
documentation](http://mustache.github.com/mustache.5.html) for details on the
template syntax.

Extensions
==========

JMustache extends the basic Mustache template language with some additional
functionality. These additional features are enumerated below:

Not escaping HTML by default
----------------------------

You can change the default HTML escaping behavior when obtaining a compiler:

    Mustache.compiler().escapeHTML(false).compile("{{foo}}").execute(new Object() {
        String foo = "<bar>";
    });
    // result: <bar>
    // not: &lt;bar&gt;

Special variables
-----------------

### this
You can use the special variable `this` to refer to the context object itself
instead of one of its members. This is particularly useful when iterating over
lists.

    Mustache.compiler().compile("{{this}}").execute("hello"); // returns: hello
    Mustache.compiler().compile("{{#names}}{{this}}{/names}}").execute(new Object() {
        List<String> names () { return Arrays.asList("Tom", "Dick", "Harry"); }
    });
    // result: TomDickHarry

### -first and -last
You can use the special variables `-first` and `-last` to perform special
processing for list elements. `-first` resolves to `true` when inside a section
that is processing the first of a list of elements. It resolves to `false` at
all other times. `-last` resolves to `true` when inside a section that is
processing the last of a list of elements. It resolves to `false` at all other
times.

One will often make use of these special variables in an inverted section, as
follows:

    String tmpl = "{{#things}}{{^-first}}, {{/-first}}{{self}}{{/things}}";
    Mustache.compiler().compile(tmpl).execute(new Object() {
        List<String> things = Arrays.asList("one", "two", "three");
    });
    // result: one, two, three

Note that the values of `-first` and `-last` refer only to the inner-most
enclosing section. If you are processing a section within a section, there is
no way to find out whether you are in the first or last iteration of an outer
section.

### -index
The `-index` special variable contains 1 for the first iteration through a
section, 2 for the second, 3 for the third and so forth. It contains 0 at all
other times. Note that it also contains 0 for a section that is populated by a
singleton value rather than a list.

    String tmpl = "My favorite things:\n{{#things}}{{-index}}. {{self}}\n{{/things}}";
    Mustache.compiler().compile(tmpl).execute(new Object() {
        List<String> things = Arrays.asList("Peanut butter", "Pen spinning", "Handstands");
    });
    // result:
    // My favorite things:
    // 1. Peanut butter
    // 2. Pen spinning
    // 3. Handstands

Compound variables
------------------

In addition to resolving simple variables using the context, you can use
compound variables to extract data from sub-objects of the current context. For
example:

    Mustache.compiler().compile("Hello {{field.who}}!").execute(new Object() {
        public Object field = new Object() {
            public String who () { return "world"; }
        }
    });
    // result: Hello world!

By taking advantage of reflection and bean-property-style lookups, you can do kooky things:

    Mustache.compiler().compile("Hello {{this.class.name}}!").execute(new Object());
    // result: Hello java.lang.Object!

Newline trimming
----------------

Newlines immediately following the opening or closing section tag are trimmed.
This allows for civilized templates, like:

    Favorite foods:
    {{#people}}
    - {{first_name}} {{last_name}} likes {{favorite_food}}.
    {{/people}}

which produces output like:

    Favorite foods:
    - Elvis Presley likes peanut butter.
    - Mahatma Gandhi likes aloo dum.

rather than:

    Favorite foods:

    - Elvis Presley likes peanut butter.

    - Mahatma Gandhi likes aloo dum.


which would be produced without the newline trimming. Note: the current
implementation does not handle Windows-style CRLF data. If you're a Windows
user, how about sending me a patch?

Nested Contexts
---------------

If a variable is not found in a nested context, it is resolved in the next
outer context. This allows usage like the following:

    String template = "{{outer}}:\n{{#inner}}{{outer}}.{{this}}\n{{/inner}}";
    Mustache.compiler().compile(template).execute(new Object() {
        String outer = "foo";
        List<String> inner = Arrays.asList("bar", "baz", "bif");
    });
    // results:
    // foo:
    // foo.bar
    // foo.baz
    // foo.bif

Note that if a variable _is_ defined in an inner context, it shadows the same
name in the outer context. There is presently no way to access the variable
from the outer context.

Limitations
===========

This version of Mustache is intended for use in non-webapp scenarios. In the
name of simplicity, some features of Mustache were omitted or simplified:

  * `{{< include}}` is not supported. JMustache does not presume to know from whence your templates come, nor to foist upon you a template loading scheme. You give JMustache a `String` or `Reader` and it gives back an executable template.
  * `{{= =}}` only supports one or two character delimiters. This is just because I'm lazy and it simplifies the parser.
