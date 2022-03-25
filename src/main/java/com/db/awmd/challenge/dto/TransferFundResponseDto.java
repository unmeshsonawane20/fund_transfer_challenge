package com.db.awmd.challenge.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferFundResponseDto extends TransferFundRequestDto {
	
    private BigDecimal balance;
}
