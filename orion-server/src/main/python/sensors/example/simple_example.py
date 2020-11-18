from com.pinterest.orion.core.automation.sensor import Sensor

class SimpleSensor(Sensor):
    def observe(self, cluster):
        print cluster
        
sensor = SimpleSensor()
