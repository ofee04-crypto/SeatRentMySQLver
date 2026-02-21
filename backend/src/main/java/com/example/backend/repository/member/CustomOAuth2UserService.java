package com.example.backend.repository.member;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.example.backend.model.member.Member;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email"); // 例如: alan123@gmail.com
        String name = oAuth2User.getAttribute("name"); // 例如: 林小安

        Optional<Member> memberOptional = memberRepository.findByMemEmail(email);

        if (memberOptional.isEmpty()) {
            Member newMember = new Member();
            newMember.setMemEmail(email);
            newMember.setMemName(name);

            // --- 關鍵修改：截取 Gmail @ 前面的字串當作帳號 ---
            if (email != null && email.contains("@")) {
                String username = email.split("@")[0];
                newMember.setMemUsername(username); // 設定為 alan123
            } else {
                newMember.setMemUsername(email); // 備案
            }

            // 處理資料庫 NOT NULL 限制
            newMember.setMemPassword("OAUTH_USER_" + UUID.randomUUID().toString().substring(0, 8));
            newMember.setMemPhone("未設定");

            // 根據你資料表的預設值
            newMember.setMemStatus(1);
            newMember.setMemPoints(0);
            newMember.setMemViolation(0);
            newMember.setMemLevel(1);

            memberRepository.save(newMember);
        }

        return oAuth2User;
    }
}
