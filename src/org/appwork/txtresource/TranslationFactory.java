/**
 *
 * ====================================================================================================================================================
 *         "AppWork Utilities" License
 *         The "AppWork Utilities" will be called [The Product] from now on.
 * ====================================================================================================================================================
 *         Copyright (c) 2009-2015, AppWork GmbH <e-mail@appwork.org>
 *         Schwabacher Straße 117
 *         90763 Fürth
 *         Germany
 * === Preamble ===
 *     This license establishes the terms under which the [The Product] Source Code & Binary files may be used, copied, modified, distributed, and/or redistributed.
 *     The intent is that the AppWork GmbH is able to provide their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 *     These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 *
 * === 3rd Party Licences ===
 *     Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 *     to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header.
 *
 * === Definition: Commercial Usage ===
 *     If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's any commercial interest or aspect in what you are doing, we consider this as a commercial usage.
 *     If your use-case is neither strictly private nor strictly educational, it is commercial. If you are unsure whether your use-case is commercial or not, consider it as commercial or contact us.
 * === Dual Licensing ===
 * === Commercial Usage ===
 *     If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 *     Contact AppWork for further details: <e-mail@appwork.org>
 * === Non-Commercial Usage ===
 *     If there is no commercial usage (see definition above), you may use [The Product] under the terms of the
 *     "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 *
 *     If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.txtresource;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.StringUtils;

public class TranslationFactory {
    private static final HashMap<String, TranslateInterface> CACHE    = new HashMap<String, TranslateInterface>();
    private static String                                    LANGUAGE = "en";
    static {
        try {
            LANGUAGE = System.getProperty("user.language").toLowerCase();
            if (StringUtils.isEmpty(LANGUAGE)) {
                LANGUAGE = "en";
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * @param string
     * @param ret
     */
    private static void collectByPath(final File path, final HashSet<String> ret) {
        final java.util.List<File> files = Files.getFiles(new FileFilter() {
            @Override
            public boolean accept(final File pathname) {
                return pathname.getName().endsWith(".lng");
            }
        }, path);
        String name;
        if (files != null) {
            for (final File file : files) {
                try {
                    name = file.getName();
                    final int index = name.indexOf(".");
                    if (index < 0 || index >= name.length() - 4) {
                        continue;
                    }
                    name = name.substring(index + 1, name.length() - 4);
                    if (ret.add(name)) {
                        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info(name + " found in " + file);
                    }
                } catch (final Throwable e) {
                    // Invalid LanguageFile nameing
                }
            }
        }
    }

    public static <T extends TranslateInterface> T create(final Class<T> class1) {
        return TranslationFactory.create(class1, TranslationFactory.getDesiredLanguage());
    }

    /**
     * do not call this directly for each translationrequest. use a static cache instead!
     */
    @SuppressWarnings("unchecked")
    public static <T extends TranslateInterface> T create(final Class<T> class1, final String... lookup) {
        synchronized (TranslationFactory.CACHE) {
            final StringBuilder sb = new StringBuilder();
            sb.append(class1.getName());
            for (final String c : lookup) {
                sb.append(c + ";");
            }
            final String id = sb.toString();
            T ret = (T) TranslationFactory.CACHE.get(id);
            if (ret == null) {
                ret = (T) Proxy.newProxyInstance(class1.getClassLoader(), new Class[] { class1 }, new TranslationHandler(class1, lookup));
                TranslationFactory.CACHE.put(id, ret);
            }
            return ret;
        }
    }

    /**
     * @param ret2
     * @param string
     * @return
     */
    private static void findInClassPath(final String path, final HashSet<String> ret) {
        // Search in jar:
        try {
            Enumeration<URL> resources;
            resources = Thread.currentThread().getContextClassLoader().getResources(path);
            String name, p, jarPath, internPath;
            while (resources.hasMoreElements()) {
                final URL url = resources.nextElement();
                if (url.getProtocol().equalsIgnoreCase("jar")) {
                    p = url.getPath();
                    int index = p.lastIndexOf('!');
                    jarPath = p.substring(0, index);
                    internPath = p.substring(index + 2);
                    final JarInputStream jarFile = new JarInputStream(new FileInputStream(new File(new URL(jarPath).toURI())));
                    JarEntry e;
                    String jarName;
                    while ((e = jarFile.getNextJarEntry()) != null) {
                        jarName = e.getName();
                        if (jarName.startsWith(internPath) && jarName.endsWith(".lng")) {
                            name = new File(jarName).getName();
                            index = name.indexOf(".");
                            if (index < 0 || index >= name.length() - 4) {
                                continue;
                            }
                            name = name.substring(index + 1, name.length() - 4);
                            if (ret.add(name)) {
                                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finer(name + " found in " + new File(jarName));
                            }
                        }
                    }
                } else {
                    TranslationFactory.collectByPath(new File(url.toURI()), ret);
                }
            }
        } catch (final Exception e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
        }
    }

    /**
     *
     */
    public static java.util.List<TranslateInterface> getCachedInterfaces() {
        final HashSet<TranslateInterface> ret = new HashSet<TranslateInterface>();
        synchronized (TranslationFactory.CACHE) {
            for (final TranslateInterface intf : TranslationFactory.CACHE.values()) {
                if (intf != null) {
                    ret.add(intf);
                }
            }
        }
        return new ArrayList<TranslateInterface>(ret);
    }

    public static String getDesiredLanguage() {
        return TranslationFactory.LANGUAGE;
    }

    /**
     * @return
     */
    public static Locale getDesiredLocale() {
        final String lng = TranslationFactory.getDesiredLanguage();
        return TranslationFactory.stringToLocale(lng);
    }

    public static List<String> listAvailableTranslations(final Class<? extends TranslateInterface>... classes) {
        final HashSet<String> ret = new HashSet<String>();
        TranslationFactory.collectByPath(Application.getResource("translations"), ret);
        TranslationFactory.findInClassPath("translations", ret);
        for (final Class<? extends TranslateInterface> clazz : classes) {
            TranslationFactory.collectByPath(Application.getResource(clazz.getPackage().getName().replace(".", "/")), ret);
            TranslationFactory.findInClassPath(clazz.getPackage().getName().replace(".", "/"), ret);
            final Defaults defs = clazz.getAnnotation(Defaults.class);
            if (defs != null) {
                for (final String s : defs.lngs()) {
                    if (ret.add(s)) {
                        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finer(s + " src: " + clazz + " Defaults");
                    }
                }
            }
        }
        return new ArrayList<String>(ret);
    }

    /**
     * @return
     */
    public static String localeToString(final Locale l) {
        final StringBuilder sb = new StringBuilder();
        sb.append(l.getLanguage());
        String c = l.getCountry();
        if (c != null && c.trim().length() > 0) {
            sb.append("-");
            sb.append(l.getCountry());
            c = l.getVariant();
            if (c != null && c.trim().length() > 0) {
                sb.append("-");
                sb.append(l.getCountry());
            }
        }
        return sb.toString();
    }

    public static void main(final String[] args) {
        // Locale.setDefault(TranslationFactory.getDesiredLocale());
        // System.out.println(TranslationFactory.getDesiredLocale().getDisplayCountry());
        // final Translate t = TranslationFactory.create(Translate.class);
        // System.out.println(t.getTestText());
        // System.out.println(t.getOrderedText(1, 7, 23, 5));
        // System.out.println(t._getTranslation("en", "getOrderedText", 1, 3, 5,
        // 8));
        // System.err.println(t._createFile("en", true));
        System.out.println(new Locale("zh", "DE", "hans").getDisplayName());
        System.out.println(Locale.TRADITIONAL_CHINESE.getDisplayName());
    }

    public static boolean setDesiredLanguage(final String loc) {
        if (TranslationFactory.getDesiredLanguage().equals(loc)) {
            return false;
        }
        TranslationFactory.LANGUAGE = loc;
        synchronized (TranslationFactory.CACHE) {
            for (final TranslateInterface i : TranslationFactory.CACHE.values()) {
                i._getHandler().setLanguage(loc);
            }
        }
        return true;
    }

    /**
     * @param lng
     * @return
     */
    public static Locale stringToLocale(final String lng) {
        // try {
        // if (Application.getJavaVersion() >= Application.JAVA17) {
        // //
        // Locale ret = Locale.forLanguageTag(lng.replace("_", "-"));
        // return ret;
        // }
        // } catch (final Throwable e) {
        // }
        final String[] split = lng.split("[\\-\\_]");
        switch (split.length) {
        case 1:
            return new Locale(split[0]);
        case 2:
            return new Locale(split[0], split[1]);
        default:
            return new Locale(split[0], split[1], split[2]);
        }
    }
}
