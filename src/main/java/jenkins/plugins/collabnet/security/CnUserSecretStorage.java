package jenkins.plugins.collabnet.security;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import hudson.model.User;

public class CnUserSecretStorage {

    private static final Logger LOGGER = Logger.getLogger(CnUserSecretStorage.class.getName());

    private CnUserSecretStorage() {
        // no accessible constructor
    }

    public static boolean contains(@Nonnull User user) {
        return user.getProperty(CnUserSecretProperty.class) != null;
    }

    public static @CheckForNull String retrieve(@Nonnull User user) {
        CnUserSecretProperty property = user.getProperty(CnUserSecretProperty.class);
        if (property == null) {
            LOGGER.info("Cache miss for username: " + user.getId());
            return null;
        } else {
            LOGGER.info("Token retrieved using cache for username: " + user.getId());
            return property.getSecret().getPlainText();
        }
    }

    public static void put(@Nonnull User user, @Nonnull String secret) {
        LOGGER.info("Populating the cache for username: " + user.getId());
        try {
            user.addProperty(new CnUserSecretProperty(secret));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING,
                    "Received an exception when trying to add the GitHub access token to the user: " + user.getId(), e);
        }
    }
}
