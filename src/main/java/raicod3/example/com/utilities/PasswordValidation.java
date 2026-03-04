package raicod3.example.com.utilities;

public class PasswordValidation {

    private static final String specialCharactersRegex = ".*[^a-zA-Z0-9].*";
    private static final String lowerCaseRegex = ".*[a-z].*";
    private static final String upperCaseRegex = ".*[A-Z].*";
    private static final String numberRegex = ".*[0-9].*";

    public static String validatePassword(String password) {
        if (password.length() < 8) {
            return "Password must be at least 8 characters";
        }

        if(!password.matches(specialCharactersRegex)) {
            return "Password must contain at least one special character";
        }

        if(!password.matches(lowerCaseRegex)) {
            return "Password must contain at least one lower case character";
        }

        if(!password.matches(upperCaseRegex)) {
            return "Password must contain at least one upper case character";
        }

        if(!password.matches(numberRegex)) {
            return "Password must contain at least one number character";
        }

        return "Strong";

    }

}
