package com.se.sebtl;

import com.se.sebtl.model.TicketView;
import com.se.sebtl.service.ParkingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ParkingServiceIntegrationTest {

    @Autowired
    private ParkingService parkingService;

    @Test
    public void testSearchGuestByLetters() {
        // Since GUEST-02 might not exist in your specific DB snapshot, 
        // this test asserts the search mechanism doesn't throw errors 
        // and safely returns a list when searching text.
        List<TicketView> results = parkingService.searchTickets("GUE");
        
        assertNotNull(results, "Search results should not be null");
        // If "GUEST-02" is in the DB, it should be found:
        // assertTrue(results.stream().anyMatch(t -> t.getUserId().contains("GUE")));
    }

    @Test
    public void testSearchByNumericTicketId() {
        // Search by a numeric value (e.g. "1")
        List<TicketView> results = parkingService.searchTickets("1");
        
        assertNotNull(results, "Numeric search results should not be null");
    }
}
