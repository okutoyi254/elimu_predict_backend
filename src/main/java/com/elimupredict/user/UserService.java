package com.elimupredict.user;

import com.elimupredict.common.enums.Role;
import com.elimupredict.student.StudentRepository;
import com.elimupredict.user.dto.RegisterRequest;
import com.elimupredict.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

//    Register system user
    @Transactional
    public UserResponse registerUser(RegisterRequest request, String registeredBy){

     // Validate unique username
        if(userRepository.existsByUsername(request.getUserName())){
            throw new RuntimeException("Username already exists: "+request.getUserName());
        }

        User user= User.builder()
                .fullName(request.getFullName())
                .username(request.getUserName())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .isActive(true)
                .createdBy(registeredBy)
                .build();

        User saved = userRepository.save(user);


//        Link to student if registering a parent
        if(request.getRole() == Role.PARENT
                && request.getAdmissionNumbers() !=null
        && !request.getAdmissionNumbers().isEmpty()){
            for (String admissionNumber : request.getAdmissionNumbers()) {
                try {
                    linkParentToStudent(saved.getId(), admissionNumber);
                } catch (Exception e) {
                    log.warn("[USER SERVICE] Could not link student {} to parent {} — {}",
                            admissionNumber, saved.getUsername(), e.getMessage());
                }
            }
        }

        log.info("[USER SERVICE] {} registered {} with role {}",
                registeredBy,saved.getUsername(),saved.getRole());

        return toResponse(user);

    }

//    Link Parent to student
    @Transactional
    public UserResponse linkParentToStudent(Long parentId, String admissionNumber){

        User parent = userRepository.findById(parentId)
                .orElseThrow(()-> new RuntimeException("Parent not found: "+parentId));

        if(parent.getRole() != Role.PARENT){
            throw new RuntimeException(" User "+parent.getUsername()+" is not a PARENT");
        }

        // Find student and link
        var student = studentRepository.findByAdmissionNumber(admissionNumber)
                .orElseThrow(() -> new RuntimeException(
                        "Student not found: " + admissionNumber));

        student.setParentId(parentId);
        studentRepository.save(student);

        log.info("[USER SERVICE] Parent {} linked to student {}",
                parent.getUsername(), admissionNumber);

        return toResponse(parent);
    }



    @Transactional
    public UserResponse assignRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.setRole(role);
        return toResponse(userRepository.save(user));
    }

    // ── Revoke access ──
    @Transactional
    public UserResponse revokeAccess(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.setIsActive(false);
        return toResponse(userRepository.save(user));
    }

    // ── Restore access ──
    @Transactional
    public UserResponse restoreAccess(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.setIsActive(true);
        return toResponse(userRepository.save(user));
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getUsersByRole(Role role) {
        return userRepository.findByRole(role)
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getByUserId(String userName) {
        return toResponse(userRepository.findByUsername(userName)
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + userName)));
    }

    private UserResponse toResponse(User u){
        return UserResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .userId(u.getUsername())
                .role(u.getRole())
                .isActive(u.getIsActive())
                .createdAt(u.getCreatedAt())
                .createdBy(u.getCreatedBy())
                .build();
    }

    public User findOrThrow(Long id){
        return userRepository.findById(id)
                .orElseThrow(()->new RuntimeException("User not found: "+id));
    }

}
