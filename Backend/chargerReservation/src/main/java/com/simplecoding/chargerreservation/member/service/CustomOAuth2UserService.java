package com.simplecoding.chargerreservation.member.service;

import com.simplecoding.chargerreservation.common.jwt.OAuth2Attributes;
import com.simplecoding.chargerreservation.member.entity.Member;
import com.simplecoding.chargerreservation.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 서비스 구분 (KAKAO, NAVER 등)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // OAuth2 로그인 진행 시 키가 되는 필드값 (PK 역할)
        String userNameAttributeName = userRequest.getClientRegistration()
            .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // 소셜별로 다른 응답 데이터를 공통 DTO(OAuth2Attributes)로 변환
        OAuth2Attributes attributes = OAuth2Attributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        // 사용자 저장 및 업데이트
        Member member = saveOrUpdate(attributes);

        // 시큐리티 세션에 저장할 사용자 객체 반환
        return new DefaultOAuth2User(
//            Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
            Collections.singleton(new SimpleGrantedAuthority(member.getMemberGrade())),
            attributes.getAttributes(),
            attributes.getNameAttributeKey()
        );
    }

    @Transactional
    public Member saveOrUpdate(OAuth2Attributes attributes) {
        if (attributes.getEmail() == null) {
            log.warn("소셜 로그인 사용자({})의 이메일 정보가 없습니다.", attributes.getProviderId());
        }

        Member member = memberRepository.findByProviderAndProviderId(attributes.getProvider(), attributes.getProviderId())
            .map(entity -> {
                entity.updateSocialInfo(attributes.getName(),"");
                return entity;
            })
            .orElseGet(attributes::toEntity);

        return memberRepository.save(member);
    }

}
