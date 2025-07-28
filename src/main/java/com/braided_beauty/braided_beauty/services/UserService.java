package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.user.member.UserMemberProfileResponseDTO;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.mappers.user.member.UserMemberDtoMapper;
import com.braided_beauty.braided_beauty.models.User;
import com.braided_beauty.braided_beauty.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMemberDtoMapper userMemberDtoMapper;

    public UserMemberProfileResponseDTO getMemberProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User does not exist."));

        return userMemberDtoMapper.toDTO(user);
    }
}
