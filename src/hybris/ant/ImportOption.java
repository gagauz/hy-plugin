package hybris.ant;

public enum ImportOption {

    CUSTOM_ONLY,
    BIN_NOT_PLATFORM,
    ALL;

    public static ImportOption currentOption;

    public boolean is() {
        return this == currentOption;
    }
}
