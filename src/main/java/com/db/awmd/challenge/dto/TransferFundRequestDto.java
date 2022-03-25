package com.db.awmd.challenge.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferFundRequestDto {
	
	private String fromAccountNumber;

    private String toAccountNumber;

    private BigDecimal amount;
}
