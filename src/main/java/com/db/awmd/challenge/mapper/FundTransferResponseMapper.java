package com.db.awmd.challenge.mapper;

import java.math.BigDecimal;

import com.db.awmd.challenge.dto.TransferFundResponseDto;

public class FundTransferResponseMapper {
	
	public static TransferFundResponseDto mapResponse(
											String fromAccountId, 
											String toAccountId, 
											BigDecimal amount, 
											BigDecimal balance) {
		
		 TransferFundResponseDto resp = new TransferFundResponseDto();
         resp.setFromAccountNumber(fromAccountId);
         resp.setToAccountNumber(fromAccountId);
         resp.setAmount(amount);
         resp.setBalance(balance);
         
         return resp;
	}

}
