package raicod3.example.com.utilities;

import java.util.Random;

public class NumberHelper {

    public static StringBuilder generateOtp () {
        StringBuilder otp = new StringBuilder();
        Random rand = new Random();

        for (int i = 0; i < 6; i++) {
            int number = rand.nextInt(10);
            otp.append(number);
        }

        return otp;
    }
}
