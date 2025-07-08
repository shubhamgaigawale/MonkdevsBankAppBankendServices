package com.monkdevs.accounts.service.impl;

import com.monkdevs.accounts.dto.AccountsDto;
import com.monkdevs.accounts.dto.CardsDto;
import com.monkdevs.accounts.dto.CustomerDetailsDto;
import com.monkdevs.accounts.dto.LoansDto;
import com.monkdevs.accounts.entity.Accounts;
import com.monkdevs.accounts.entity.Customer;
import com.monkdevs.accounts.exception.ResourceNotFoundException;
import com.monkdevs.accounts.mapper.AccountsMapper;
import com.monkdevs.accounts.mapper.CustomerMapper;
import com.monkdevs.accounts.repository.AccountsRepository;
import com.monkdevs.accounts.repository.CustomerRepository;
import com.monkdevs.accounts.service.ICustomersService;
import com.monkdevs.accounts.service.client.CardsFeignClient;
import com.monkdevs.accounts.service.client.LoansFeignClient;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomersServiceImpl implements ICustomersService {

    private AccountsRepository accountsRepository;
    private CustomerRepository customerRepository;
    private CardsFeignClient cardsFeignClient;
    private LoansFeignClient loansFeignClient;

    /**
     * @param mobileNumber - Input Mobile Number
     * @return Customer Details based on a given mobileNumber
     */
    @Override
    public CustomerDetailsDto fetchCustomerDetails(String mobileNumber, String correlationId)
    {
        Customer customer = customerRepository.findByMobileNumber(mobileNumber).orElseThrow(
                () -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber)
        );
        Accounts accounts = accountsRepository.findByCustomerId(customer.getCustomerId()).orElseThrow(
                () -> new ResourceNotFoundException("Account", "customerId", customer.getCustomerId().toString())
        );

        CustomerDetailsDto customerDetailsDto = CustomerMapper.mapToCustomerDetailsDto(customer, new CustomerDetailsDto());
        customerDetailsDto.setAccountsDto(AccountsMapper.mapToAccountsDto(accounts, new AccountsDto()));

        ResponseEntity<LoansDto> loansDtoResponseEntity = loansFeignClient.fetchLoanDetails(correlationId, mobileNumber);

        if (loansDtoResponseEntity != null)
        {
            customerDetailsDto.setLoansDto(loansDtoResponseEntity.getBody());
        }

        ResponseEntity<CardsDto> cardsDtoResponseEntity = cardsFeignClient.fetchCardDetails(correlationId, mobileNumber);

        if (cardsDtoResponseEntity != null)
        {
            customerDetailsDto.setCardsDto(cardsDtoResponseEntity.getBody());
        }

        return customerDetailsDto;

    }
}