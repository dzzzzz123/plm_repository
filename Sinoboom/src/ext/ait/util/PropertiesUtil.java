package ext.ait.util;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public class PropertiesUtil {

    private Properties properties;

    public PropertiesUtil(Class<?> callingClass) {
        try {
            String propertiefile = callingClass.getResource("config.properties").getFile();
            properties = new Properties();
            properties.load(new InputStreamReader(new FileInputStream(propertiefile), "UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getStr(String key) {
        String strinfo = properties.getProperty(key);
        if (strinfo != null) {
            strinfo = strinfo.trim();
        }
        return strinfo;
    }
}
