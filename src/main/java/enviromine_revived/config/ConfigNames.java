package enviromine_revived.config;

/** Config key naming, kept free of game classes so a unit test can check the lang file. */
public final class ConfigNames {
    private ConfigNames() {}

    /** "normalBodyTemp" → "normal_body_temp". */
    public static String key(String componentName) {
        StringBuilder out = new StringBuilder();
        for (char c : componentName.toCharArray()) {
            if (Character.isUpperCase(c)) {
                out.append('_').append(Character.toLowerCase(c));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
