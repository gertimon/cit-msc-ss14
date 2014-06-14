package de.tuberlin.cit.project.energy.helper.fileTools;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 * @author Tobias
 */
public class PropertyLoader {

    /**
     * loads file, TODO cache file for specific time te reduce disk io
     *
     * @param filename
     * @return
     */
    public static Properties loadProperties(String filename) {
        Properties properties = null;
        InputStream input = null;
        try {
            input = PropertyLoader.class.getClassLoader().getResourceAsStream(filename);
            if (input == null) {
                System.out.println("Sorry, unable to find " + filename);
                return null;
            }
            properties = new Properties();
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return properties;
    }

}
