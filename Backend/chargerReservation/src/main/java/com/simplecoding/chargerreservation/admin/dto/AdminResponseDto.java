package com.simplecoding.chargerreservation.admin.dto;

import com.simplecoding.chargerreservation.admin.entity.Admin;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminResponseDto {
        private Long adminId;
        private Long memberId;
        private  String adminRole;

        public static AdminResponseDto from(Admin admin) {
            return new AdminResponseDto(
                    admin.getAdminId(),
                    admin.getMemberId(),
                    admin.getAdminRole()
            );
        }
}