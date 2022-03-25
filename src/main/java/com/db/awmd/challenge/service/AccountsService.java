package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.dto.TransferFundRequestDto;
import com.db.awmd.challenge.dto.TransferFundResponseDto;
import com.db.awmd.challenge.exception.BadRequestException;
import com.db.awmd.challenge.exception.InsufficientFundException;
import com.db.awmd.challenge.mapper.FundTransferResponseMapper;
import com.db.awmd.challenge.repository.AccountsRepository;
import com.db.awmd.challenge.web.AccountsController;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AccountsService {
	
	static final HashMap<String, ReentrantLock> locks = new HashMap<String, ReentrantLock>();

  @Getter
  private final AccountsRepository accountsRepository;
  
  @Getter
  private final EmailNotificationService emailNotificationService;

  @Autowired
  public AccountsService(AccountsRepository accountsRepository,EmailNotificationService emailNotificationService) {
    this.accountsRepository = accountsRepository;
    this.emailNotificationService = emailNotificationService;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  //TODO: annotate with  @Transactional(propagation = Propagation.REQUIRES_NEW) so that if any fail occurs at DB level then transaction will roll back automatically
  /**
   * This method is responsible to transfer fund from one account to another account
   * @param transferFundRequest
   * @return TransferFundResponseDto
   */
  public TransferFundResponseDto transferFund(TransferFundRequestDto transferFundRequest) {
	  
	 Account fromAccount = this.accountsRepository.getAccount(transferFundRequest.getFromAccountNumber());
	 Account toAccount = this.accountsRepository.getAccount(transferFundRequest.getToAccountNumber());
	 BigDecimal amount = transferFundRequest.getAmount();
	 
	 //create lock for from account so that no other transaction will happened with from account
	 String fromAccountId = fromAccount.getAccountId().intern();
	 String toAccountId = toAccount.getAccountId().intern();
	 Lock lock = getLock(fromAccountId);
	 Lock lock2 = getLock(toAccountId);
	 try {
		 //lock both account so that no other thread will modify the balance of both account
         lock.lock();
         lock2.lock();
         if(amount.intValueExact() <= 0) {
			 throw new BadRequestException("Amount must be positive value.");
		 }	
         
         log.debug("Balance before transfer from account {} to account {}" + 
        		 				fromAccount.getBalance(),toAccount.getBalance()); 
	     //createTransactions
         withdraw(fromAccount, amount);
         deposit(toAccount, amount);  
	    
         log.debug("Balance after transfer from account {} to account {}" + 
        		 				fromAccount.getBalance(),toAccount.getBalance());         
        //Send notification to both account
         sendFundTransferNotification(fromAccount,toAccount,amount);
	     return FundTransferResponseMapper.mapResponse(fromAccountId, fromAccountId, amount, fromAccount.getBalance());
	     
	    }finally{
	    	lock2.unlock();
	        lock.unlock();
	    }
  }
  
  /**
   * This method is responsible to add the fund & update the balance of beneficiary account.
   * 
   * @param toAccount : Account where fund will be credited.
   * @param amount : amount that will be credited.
   */
  private void deposit(Account toAccount, BigDecimal amount) {	
	  BigDecimal balance = toAccount.getBalance();
	  balance = balance.add(amount);
	  toAccount.setBalance(balance);
  }

  	/**
  	 * This method is responsible to subtract the fund & update the balance of beneficiary account.
  	 * 
  	 * @param fromAccount : Account from which fund will be debited
  	 * @param amount : total amount to debited.
  	 */
	private void withdraw(Account fromAccount, BigDecimal amount) {
		BigDecimal balance = fromAccount.getBalance();		
		if(balance.compareTo(amount) == -1) {
			throw new InsufficientFundException(String.format("You do not have sufficient fund to transfer amount %f", amount));			
		}
		 balance = balance.subtract(amount);
		 fromAccount.setBalance(balance);
	}
	
	private void sendFundTransferNotification(Account fromAccount,Account toAccount,BigDecimal amount) {
		//Send notification to fund transferrer
		 emailNotificationService.notifyAboutTransfer(fromAccount, 
				 String.format("Amount debited %f from your account to %s. Your current balance is %f", 
						 amount,
						 toAccount.getAccountId(),
						 fromAccount.getBalance()));
		 //Send notification to beneficiary
		 emailNotificationService.notifyAboutTransfer(toAccount, 
				 String.format("Amount credited %f by %s to your account %s. Your current balance is %f", 
						 amount,
						 fromAccount.getAccountId(),
						 toAccount.getAccountId(),
						 toAccount.getBalance()));
	}

	private ReentrantLock getLock(String id) {
	      synchronized (locks) {
	          ReentrantLock lock = locks.get(id);
	          if (lock == null) {
	              lock = new ReentrantLock();
	              locks.put(id, lock);
	          }
	          return lock;
	      }
	    }
  
}
