package raicod3.example.com.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import raicod3.example.com.constants.Http_Constants;
import raicod3.example.com.dto.user.UserResponseDto;
import raicod3.example.com.exception.ResourceNotFoundException;
import raicod3.example.com.model.User;
import raicod3.example.com.repository.UserRepository;
import raicod3.example.com.utilities.APIResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;


    public UserService (UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Page<UserResponseDto> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);

        return users.map(UserResponseDto::new);
    }

    public UserResponseDto getUserById(UUID id) {

        User existingUser = userRepository.findById(id).orElseThrow(() ->  new ResourceNotFoundException("User not found."));

        return new UserResponseDto(existingUser);
    }

    public UserResponseDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found."));

        return new UserResponseDto(user);
    }

}
