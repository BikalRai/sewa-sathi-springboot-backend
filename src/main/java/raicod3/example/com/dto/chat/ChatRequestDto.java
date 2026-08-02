package raicod3.example.com.dto.chat;

import jakarta.validation.constraints.NotBlank;

public record ChatRequestDto(
        @NotBlank(message = "Message content cannot be empty")
        String content
) {
}
