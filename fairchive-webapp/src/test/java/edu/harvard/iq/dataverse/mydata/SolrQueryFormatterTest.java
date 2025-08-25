/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.mydata;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author rmp553
 */
public class SolrQueryFormatterTest {

    private Long[] getListOfLongs(int listCount) {
        Long[] array = new Long[listCount];
        for (long a = 0; a < array.length; a++) {
            array[(int) a] = a + 1;
        }
        return array;
    }

    /**
     * Test of buildIdQuery method, of class SolrQueryFormatter.
     */
    @Test
    public void testBasics() {
        SolrQueryFormatter sqf = new SolrQueryFormatter();
        sqf.setSolrIdGroupSize(10);

        String paramName = "entityId";
       
        makeQueryTest2(sqf, 10, paramName, 1);

        makeQueryTest2(sqf, 11, paramName, 2);

        makeQueryTest2(sqf, 21, paramName, 3);

        makeQueryTest(sqf, 0, paramName, null);

        sqf.setSolrIdGroupSize(3);
        String expectedResult = "(parentId:(1 2 3)) OR (parentId:(4 5 6)) OR (parentId:(7 8 9)) OR (parentId:(10 11))";
        makeQueryTest2(sqf, 11, "parentId", 4);

    }

    private void makeQueryTest2(SolrQueryFormatter sqf, int numIds, String paramName, int numParamOccurrences) {

        Long[] idList = this.getListOfLongs(numIds);
        Set<Long> idListSet = new HashSet<>(Arrays.asList(idList));

        String queryClause = sqf.buildIdQuery(idListSet, paramName, null);
        assertEquals(StringUtils.countMatches(queryClause, paramName), numParamOccurrences);
    }

    private void makeQueryTest(SolrQueryFormatter sqf, int numIds, String paramName, String expectedQuery) {

        Long[] idList = this.getListOfLongs(numIds);
        Set<Long> idListSet = new HashSet<>(Arrays.asList(idList));

        String queryClause = sqf.buildIdQuery(idListSet, paramName, null);
        assertEquals(queryClause, expectedQuery);
    }

}
