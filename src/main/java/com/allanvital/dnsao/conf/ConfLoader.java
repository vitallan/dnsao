package com.allanvital.dnsao.conf;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ConfLoader {

    public static Conf load(InputStream input) {
        try (input) {

            if (input == null) {
                throw new RuntimeException("application.yml not found");
            }

            LoaderOptions options = new LoaderOptions();
            Constructor constructor = new Constructor(Conf.class, options);
            Representer representer = new Representer(new DumperOptions());
            representer.getPropertyUtils().setSkipMissingProperties(true);
            Yaml yaml = new Yaml(constructor, representer);
            Conf conf = yaml.load(input);
            if (conf == null) {
                conf = new Conf();
            }
            return conf;

        } catch (Exception e) {
            throw new RuntimeException("Error loading configuration", e);
        }
    }

    public static Conf load() {
        String configPath = System.getProperty("config");
        if (configPath == null) {
            return load(ConfLoader.class.getClassLoader().getResourceAsStream("application.yml"));
        } else {
            try {
                return load(Files.newInputStream(Paths.get(configPath)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}