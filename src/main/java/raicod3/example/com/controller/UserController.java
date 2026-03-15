package raicod3.example.com.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import raicod3.example.com.constants.Http_Constants;
import raicod3.example.com.dto.user.UserResponseDto;
import raicod3.example.com.service.UserService;
import raicod3.example.com.utilities.APIResponse;
import raicod3.example.com.utilities.PaginationData;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("")
    public ResponseEntity<APIResponse> getUser(@RequestParam(value = "page", defaultValue = "0") int page, @RequestParam(value = "per_page", defaultValue = "20")  int per_page) {

        Pageable pageable = PageRequest.of(page, per_page);

        Page<UserResponseDto> allUsers = userService.getAllUsers(pageable);


        PaginationData data = new PaginationData(allUsers.getContent(), page, per_page, allUsers.getTotalElements());

        return ResponseEntity.ok(APIResponse.paginate("List of users", data, Http_Constants.OK));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<APIResponse> getUser(@PathVariable("userId") UUID userId) {
        UserResponseDto user = userService.getUserById(userId);

        return ResponseEntity.ok(APIResponse.success(user, "User retrieved successfully", Http_Constants.OK));
    }

    @GetMapping("/email")
    public ResponseEntity<APIResponse> getUserByEmail(@RequestParam(value = "email") String email) {
        UserResponseDto user = userService.getUserByEmail(email);

        return ResponseEntity.ok(APIResponse.success(user, "User retrieved successfully", Http_Constants.OK));
    }

//    @PutMapping("/update/{userId}")
//    public ResponseEntity<APIResponse> updateUser(@PathVariable("userId") UUID userId, @RequestBody UserEmailRequestDto userEmailRequestDto) {
//
//    }
}
