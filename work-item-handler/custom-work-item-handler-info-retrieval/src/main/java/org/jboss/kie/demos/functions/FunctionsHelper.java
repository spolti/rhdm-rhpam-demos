package org.jboss.kie.demos.functions;

public class FunctionsHelper {


    public String systemUsername() {
        return System.getProperty("user.name");
    }

    public String systemUserHome() {
        return System.getProperty("user.home");
    }

    public String systemUserCountryLang() {
        return System.getProperty("user.language") + "_" + System.getProperty("user.country");
    }

    public String javaVersion() {
        return System.getProperty("java.runtime.version");
    }

}