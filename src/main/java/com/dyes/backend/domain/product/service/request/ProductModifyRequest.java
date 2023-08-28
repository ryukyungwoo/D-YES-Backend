package com.dyes.backend.domain.product.service.request;

import com.dyes.backend.domain.product.entity.CultivationMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductModifyRequest {
    private String productName;
    private String productDescription;
    private CultivationMethod cultivationMethod;
}
