package com.pinterest.orion.core.automation.sensor.kafka;

import com.pinterest.orion.core.kafka.Brokerset;
import com.pinterest.orion.core.kafka.Brokerset.BrokersetRange;
import com.pinterest.orion.core.kafka.BrokersetTest;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class KafkaClusterInfoSensorTest {
    private String pwdPath = "src/test/java/com/pinterest/orion/core/automation/sensor/kafka/testFiles/";

    @Test
    public void whenBrokersetFileDoesntExistShouldThrow() {
        File brokersetFile = new File(pwdPath + "doesntexist.json");

        Throwable exception = assertThrows(IOException.class,
                () -> KafkaClusterInfoSensor.loadBrokersetsFromFile(brokersetFile));
        assertThat(exception.getMessage()).contains("Problem found when reading " + brokersetFile);
        assertThat(exception.getCause().getMessage()).contains("No such file or directory");
    }

    @Test
    public void whenLoadingBrokersetWithBadRangeShouldThrow() {
        File brokersetFile = new File(pwdPath + "brokersetWithBadRange.json");

        Throwable exception = assertThrows(IOException.class,
                () -> KafkaClusterInfoSensor.loadBrokersetsFromFile(brokersetFile));
        assertThat(exception.getMessage()).contains("Problem found when reading " + brokersetFile);
        assertThat(exception.getCause().getMessage()).contains("Brokerset Ranges must come in pairs where 'startBrokerIdx' <= 'endBrokerIdx'");
    }

    @Test
    public void testLoadBrokersetSimple() throws IOException {
        File brokersetFile = new File(pwdPath + "brokersets.json");
        File overridesFile = new File(pwdPath + "brokersetOverrides.json");
        List<Brokerset> result = KafkaClusterInfoSensor.loadBrokersetsFromFile(brokersetFile, overridesFile);

        List<BrokersetRange> entries = new ArrayList<>();
        entries.add(new BrokersetRange(4, 6));
        Brokerset brokerset = new Brokerset("Capacity_B3_P3_0", entries, 3);
        assertTrue(BrokersetTest.realBrokersetEquality(brokerset, result.get(0)));
    }

    @Test
    public void testLoadBrokerset() throws IOException {
        File brokersetFile = new File(pwdPath + "brokersets2.json");
        File overridesFile = new File(pwdPath + "brokersetOverrides2.json");
        List<Brokerset> result = KafkaClusterInfoSensor.loadBrokersetsFromFile(brokersetFile, overridesFile);

        List<BrokersetRange> entries1 = new ArrayList<>();
        entries1.add(new BrokersetRange(1, 1));
        entries1.add(new BrokersetRange(10, 10));
        entries1.add(new BrokersetRange(13, 14));
        entries1.add(new BrokersetRange(7, 7));
        Brokerset brokerset1 = new Brokerset("Capacity_B5_P5_0", entries1, 5);

        List<BrokersetRange> entries2 = new ArrayList<>();
        entries2.add(new BrokersetRange(1, 1));
        entries2.add(new BrokersetRange(10, 11));
        Brokerset brokerset2 = new Brokerset("Capacity_B3_P3_0", entries2, 3);
        assertTrue(BrokersetTest.realBrokersetEquality(brokerset1, result.get(0)));
        assertTrue(BrokersetTest.realBrokersetEquality(brokerset2, result.get(1)));
    }

    @Test
    public void whenMappingInRangeDoesntMatchMappingOutRangeShouldThrow() {
        File brokersetFile = new File(pwdPath + "brokersets.json");
        File overridesFile = new File(pwdPath + "mismatchedRangeOverrides.json");
        Throwable exception = assertThrows(IOException.class,
                () -> KafkaClusterInfoSensor.loadBrokersetsFromFile(brokersetFile, overridesFile));
        assertThat(exception.getMessage()).contains("Problem found when reading " + overridesFile);
        assertThat(exception.getCause().getMessage()).contains("The override map file included mismatched ranges");
    }

    @Test
    public void whenMissingFieldsInJson() {
        File brokersetFile = new File(pwdPath + "brokersets.json");
        File overridesFile = new File(pwdPath + "halfFormedRangeOverride.json");

        Throwable exception = assertThrows(IOException.class,
                () -> KafkaClusterInfoSensor.loadBrokersetsFromFile(brokersetFile, overridesFile));
        assertThat(exception.getMessage()).contains("Problem found when reading " + overridesFile);
        assertThat(exception.getCause().getMessage()).contains("There was a problem parsing the Brokerset Override");
    }

    @Test
    public void whenMissingAllFieldsInJson() {
        File brokersetFile = new File(pwdPath + "brokersets.json");
        File overridesFile = new File(pwdPath + "emptyOverrideWithBraces.json");

        Throwable exception = assertThrows(IOException.class,
                () -> KafkaClusterInfoSensor.loadBrokersetsFromFile(brokersetFile, overridesFile));
        assertThat(exception.getMessage()).contains("Problem found when reading " + overridesFile);
        assertThat(exception.getCause().getMessage()).contains("There was a problem parsing the Brokerset Override");
    }

    @Test
    public void whenOverrideIsEmpty() throws IOException {
        File brokersetFile = new File(pwdPath + "brokersets.json");
        File overridesFile = new File(pwdPath + "trueEmptyOverride.json");
        List<Brokerset> result = KafkaClusterInfoSensor.loadBrokersetsFromFile(brokersetFile, overridesFile);

        List<BrokersetRange> entries = new ArrayList<>();
        entries.add(new BrokersetRange(1, 3));
        Brokerset brokerset = new Brokerset("Capacity_B3_P3_0", entries, 3);
        assertTrue(BrokersetTest.realBrokersetEquality(brokerset, result.get(0)));
    }
}