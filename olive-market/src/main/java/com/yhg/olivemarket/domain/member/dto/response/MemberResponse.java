package com.yhg.olivemarket.domain.member.dto.response;

import com.yhg.olivemarket.domain.member.entity.Member;
import com.yhg.olivemarket.domain.member.entity.Role;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MemberResponse {

    private final Long id;
    private final String email;
    private final String name;
    private final Role role;
    private final LocalDateTime createdAt;

    private MemberResponse(Member member) {
        this.id = member.getId();
        this.email = member.getEmail();
        this.name = member.getName();
        this.role = member.getRole();
        this.createdAt = member.getCreatedAt();
    }

    public static MemberResponse from(Member member) {
        return new MemberResponse(member);
    }
}
