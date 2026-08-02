package raicod3.example.com.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import raicod3.example.com.custom.CustomUserDetails;
import raicod3.example.com.dto.chat.ChatRequestDto;
import raicod3.example.com.dto.chat.ChatResponseDto;
import raicod3.example.com.dto.chat.ConversationSummaryDto;
import raicod3.example.com.service.ChatService;
import raicod3.example.com.utilities.APIResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CUSTOMER', 'PROVIDER')")
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/jobs/{jobId}/chat")
    public ResponseEntity<APIResponse> getHistory(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID jobId
    ) {
        List<ChatResponseDto> history = chatService.getJobChatHistory(principal.getId(), jobId);

        return ResponseEntity.ok(
                APIResponse.success(history, "Chat history retrieved successfully", HttpStatus.OK.value())
        );
    }

    @PostMapping("/jobs/{jobId}/chat")
    public ResponseEntity<APIResponse> sendMessage(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID jobId,
            @Valid @RequestBody ChatRequestDto request
    ) {
        chatService.sendMessage(principal.getId(), jobId, request.content());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                APIResponse.success("Message sent successfully", HttpStatus.CREATED.value())
        );
    }

    @GetMapping("/messages/inbox")
    public ResponseEntity<APIResponse> getInbox(@AuthenticationPrincipal CustomUserDetails principal) {
        List<ConversationSummaryDto> inbox = chatService.getInbox(principal.getId());
        return ResponseEntity.ok(
                APIResponse.success(inbox, "Inbox retrieved successfully", HttpStatus.OK.value())
        );
    }
}
