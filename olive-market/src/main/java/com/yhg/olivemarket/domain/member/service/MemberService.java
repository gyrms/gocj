package com.yhg.olivemarket.domain.member.service;

import com.yhg.olivemarket.domain.member.dto.request.JoinRequest;
import com.yhg.olivemarket.domain.member.dto.request.LoginRequest;
import com.yhg.olivemarket.domain.member.dto.response.MemberResponse;
import com.yhg.olivemarket.domain.member.dto.response.TokenResponse;
import com.yhg.olivemarket.domain.member.entity.Member;
import com.yhg.olivemarket.domain.member.entity.Role;
import com.yhg.olivemarket.domain.member.repository.MemberRepository;
import com.yhg.olivemarket.global.auth.JwtTokenProvider;
import com.yhg.olivemarket.global.exception.CustomException;
import com.yhg.olivemarket.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // 회원가입
    @Transactional
    public MemberResponse join(JoinRequest request) {
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Member member = Member.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // BCrypt 암호화
                .name(request.getName())
                .role(Role.USER) // 기본 권한은 USER
                .build();

        return MemberResponse.from(memberRepository.save(member));
    }

    // 로그인 → JWT 반환
    public TokenResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        String token = jwtTokenProvider.generateToken(member.getEmail(), member.getRole().name());
        return TokenResponse.of(token);
    }
}
