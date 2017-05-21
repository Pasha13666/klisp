
# KLISP

Klisp is simple lisp implementation in kotlin.

## Building

Klisp uses gradle. Run `./gradlew jar` to build java application `build/libs/klisp.jar`.

## Using

You can use klisp as standalone interpreter or as library in your project.

### Using standalone

To execute file `file.kl` run
```sh
java -cp klisp.jar ru.pasha__kun.KlispMain <file.kl
```

### Using as library with gradle

Copy `klisp.jar` to your project (for example, in `libs` directory)

```gradle
dependencies {
    compile files('libs/klisp.jar')
}
```

### Using as library with maven

Copy `klisp.jar` to your project (for example, in `libs` directory)

```xml
<dependency>
    <groupId>ru.pasha__kun</groupId>
    <artifactId>klisp</artifactId>
    <version>1.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/klisp.jar</systemPath>
</dependency>
```

## TODO

- New main file, CLI options support.
- Builtin libraries
  - some functions in native `__builtin__` library.
  - native libraries for types (array, map, string, etc).
  - non-native `__builtin_macro__` library.
  - some native libraries, such as `io` or `network`.
  - non-native libraries.
- Document internal functions and classes.
- Move some libraries to separate jar.
- Create tests and examples.

## License

This project is licensed under the terms of the MIT license.

See [license file](LICENSE.md).