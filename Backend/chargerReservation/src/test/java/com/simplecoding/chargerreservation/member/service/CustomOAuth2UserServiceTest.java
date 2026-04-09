package com.simplecoding.chargerreservation.member.service;

import com.simplecoding.chargerreservation.common.jwt.OAuth2Attributes;
import com.simplecoding.chargerreservation.member.entity.Member;
import com.simplecoding.chargerreservation.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    @Test
    @DisplayName("카카오 소셜 로그인 - 신규 회원 가입 테스트")
    void saveOrUpdate_NewMember_Success() {
        // 1. given: 카카오 응답 데이터(Attributes) 생성
        Map<String, Object> attributes = new HashMap<>();
        Map<String, Object> kakaoAccount = new HashMap<>();
        Map<String, Object> profile = new HashMap<>();

        profile.put("nickname", "테스터");
        kakaoAccount.put("profile", profile);
        kakaoAccount.put("email", "test@kakao.com");
        attributes.put("id", "12345678");
        attributes.put("kakao_account", kakaoAccount);

        // 로직의 핵심인 DTO 생성
        OAuth2Attributes oauthAttributes = OAuth2Attributes.of("kakao", "id", attributes);

        // Repository 동작 정의 (saveOrUpdate 내부에서 호출되는 것들만!)
        when(memberRepository.findByProviderAndProviderId("KAKAO", "12345678"))
            .thenReturn(Optional.empty());

        when(memberRepository.save(any(Member.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // 2. when: 핵심 로직인 saveOrUpdate만 직접 호출
        Member savedMember = customOAuth2UserService.saveOrUpdate(oauthAttributes);

        // 3. then: 결과 검증
        assertThat(savedMember.getName()).isEqualTo("테스터");
        assertThat(savedMember.getProvider()).isEqualTo("KAKAO");
        assertThat(savedMember.getProviderId()).isEqualTo("12345678");

        // 실제로 호출되었는지 횟수 검증
        verify(memberRepository, times(1)).findByProviderAndProviderId("KAKAO", "12345678");
        verify(memberRepository, times(1)).save(any(Member.class));
    }
}