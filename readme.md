
Running custom doclet in Eclipse:

* Menu *Project > Generate javadoc*.
* Select *Use custom doclet*.
* Doclet name is the doclet class name, e.g. `com.xrbpowered.doclet.Doclet`.
* Doclet class path is the full path to doclet JAR.
* In the next window, specify extra Javadoc options. XRBDoclet requires `-d` option (output path).

XRBDoclet options:

* `-d <fullpath>` - output path.
* `-doctitle <string>` - title for the generated files.
* `-css <fullpath>` - path to custom CSS.
* `-js <fullpath>` - path to custom Javascript.
* `-overview <packagename>` - overview page defined as a Java package with **package-info.java**.
* `-date` - print current date on every page.
