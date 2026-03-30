package com.yhg.olivemarket.domain.member.controller;

import com.yhg.olivemarket.domain.member.dto.request.JoinRequest;
import com.yhg.olivemarket.domain.member.dto.request.LoginRequest;
import com.yhg.olivemarket.domain.member.dto.response.MemberResponse;
import com.yhg.olivemarket.domain.member.dto.response.TokenResponse;
import com.yhg.olivemarket.domain.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class MemberController {

    private final MemberService memberService;

    // POST /api/auth/join - 회원가입
    @PostMapping("/join")
    public ResponseEntity<MemberResponse> join(@RequestBody @Valid JoinRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberService.join(request));
    }

    // POST /api/auth/login - 로그인 → JWT 발급
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(memberService.login(request));
    }
}
