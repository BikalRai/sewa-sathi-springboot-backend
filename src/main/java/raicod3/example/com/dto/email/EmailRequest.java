package raicod3.example.com.dto.email;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class EmailRequest {

    private String email;
    private String subject;
    private String body;

    public EmailRequest(String email, String subject) {
        this.email = email;
        this.subject = subject;
    }
}
