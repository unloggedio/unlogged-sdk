package io.unlogged.core;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;

public class ClassRootFinder {
    public ClassRootFinder() {
    }

    private static String urlDecode(String in, boolean forceUtf8) {
        try {
            return URLDecoder.decode(in, forceUtf8 ? "UTF-8" : Charset.defaultCharset().name());
        } catch (UnsupportedEncodingException var3) {
            try {
                return URLDecoder.decode(in, "UTF-8");
            } catch (UnsupportedEncodingException var2) {
                return in;
            }
        }
    }

    public static String findClassRootOfSelf() {
        return findClassRootOfClass(ClassRootFinder.class);
    }

    public static String findClassRootOfClass(Class<?> context) {
        String name = context.getName();
        int idx = name.lastIndexOf(46);
        String packageBase;
        if (idx > -1) {
            packageBase = name.substring(0, idx);
            name = name.substring(idx + 1);
        } else {
            packageBase = "";
        }

        URL selfURL = context.getResource(name + ".class");
        String self = selfURL.toString();
        String jarLoc;
        if (self.startsWith("file:/")) {
            String path = urlDecode(self.substring(5), false);
            if (!(new File(path)).exists()) {
                path = urlDecode(self.substring(5), true);
            }

            jarLoc = "/" + packageBase.replace('.', '/') + "/" + name + ".class";
            if (!path.endsWith(jarLoc)) {
                throw new IllegalArgumentException("Unknown path structure: " + path);
            }

            self = path.substring(0, path.length() - jarLoc.length());
        } else {
            if (!self.startsWith("jar:")) {
                throw new IllegalArgumentException("Unknown protocol: " + self);
            }

            int sep = self.indexOf(33);
            if (sep == -1) {
                throw new IllegalArgumentException("No separator in jar protocol: " + self);
            }

            jarLoc = self.substring(4, sep);
            if (!jarLoc.startsWith("file:/")) {
                throw new IllegalArgumentException("Unknown path structure: " + self);
            }

            String path = urlDecode(jarLoc.substring(5), false);
            if (!(new File(path)).exists()) {
                path = urlDecode(jarLoc.substring(5), true);
            }

            self = path;
        }

        if (self.isEmpty()) {
            self = "/";
        }

        return self;
    }

    public static void main(String[] args) {
        System.out.println(findClassRootOfSelf());
    }
}