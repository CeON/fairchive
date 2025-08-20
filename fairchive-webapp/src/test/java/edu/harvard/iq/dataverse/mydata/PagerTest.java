/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.mydata;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author rmp553
 */
public class PagerTest {
    private Pager pager1;

    public PagerTest() {
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp() {
        this.pager1 = new Pager(100, 10, 1);
    }

    @AfterEach
    public void tearDown() {
    }


    /**
     * Test of getNumResults method, of class Pager.
     */
    @Test
    public void testBasics() {
        pager1 = new Pager(102, 10, 1);

        assertEquals(true, pager1.isPagerNecessary());

        assertEquals(102, pager1.getNumResults());
        assertEquals(1, pager1.getPreviousPageNumber());
        assertEquals(2, pager1.getNextPageNumber());
        assertEquals(false, pager1.hasPreviousPageNumber());
        assertEquals(true, pager1.hasNextPageNumber());

        assertEquals(1, pager1.getPageNumberList()[0]);
        assertEquals(5, pager1.getPageNumberList()[4]);

        assertEquals(1, pager1.getStartCardNumber());
        assertEquals(10, pager1.getEndCardNumber());

        pager1 = new Pager(102, 10, 6);

        assertEquals(4, pager1.getPageNumberList()[0]);
        assertEquals(8, pager1.getPageNumberList()[4]);

        pager1 = new Pager(100, 10, 9);
        assertEquals(6, pager1.getPageNumberList()[0]);
        assertEquals(10, pager1.getPageNumberList()[4]);

        pager1 = new Pager(100, 10, 10);
        assertEquals(6, pager1.getPageNumberList()[0]);
        assertEquals(10, pager1.getPageNumberList()[4]);

        pager1 = new Pager(102, 10, 9);
        assertEquals(7, pager1.getPageNumberList()[0]);
        assertEquals(11, pager1.getPageNumberList()[4]);
    }

    @Test
    public void testNoResults() {
        pager1 = new Pager(0, 10, 1);

        assertEquals(false, pager1.isPagerNecessary());

        assertEquals(0, pager1.getNumResults());
        assertEquals(0, pager1.getPreviousPageNumber());
        assertEquals(0, pager1.getNextPageNumber());
        assertEquals(false, pager1.hasPreviousPageNumber());
        assertEquals(false, pager1.hasNextPageNumber());

        assertEquals(0, pager1.getStartCardNumber());
        assertEquals(0, pager1.getEndCardNumber());
    }
}
