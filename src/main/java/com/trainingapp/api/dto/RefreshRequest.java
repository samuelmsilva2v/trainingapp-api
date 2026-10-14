package com.trainingapp.api.dto;

import jakarta.validation.constraints.Size;

/** Refresh token no corpo, para o app Android (na web ele vem no cookie). */
public record RefreshRequest(@Size(max = 200) String refresh) {
}
