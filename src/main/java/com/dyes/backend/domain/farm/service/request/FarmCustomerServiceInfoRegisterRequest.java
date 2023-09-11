package com.dyes.backend.domain.farm.service.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FarmCustomerServiceInfoRegisterRequest {
    private String csContactNumber;
    private String address;
    private String zipCode;
    private String addressDetail;
}
