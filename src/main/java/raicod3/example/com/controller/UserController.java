package raicod3.example.com.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import raicod3.example.com.constants.Http_Constants;
import raicod3.example.com.custom.CustomUserDetails;
import raicod3.example.com.dto.customer.UpdateCustomerProfileRequest;
import raicod3.example.com.dto.user.UserAddressDto;
import raicod3.example.com.dto.user.UserResponseDto;
import raicod3.example.com.service.UserAddressService;
import raicod3.example.com.service.UserService;
import raicod3.example.com.utilities.APIResponse;
import raicod3.example.com.utilities.PaginationData;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final UserAddressService userAddressService;

    public UserController(UserService userService, UserAddressService userAddressService) {
        this.userService = userService;
        this.userAddressService = userAddressService;
    }

    @GetMapping("")
    public ResponseEntity<APIResponse> getUser(@RequestParam(value = "page", defaultValue = "0") int page, @RequestParam(value = "per_page", defaultValue = "20")  int per_page) {

        Pageable pageable = PageRequest.of(page, per_page);

        Page<UserResponseDto> allUsers = userService.getAllUsers(pageable);


        PaginationData data = new PaginationData(allUsers.getContent(), page, per_page, allUsers.getTotalElements());

        return ResponseEntity.ok(APIResponse.paginate("List of users", data, Http_Constants.OK));
    }

    @GetMapping("/me")
    public ResponseEntity<APIResponse> getLoggedInUser(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        UserResponseDto user = userService.loggedInUser(customUserDetails);

        return ResponseEntity.ok(APIResponse.success(user, "User details", Http_Constants.OK));
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

    @PatchMapping("/update-address/{userId}")
    public ResponseEntity<APIResponse> updateCustomerAddress(@PathVariable("userId") UUID userId, @RequestBody UserAddressDto addressDto) {
        System.out.println("USER: " + userId);
        APIResponse res = userAddressService.addUserAddress(userId, addressDto);

        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }

    @PatchMapping("/profile")
    public ResponseEntity<UserResponseDto> updateProfile(
            @Valid @RequestBody UpdateCustomerProfileRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UserResponseDto updatedProfile = userService.updateCustomerProfile(userDetails.getId(), request);
        return ResponseEntity.ok(updatedProfile);
    }
}
