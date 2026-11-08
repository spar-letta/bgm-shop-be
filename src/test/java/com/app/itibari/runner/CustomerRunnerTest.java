//package com.app.itibari.runner;
//
//import com.app.itibari.ItibariApplicationTests;
//import com.app.itibari.models.entities.Customers;
//import com.app.itibari.repository.CustomersRepository;
//import com.app.itibari.services.CustomersService;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.MockitoJUnitRunner;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Sort;
//
//import java.util.List;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNull;
//import static org.mockito.Mockito.*;
//
//@RunWith(MockitoJUnitRunner.class)
//public class CustomerRunnerTest extends ItibariApplicationTests {
//    @Mock
//    private CustomersRepository customersRepository;
//    @Mock
//    private CustomersService customersService;
//    @InjectMocks
//    private CustomerRunner runner;
//
//    @Test
//    public void normalizesAllContactNumbersAcrossPagesAndSavesOnlyChanges() throws Exception {
//        Customers changed = new Customers();
//        changed.setPhoneNumber("0714394472");
//        changed.setOwnerNumber("255714394473");
//        changed.setShopKeeperNumber(" 0714-394-474 ");
//        Customers normalized = new Customers();
//        normalized.setPhoneNumber("+255714394475");
//        Customers empty = new Customers();
//        empty.setOwnerNumber("");
//        empty.setShopKeeperNumber("   ");
//
//        PageRequest firstPage = PageRequest.of(0, 500, Sort.by("id"));
//        PageRequest secondPage = firstPage.next();
//        // A full first page signals that another page must be read.
//        List<Customers> firstCustomers = new java.util.ArrayList<>(java.util.Collections.nCopies(499, normalized));
//        firstCustomers.add(changed);
//        when(customersRepository.findByDeletedIsFalse(firstPage))
//                .thenReturn(new PageImpl<>(firstCustomers, firstPage, 501));
//        when(customersRepository.findByDeletedIsFalse(secondPage))
//                .thenReturn(new PageImpl<>(List.of(empty), secondPage, 501));
//
//        runner.run();
//
//        assertEquals("+255714394472", changed.getPhoneNumber());
//        assertEquals("+255714394473", changed.getOwnerNumber());
//        assertEquals("+255714394474", changed.getShopKeeperNumber());
//        assertNull(empty.getPhoneNumber());
//        assertEquals("", empty.getOwnerNumber());
//        assertEquals("   ", empty.getShopKeeperNumber());
//        verify(customersRepository).findByDeletedIsFalse(secondPage);
//        verify(customersRepository).saveAll(List.of(changed));
//
//        runner.run();
//
//        // Running again must not write already normalized records.
//        verify(customersRepository, times(1)).saveAll(any());
//    }
//}
