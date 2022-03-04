package com.pinterest.orion.core.kafka;

import org.junit.Test;

import java.util.ArrayList;

import static com.pinterest.orion.core.kafka.Brokerset.BrokersetRange;
import static com.pinterest.orion.core.kafka.Brokerset.BrokersetRangeMap;
import static org.junit.Assert.*;

public class BrokersetTest {

    @Test
    public void brokersetRangeShould() {

    }

    @Test
    public void rangeStartShouldAlwaysBeLessThanEndElseThrow()
    {
        assertThrows(IllegalArgumentException.class, () -> new BrokersetRange(5, 1));
    }

    @Test
    public void testNoOverlap()
    {
        BrokersetRange range1 = new BrokersetRange(5, 10);
        BrokersetRange range2 = new BrokersetRange(1, 4);
        assertFalse(range1.overlaps(range2));
        assertFalse(range2.overlaps(range1));
    }

    @Test
    public void testSuperset()
    {
        BrokersetRange range1 = new BrokersetRange(5, 10);
        BrokersetRange range2 = new BrokersetRange(6, 9);
        assertTrue(range1.overlaps(range2));
        assertTrue(range2.overlaps(range1));
    }

    @Test
    public void testEdgeOverlap()
    {
        BrokersetRange range1 = new BrokersetRange(5, 10);
        BrokersetRange range2 = new BrokersetRange(1, 5);
        assertTrue(range1.overlaps(range2));
        assertTrue(range2.overlaps(range1));
    }

    @Test
    public void testPartOverlap()
    {
        BrokersetRange range1 = new BrokersetRange(5, 10);
        BrokersetRange range2 = new BrokersetRange(1, 8);
        assertTrue(range1.overlaps(range2));
        assertTrue(range2.overlaps(range1));
    }

    @Test
    public void testBasicBrokersetMapping() {
        ArrayList<BrokersetRange> brokersetRanges = new ArrayList<>();
        brokersetRanges.add(new BrokersetRange(1, 3));
        Brokerset brokerset = new Brokerset("testBrokerset", brokersetRanges, 3);
        BrokersetRangeMap map = new BrokersetRangeMap(1,3, 4, 6);

        Brokerset mappedBrokerset = brokerset.applyBrokerOverrides(map);

        ArrayList<BrokersetRange> otherRanges = new ArrayList<>();
        otherRanges.add(new BrokersetRange(4, 6));
        Brokerset comparisonBrokerset = new Brokerset("testBrokerset", otherRanges, 3);
        assertTrue(realBrokersetEquality(mappedBrokerset, comparisonBrokerset));
    }

    @Test
    public void testLeftIntersect() {
        ArrayList<BrokersetRange> brokersetRanges = new ArrayList<>();
        brokersetRanges.add(new BrokersetRange(1, 5));
        Brokerset brokerset = new Brokerset("testBrokerset", brokersetRanges, 3);
        BrokersetRangeMap map = new BrokersetRangeMap(3,5, 10, 12);

        Brokerset mappedBrokerset = brokerset.applyBrokerOverrides(map);

        ArrayList<BrokersetRange> otherRanges = new ArrayList<>();
        otherRanges.add(new BrokersetRange(1, 2));
        otherRanges.add(new BrokersetRange(10, 12));
        Brokerset comparisonBrokerset = new Brokerset("testBrokerset", otherRanges, 3);
        assertTrue(realBrokersetEquality(mappedBrokerset, comparisonBrokerset));
    }

    @Test
    public void testRightIntersect() {
        ArrayList<BrokersetRange> brokersetRanges = new ArrayList<>();
        brokersetRanges.add(new BrokersetRange(1, 5));
        Brokerset brokerset = new Brokerset("testBrokerset", brokersetRanges, 3);
        BrokersetRangeMap map = new BrokersetRangeMap(1,3, 10, 12);

        Brokerset mappedBrokerset = brokerset.applyBrokerOverrides(map);

        ArrayList<BrokersetRange> otherRanges = new ArrayList<>();
        otherRanges.add(new BrokersetRange(10, 12));
        otherRanges.add(new BrokersetRange(4, 5));
        Brokerset comparisonBrokerset = new Brokerset("testBrokerset", otherRanges, 3);
        assertTrue(realBrokersetEquality(mappedBrokerset, comparisonBrokerset));
    }

    @Test
    public void testBothIntersect() {
        ArrayList<BrokersetRange> brokersetRanges = new ArrayList<>();
        brokersetRanges.add(new BrokersetRange(1, 5));
        Brokerset brokerset = new Brokerset("testBrokerset", brokersetRanges, 3);
        BrokersetRangeMap map = new BrokersetRangeMap(2,4, 10, 12);

        Brokerset mappedBrokerset = brokerset.applyBrokerOverrides(map);

        ArrayList<BrokersetRange> otherRanges = new ArrayList<>();
        otherRanges.add(new BrokersetRange(1, 1));
        otherRanges.add(new BrokersetRange(10, 12));
        otherRanges.add(new BrokersetRange(5, 5));
        Brokerset comparisonBrokerset = new Brokerset("testBrokerset", otherRanges, 3);
        assertTrue(realBrokersetEquality(mappedBrokerset, comparisonBrokerset));
    }

    @Test
    public void testNeitherIntersect() {
        ArrayList<BrokersetRange> brokersetRanges = new ArrayList<>();
        brokersetRanges.add(new BrokersetRange(2, 4));
        Brokerset brokerset = new Brokerset("testBrokerset", brokersetRanges, 3);
        BrokersetRangeMap map = new BrokersetRangeMap(1,5, 10, 14);

        Brokerset mappedBrokerset = brokerset.applyBrokerOverrides(map);

        ArrayList<BrokersetRange> otherRanges = new ArrayList<>();
        otherRanges.add(new BrokersetRange(11, 13));
        Brokerset comparisonBrokerset = new Brokerset("testBrokerset", otherRanges, 3);
        assertTrue(realBrokersetEquality(mappedBrokerset, comparisonBrokerset));
    }

    public static Boolean realBrokersetEquality(Brokerset one, Brokerset two) {
        if (!one.getBrokersetAlias().equals(two.getBrokersetAlias())) {
            return false;
        }
        if (!(one.getPartitions() == two.getPartitions())) {
            return false;
        }
        if (!one.getEntries().equals(two.getEntries())) {
            return false;
        }
        return true;
    }
}