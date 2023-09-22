# Unlogged Java SDK

Unlogged Java SDK enabled recording of code execution in a binary format. The recording is highly detailed and can 
be used to reconstruct the code execution from scratch.

The [binary format descriptions](https://github.com/unloggedio/common/tree/master/src/main/kaitai) are available in Kaitai format here

## Usage

1. Include dependency

### Maven
```xml
<dependencies>
    <dependency>
        <groupId>io.unlogged</groupId>
        <artifactId>unlogged-java-sdk</artifactId>
        <version>0.1.9</version>
    </dependency>
</dependencies>
```

### Gradle

```groovy
dependencies
{
    implementation 'video.bug:unlogged-sdk:0.1.9'
    annotationProcessor 'video.bug:unlogged-sdk:0.1.9'
}
```

2. Add `@Unlogged` annotation to your application entry point
```java
    @Unlogged
    public static void main(String[] args) {
        // 
    }
```

## Disabling recording

Adding the unlogged-sdk adds probes to your code which emits events in a binary format. Adding the `@Unlogged` 
enabled to actual execution of those probes.

### To disable at compile time

```bash
mvn package -Dunlogged.disable
```

or 

```bash
./gradlew build -Dunlogged.disable
```

### To disable at runtime (if not disabled at compile time)

```java
@Unlogged(enable = false)
```

You can find the latest release version here: https://mvnrepository.com/artifact/video.bug/unlogged-sdk


Lombok License

```java
/*
 * Copyright (C) 2009-2018 The Project Lombok Authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
```
