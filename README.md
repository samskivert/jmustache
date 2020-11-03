This is a Java implementation of the [Mustache template language](http://mustache.github.com/).

[![Build Status](https://travis-ci.org/samskivert/jmustache.svg?branch=master)](https://travis-ci.org/samskivert/jmustache)

Motivations
===========

  * Zero dependencies. You can include this single tiny library in your project and start making
    use of templates.
  * Usability on a variety of target platforms. This implementation makes very limited demands on
    the JVM in which it runs and as a result is usable on Android, or on other limited JVMs. It is
    even possible to avoid the use of reflection and provide all of your data as a series of nested
    maps.

  * [Proguard](http://proguard.sourceforge.net/) and [JarJar](http://code.google.com/p/jarjar/)
    friendly. Though the library will reflectively access your data (if you desire it), the library
    makes no other internal use of reflection or by name instantiation of classes. So you can embed
    it using Proguard or JarJar without any annoying surprises.

  * Minimal API footprint. There are really only two methods you need to know about: `compile` and
    `execute`. You can even chain them together in cases where performance is of no consequence.

Its existence justified by the above motivations, this implementation then strives to provide
additional benefits:

  * It is available via Maven Central, see below for details.
  * It is reasonably performant. Templates are parsed separately from execution. A template will
    specialize its variables on (class of context, name) pairs so that if a variable is first
    resolved to be (for example) a field of the context object, that will be attempted directly on
    subsequent template invocations, and the slower full resolution will only be tried if accessing
    the variable as a field fails.

Get It
======

JMustache is available via Maven Central and can thus be easily added to your Maven, Ivy, etc.
projects by adding a dependency on `com.samskivert:jmustache:1.15`. Or download the pre-built
[jar file](https://repo1.maven.org/maven2/com/samskivert/jmustache/1.15/jmustache-1.15.jar).

Documentation
=============

In addition to the usage section below, the following documentation may be useful:

  * [API docs](http://samskivert.github.io/jmustache/apidocs/)
  * [Mustache manual](http://mustache.github.io/mustache.5.html)

Usage
=====

Using JMustache is very simple. Supply your template as a `String` or a `Reader` and get back a
`Template` that you can execute on any context:

```java
String text = "One, two, {{three}}. Three sir!";
Template tmpl = Mustache.compiler().compile(text);
Map<String, String> data = new HashMap<String, String>();
data.put("three", "five");
System.out.println(tmpl.execute(data));
// result: "One, two, five. Three sir!"
```

Use `Reader` and `Writer` if you're doing something more serious:

```java
void executeTemplate (Reader template, Writer out, Map<String, String> data) {
   Mustache.compiler().compile(template).execute(data, out);
}
```

The execution context can be any Java object. Variables will be resolved via the following
mechanisms:

  * If the context is a `MustacheCustomContext`, `MustacheCustomContext.get` will be used.
  * If the context is a `Map`, `Map.get` will be used.
  * If a non-void method with the same name as the variable exists, it will be called.
  * If a non-void method named (for variable `foo`) `getFoo` exists, it will be called.
  * If a field with the same name as the variable exists, its contents will be used.

Example:

```java
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
```

As you can see from the example, the fields (and methods) need not be public. The `persons` field
in the anonymous class created to act as a context is accessible. Note that the use of non-public
fields will not work in a sandboxed security environment.

Sections behave as you would expect:

 * `Boolean` values enable or disable the section.
 * Array, `Iterator`, or `Iterable` values repeatedly execute the section with each element used as
   the context for each iteration. Empty collections result in zero instances of the section being
   included in the template.
 * An unresolvable or null value is treated as false. This behavior can be changed by using
   `strictSections()`. See _Default Values_ for more details.
 * Any other object results in a single execution of the section with that object as a context.

See the code in
[MustacheTest.java](http://github.com/samskivert/jmustache/blob/master/src/test/java/com/samskivert/mustache/MustacheTest.java)
for concrete examples. See also the
[Mustache documentation](http://mustache.github.com/mustache.5.html) for details on the template
syntax.

Partials
--------

If you wish to make use of partials (e.g. `{{>subtmpl}}`) you must provide a
`Mustache.TemplateLoader` to the compiler when creating it. For example:

```java
final File templateDir = ...;
Mustache.Compiler c = Mustache.compiler().withLoader(new Mustache.TemplateLoader() {
    public Reader getTemplate (String name) {
        return new FileReader(new File(templateDir, name));
    }
});
String tmpl = "...{{>subtmpl}}...";
c.compile(tmpl).execute();
```

The above snippet will load `new File(templateDir, "subtmpl")` when compiling the template.

Lambdas
-------

JMustache implements lambdas by passing you a `Template.Fragment` instance which you can use to
execute the fragment of the template that was passed to the lambda. You can decorate the results of
the fragment execution, as shown in the standard Mustache documentation on lambdas:

```java
String tmpl = "{{#bold}}{{name}} is awesome.{{/bold}}";
Mustache.compiler().compile(tmpl).execute(new Object() {
   String name = "Willy";
   Mustache.Lambda bold = new Mustache.Lambda() {
        public void execute (Template.Fragment frag, Writer out) throws IOException {
            out.write("<b>");
            frag.execute(out);
            out.write("</b>");
        }
    };
});
// result:
<b>Willy is awesome.</b>
```

You can also obtain the results of the fragment execution to do things like internationalization or
caching:

```java
Object ctx = new Object() {
    Mustache.Lambda i18n = new Mustache.Lambda() {
         public void execute (Template.Fragment frag, Writer out) throws IOException {
             String key = frag.execute();
             String text = // look up key in i18n system
             out.write(text);
         }
    };
};
// template might look something like:
<h2>{{#i18n}}title{{/i18n}</h2>
{{#i18n}}welcome_msg{{/i18n}}
```

There is also limited support for decompiling (unexecuting) the template and obtaining the original
Mustache template text contained in the section. See the documentation for [Template.Fragment] for
details on the limitations.

Default Values
--------------

By default, an exception will be thrown any time a variable cannot be resolved, or resolves to null
(except for sections, see below). You can change this behavior in two ways. If you want to provide a
value for use in all such circumstances, use `defaultValue()`:

```java
String tmpl = "{{exists}} {{nullValued}} {{doesNotExist}}?";
Mustache.compiler().defaultValue("what").compile(tmpl).execute(new Object() {
    String exists = "Say";
    String nullValued = null;
    // String doesNotExist
});
// result:
Say what what?
```

If you only wish to provide a default value for variables that resolve to null, and wish to
preserve exceptions in cases where variables cannot be resolved, use `nullValue()`:

```java
String tmpl = "{{exists}} {{nullValued}} {{doesNotExist}}?";
Mustache.compiler().nullValue("what").compile(tmpl).execute(new Object() {
    String exists = "Say";
    String nullValued = null;
    // String doesNotExist
});
// throws MustacheException when executing the template because doesNotExist cannot be resolved
```

When using a `Map` as a context, `nullValue()` will only be used when the map contains a mapping to
`null`. If the map lacks a mapping for a given variable, then it is considered unresolvable and
throws an exception.

```java
Map<String,String> map = new HashMap<String,String>();
map.put("exists", "Say");
map.put("nullValued", null);
// no mapping exists for "doesNotExist"
String tmpl = "{{exists}} {{nullValued}} {{doesNotExist}}?";
Mustache.compiler().nullValue("what").compile(tmpl).execute(map);
// throws MustacheException when executing the template because doesNotExist cannot be resolved
```

**Do not** use both `defaultValue` and `nullValue` in your compiler configuration. Each one
overrides the other, so whichever one you call last is the behavior you will get. But even if you
accidentally do the right thing, you have confusing code, so don't call both, use one or the other.

### Sections

Sections are not affected by the `nullValue()` or `defaultValue()` settings. Their behavior is
governed by a separate configuration: `strictSections()`.

By default, a section that is not resolvable or which resolves to `null` will be omitted (and
conversely, an inverse section that is not resolvable or resolves to `null` will be included). If
you use `strictSections(true)`, sections that refer to an unresolvable value will always throw an
exception. Sections that refer to a resolvable but `null` value never throw an exception,
regardless of the `strictSections()` setting.

Extensions
==========

JMustache extends the basic Mustache template language with some additional functionality. These
additional features are enumerated below:

Not escaping HTML by default
----------------------------

You can change the default HTML escaping behavior when obtaining a compiler:

```java
Mustache.compiler().escapeHTML(false).compile("{{foo}}").execute(new Object() {
    String foo = "<bar>";
});
// result: <bar>
// not: &lt;bar&gt;
```

User-defined object formatting
------------------------------

By default, JMustache uses `String.valueOf` to convert objects to strings when rendering a
template. You can customize this formatting by implementing the `Mustache.Formatter` interface:

```java
Mustache.compiler().withFormatter(new Mustache.Formatter() {
    public String format (Object value) {
      if (value instanceof Date) return _fmt.format((Date)value);
      else return String.valueOf(value);
    }
    protected DateFormat _fmt = new SimpleDateFormat("yyyy/MM/dd");
}).compile("{{msg}}: {{today}}").execute(new Object() {
    String msg = "Date";
    Date today = new Date();
})
// result: Date: 2013/01/08
```

User-defined escaping rules
---------------------------

You can change the escaping behavior when obtaining a compiler, to support file formats other than
HTML and plain text.

If you only need to replace fixed strings in the text, you can use `Escapers.simple`:

```java
String[][] escapes = {{ "[", "[[" }, { "]", "]]" }};
Mustache.compiler().withEscaper(Escapers.simple(escapes)).
  compile("{{foo}}").execute(new Object() {
      String foo = "[bar]";
  });
// result: [[bar]]
```

Or you can implement the `Mustache.Escaper` interface directly for more control over the escaping
process.

Special variables
-----------------

### this
You can use the special variable `this` to refer to the context object itself instead of one of its
members. This is particularly useful when iterating over lists.

```java
Mustache.compiler().compile("{{this}}").execute("hello"); // returns: hello
Mustache.compiler().compile("{{#names}}{{this}}{{/names}}").execute(new Object() {
    List<String> names () { return Arrays.asList("Tom", "Dick", "Harry"); }
});
// result: TomDickHarry
```

Note that you can also use the special variable `.` to mean the same thing.

```java
Mustache.compiler().compile("{{.}}").execute("hello"); // returns: hello
Mustache.compiler().compile("{{#names}}{{.}}{{/names}}").execute(new Object() {
    List<String> names () { return Arrays.asList("Tom", "Dick", "Harry"); }
});
// result: TomDickHarry
```

`.` is apparently supported by other Mustache implementations, though it does not appear in the
official documentation.

### -first and -last
You can use the special variables `-first` and `-last` to perform special processing for list
elements. `-first` resolves to `true` when inside a section that is processing the first of a list
of elements. It resolves to `false` at all other times. `-last` resolves to `true` when inside a
section that is processing the last of a list of elements. It resolves to `false` at all other
times.

One will often make use of these special variables in an inverted section, as follows:

```java
String tmpl = "{{#things}}{{^-first}}, {{/-first}}{{this}}{{/things}}";
Mustache.compiler().compile(tmpl).execute(new Object() {
    List<String> things = Arrays.asList("one", "two", "three");
});
// result: one, two, three
```

Note that the values of `-first` and `-last` refer only to the inner-most enclosing section. If you
are processing a section within a section, there is no way to find out whether you are in the first
or last iteration of an outer section.

### -index
The `-index` special variable contains 1 for the first iteration through a section, 2 for the
second, 3 for the third and so forth. It contains 0 at all other times. Note that it also contains
0 for a section that is populated by a singleton value rather than a list.

```java
String tmpl = "My favorite things:\n{{#things}}{{-index}}. {{this}}\n{{/things}}";
Mustache.compiler().compile(tmpl).execute(new Object() {
    List<String> things = Arrays.asList("Peanut butter", "Pen spinning", "Handstands");
});
// result:
// My favorite things:
// 1. Peanut butter
// 2. Pen spinning
// 3. Handstands
```

Compound variables
------------------

In addition to resolving simple variables using the context, you can use compound variables to
extract data from sub-objects of the current context. For example:

```java
Mustache.compiler().compile("Hello {{field.who}}!").execute(new Object() {
    public Object field = new Object() {
        public String who () { return "world"; }
    }
});
// result: Hello world!
```

By taking advantage of reflection and bean-property-style lookups, you can do kooky things:

```java
Mustache.compiler().compile("Hello {{class.name}}!").execute(new Object());
// result: Hello java.lang.Object!
```

Note that compound variables are essentially short-hand for using singleton sections. The above
examples could also be represented as:

    Hello {{#field}}{{who}}{{/field}}!
    Hello {{#class}}{{name}}{{/class}}!

Note also that one semantic difference exists between nested singleton sections and compound
variables: after resolving the object for the first component of the compound variable, parent
contexts will not be searched when resolving subcomponents.

Newline trimming
----------------

If the opening or closing section tag are the only thing on a line, any surrounding whitespace and
the line terminator following the tag are trimmed. This allows for civilized templates, like:

    Favorite foods:
    <ul>
      {{#people}}
      <li>{{first_name}} {{last_name}} likes {{favorite_food}}.</li>
      {{/people}}
    </ul>

which produces output like:

    Favorite foods:
    <ul>
      <li>Elvis Presley likes peanut butter.</li>
      <li>Mahatma Gandhi likes aloo dum.</li>
    </ul>

rather than:

    Favorite foods:
    <ul/>
      
      <li>Elvis Presley likes peanut butter.</li>
      
      <li>Mahatma Gandhi likes aloo dum.</li>
      
    </ul>

which would be produced without the newline trimming.

Nested Contexts
---------------

If a variable is not found in a nested context, it is resolved in the next outer context. This
allows usage like the following:

```java
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
```

Note that if a variable _is_ defined in an inner context, it shadows the same name in the outer
context. There is presently no way to access the variable from the outer context.

Invertible Lambdas
------------------

For some applications, it may be useful for lambdas to be executed for an inverse section rather
than having the section omitted altogether. This allows for proper conditional substitution when
statically translating templates into other languages or contexts:

```java
String template = "{{#condition}}result if true{{/condition}}\n" +
  "{{^condition}}result if false{{/condition}}";
Mustache.compiler().compile(template).execute(new Object() {
    Mustache.InvertibleLambda condition = new Mustache.InvertibleLambda() {
        public void execute (Template.Fragment frag, Writer out) throws IOException {
            // this method is executed when the lambda is referenced in a normal section
            out.write("if (condition) {console.log(\"");
            out.write(toJavaScriptLiteral(frag.execute()));
            out.write("\")}");
        }
        public void executeInverse (Template.Fragment frag, Writer out) throws IOException {
            // this method is executed when the lambda is referenced in an inverse section
            out.write("if (!condition) {console.log(\"");
            out.write(toJavaScriptLiteral(frag.execute()));
            out.write("\")}");
        }
        private String toJavaScriptLiteral (String execute) {
            // note: this is NOT a complete implementation of JavaScript string literal escaping
            return execute.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"");
        }
    };
});
// results:
// if (condition) {console.log("result if true")}
// if (!condition) {console.log("result if false")}
```

Of course, you are not limited strictly to conditional substitution -- you can use an
InvertibleLambda whenever you need a single function with two modes of operation.

Standards Mode
--------------

The more intrusive of these extensions, specifically the searching of parent contexts and the use
of compound varables, can be disabled when creating a compiler, like so:

```java
Map<String,String> ctx = new HashMap<String,String>();
ctx.put("foo.bar", "baz");
Mustache.compiler().standardsMode(true).compile("{{foo.bar}}").execute(ctx);
// result: baz
```

Thread Safety
=============

JMustache is internally thread safe with the following caveats:

  * Compilation: compiling templates calls out to a variety of helper classes:
    `Mustache.Formatter`, `Mustache.Escaper`, `Mustache.TemplateLoader`, `Mustache.Collector`. The
    default implementations of these classes are thread-safe, but if you supply custom instances,
    then you have to ensure that your custom instances are thread-safe.

  * Execution: executing templates can call out to some helper classes: `Mustache.Lambda`,
    `Mustache.VariableFetcher`. The default implementations of these classes are thread-safe, but
    if you supply custom instances, then you have to ensure that your custom instances are
    thread-safe.

  * Context data: if you mutate the context data passed to template execution while the template is
    being executed, then you subject yourself to race conditions. It is in theory possible to use a
    thread-safe map (`ConcurrentHashMap` or `Collections.synchronizedMap`) for your context data,
    which would allow you to mutate the data while templates were being rendered based on that
    data, but you're playing with fire by doing that. I don't recommend it. If your data is
    supplied as POJOs where fields or methods are called via reflection to populate your templates,
    volatile fields and synchronized methods could similarly be used to support simultaneous
    reading and mutating, but again you could easily make a mistake that introduces race conditions
    or cause weirdness when executing your templates. The safest approach when rendering the same
    template via simultaneous threads is to pass immutable/unchanging data as the context for each
    execution.

  * `VariableFetcher` cache: template execution uses one internal cache to store resolved
    `VariableFetcher` instances (because resolving a variable fetcher is expensive). This cache is
    thread-safe by virtue of using a `ConcurrentHashMap`. It's possible for a bit of extra work to
    be done if two threads resolve the same variable at the same time, but they won't conflict with
    one another, they'll simply both resolve the variable instead of one resolving the variable and
    the other using the cached resolution.

So the executive summary is: as long as all helper classes you supply are thread-safe (or you use
the defaults), it is safe to share a `Mustache.Compiler` instance across threads to compile
templates. If you pass immutable data to your templates when executing, it is safe to have multiple
threads simultaneously execute a single `Template` instance.

Limitations
===========

In the name of simplicity, some features of Mustache were omitted or simplified:

  * `{{= =}}` only supports one or two character delimiters. This is just because I'm lazy and it
    simplifies the parser.

[Template.Fragment]: http://samskivert.github.io/jmustache/apidocs/com/samskivert/mustache/Template.Fragment.html#decompile--
