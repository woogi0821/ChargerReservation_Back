package com.simplecoding.chargerreservation.common.jwt;

import com.simplecoding.chargerreservation.member.entity.Member;
import lombok.Builder;
import lombok.Getter;
import java.util.Map;

@Getter
@Builder
public class OAuth2Attributes {
    private Map<String, Object> attributes;     // 소셜 응답 전체 데이터
    private String nameAttributeKey;           // OAuth2 로그인 키 (PK 역할)
    private String name;                       // 사용자 이름
    private String email;                      // 사용자 이메일
    private String provider;                   // 소셜 종류 (KAKAO, NAVER)
    private String providerId;                 // 소셜 고유 ID

    /**
     * registrationId(kakao, naver)에 따라 빌더 메서드를 호출
     */
    public static OAuth2Attributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if ("naver".equals(registrationId)) {
            return ofNaver("id", attributes);
        }
        if ("kakao".equals(registrationId)) {
            return ofKakao("id", attributes); // 카카오는 id가 최상위에 있음
        }

        return ofKakao(userNameAttributeName, attributes);
    }

    private static OAuth2Attributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        // 카카오: kakao_account 내부에 profile이 있고 그 안에 닉네임이 있음
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> kakaoProfile = (Map<String, Object>) kakaoAccount.get("profile");

        return OAuth2Attributes.builder()
            .name((String) kakaoProfile.get("nickname"))
            .email((String) kakaoAccount.get("email"))
            .provider("KAKAO")
            .providerId(String.valueOf(attributes.get(userNameAttributeName)))
            .attributes(attributes)
            .nameAttributeKey(userNameAttributeName)
            .build();
    }

    private static OAuth2Attributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
        // 네이버: 모든 데이터가 response라는 키로 감싸져 있음
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        return OAuth2Attributes.builder()
            .name((String) response.get("name"))
            .email((String) response.get("email"))
            .provider("NAVER")
            .providerId((String) response.get(userNameAttributeName))
            .attributes(response)
            .nameAttributeKey(userNameAttributeName)
            .build();
    }

    /**
     * 처음 가입하는 시점 Member 엔티티를 생성
     */
    public Member toEntity() {
        return Member.builder()
            .loginId(provider + "_" + providerId) // 중복 방지를 위한 고유 ID 조합
            .loginPw("")
            .name(name)
            .email(email)
            .provider(provider)
            .providerId(providerId)
//            .status("ACTIVE")
//            .memberGrade("N")
            .build();
    }

}
