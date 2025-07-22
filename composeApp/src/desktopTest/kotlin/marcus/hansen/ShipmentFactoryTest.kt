package marcus.hansen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class ShipmentFactoryTest {

    @Test
    fun `createShipment should create a StandardShipment by default`() {
        val shipmentId = "SF_001"
        val shipment = ShipmentFactory.createShipment(shipmentId, "Standard")

        assertNotNull(shipment)
        assertEquals(shipmentId, shipment.id)
        assertEquals(ShipmentType.STANDARD, shipment.type)
        assertTrue(shipment is StandardShipment)
        assertTrue(shipment.createdTimestamp > 0) // Check that timestamp is set
    }

    @Test
    fun `createShipment should create an ExpressShipment`() {
        val shipmentId = "SF_002"
        val shipment = ShipmentFactory.createShipment(shipmentId, "EXPRESS")

        assertNotNull(shipment)
        assertEquals(shipmentId, shipment.id)
        assertEquals(ShipmentType.EXPRESS, shipment.type)
        assertTrue(shipment is ExpressShipment)
    }

    @Test
    fun `createShipment should create an OvernightShipment`() {
        val shipmentId = "SF_003"
        val shipment = ShipmentFactory.createShipment(shipmentId, "OVERNIGHT")

        assertNotNull(shipment)
        assertEquals(shipmentId, shipment.id)
        assertEquals(ShipmentType.OVERNIGHT, shipment.type)
        assertTrue(shipment is OvernightShipment)
    }

    @Test
    fun `createShipment should create a BulkShipment`() {
        val shipmentId = "SF_004"
        val shipment = ShipmentFactory.createShipment(shipmentId, "BULK")

        assertNotNull(shipment)
        assertEquals(shipmentId, shipment.id)
        assertEquals(ShipmentType.BULK, shipment.type)
        assertTrue(shipment is BulkShipment)
    }

    @Test
    fun `createShipment should be case-insensitive for typeString`() {
        val shipmentId = "SF_005"
        val shipment = ShipmentFactory.createShipment(shipmentId, "oVeRnIgHt")

        assertNotNull(shipment)
        assertEquals(ShipmentType.OVERNIGHT, shipment.type)
        assertTrue(shipment is OvernightShipment)
    }

    @Test
    fun `createShipment should throw IllegalArgumentException for invalid typeString`() {
        val shipmentId = "SF_006"
        val exception = assertFailsWith<IllegalArgumentException> {
            ShipmentFactory.createShipment(shipmentId, "INVALID_TYPE")
        }
        assertEquals("Invalid shipment type string: INVALID_TYPE. Must be STANDARD, EXPRESS, OVERNIGHT, or BULK.", exception.message)
    }
}