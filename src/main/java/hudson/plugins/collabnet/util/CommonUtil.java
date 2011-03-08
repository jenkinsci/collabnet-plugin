package hudson.plugins.collabnet.util;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.json.JSONObject;

/**
 * Class for methods that are useful across Jenkins plugins.
 */
public class CommonUtil {
    
    /**
     * As a utility class, CommonUtil should never be instantiated.
     */
    private CommonUtil() {}

    /**
     * Returns true if the string value of the key in the form
     * is "true".  If it's missing, returns false.
     *
     * @param key to find value for.
     * @param formData that holds key/value.
     * @return true if the string value of this key is "true".
     */
    public static boolean getBoolean(String key, JSONObject formData) {
        boolean value = false;
        String valueStr = (String)formData.get(key);
        if (valueStr != null) {
            value = valueStr.equals("true");
        }
        return value;
    }  

    /**
     * Translates a string that may contain  build vars like ${BUILD_VAR} to
     * a string with those vars interpreted.
     * 
     * @param build the Jenkins build.
     * @param str the string to be interpreted.
     * @return the interpreted string.
     * @throws IllegalArgumentException if the env var is not found.
     */
    public static String getInterpreted(Map<String, String> envVars, 
                                        String str) {
        Pattern envPat = Pattern.compile("\\$\\{(\\w*)\\}");
        Matcher matcher = envPat.matcher(str);
        StringBuffer intStr = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            // find build_var
            if (!envVars.containsKey(key)) {
                String message = "Environmental Variable not found: " + key;
                throw new IllegalArgumentException(message);
            } else {
                String value = envVars.get(key);
                matcher.appendReplacement(intStr, value);
            }
        }
        matcher.appendTail(intStr);
        return intStr.toString();
    }

    /**
     * Convenience method to log RemoteExceptions.  
     *
     * @param log to log this message to.
     * @param methodName in progress on when this exception occurred.
     * @param re The RemoteException that was thrown.
     */
    public static void logRE(Logger logger, String methodName, 
                             RemoteException re) {
        logger.info(methodName + " failed due to " + 
                    re.getClass().getName() + ": " + re.getMessage());
    }

    /**
     * Escape characters that may make javascript error (like quotes or
     * backslashes).
     *
     * @param collection list of strings to sanitize
     * @return collection of sanitized strings.
     */
    @Deprecated // marshalling related details should be done only at the very end
    public static Collection<String> sanitizeForJS(Collection<String> collection) {
        Collection<String> sanitized = new ArrayList<String>();
        for (String c: collection) {
            c = c.replace("\\", "\\\\"); 
            c = c.replace("'","\\'"); 
            c = c.replace("\"","\\\""); 
            c = c.replace("<","\\<"); 
            c = c.replace(">","\\>"); 
            sanitized.add(c);
        }
        return sanitized;
    }

    /**
     * String leading and trailing '/'s from a String.
     *
     * @param str string to strip.
     * @return string without leading or trailing '/'s.
     */
    public static String stripSlashes(String str) {
        str = str.replaceAll("^/+", "");
        return str.replaceAll("/+$", "");
    }

    /**
     * @param value string to test.
     * @return true if a String value is null or empty.
     */
    public static boolean unset(String value) {
        if (value == null || value.equals("")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Given a comma-delimited string, split it into an array of
     * strings, removing unneccessary whitespace.  Also will remove
     * empty values (i.e. only whitespace).
     * 
     * @param commaStr 
     * @return an array of the strings, with leading and trailing 
     *         whitespace removed.
     */
    public static List<String> splitCommaStr(String commaStr) {
        List<String> results =
            new ArrayList<String>(Arrays.asList(commaStr.trim().split("\\s*,\\s*")));
        for (Iterator<String> it = results.iterator(); it.hasNext();) {
            String next = it.next();
            next = next.trim();
            if (next.equals("")) {
                it.remove();
            }
        }
        return results;
    }

    /**
     * Determine whether a string is "empty", meaning is null or empty.
     * @param str string in question
     * @return true if empty
     */
    public static boolean isEmpty(String str) {
        return (str == null || str.trim().length() == 0); 
    }
}
