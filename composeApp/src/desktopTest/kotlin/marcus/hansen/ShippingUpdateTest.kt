package marcus.hansen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.test.assertContains

class ShippingUpdateTest {

    @Test
    fun `fromString should correctly parse a 'created' update line`() {
        val line = "created,1243234,1999283774"
        val update = ShippingUpdate.fromString(line)

        assertEquals("created", update.updateType)
        assertEquals("1243234", update.shipmentId)
        assertEquals(1999283774L, update.timestamp)
        assertTrue(update.otherInfo.isEmpty())
    }

    @Test
    fun `fromString should correctly parse a 'shipped' update line with expected delivery timestamp`() {
        val line = "shipped,1243234,1999437478,19999488398"
        val update = ShippingUpdate.fromString(line)

        assertEquals("shipped", update.updateType)
        assertEquals("1243234", update.shipmentId)
        assertEquals(1999437478L, update.timestamp)
        assertEquals(1, update.otherInfo.size)
        assertEquals("19999488398", update.otherInfo[0]) // The final value is the timestamp of the expected delivery date
    }

    @Test
    fun `fromString should correctly parse a 'location' update line with location info`() {
        val line = "location,1243234,1983893498,Los Angeles CA"
        val update = ShippingUpdate.fromString(line)

        assertEquals("location", update.updateType)
        assertEquals("1243234", update.shipmentId)
        assertEquals(1983893498L, update.timestamp)
        assertEquals(1, update.otherInfo.size)
        assertEquals("Los Angeles CA", update.otherInfo[0]) // otherInfo is the location string
    }

    @Test
    fun `fromString should correctly parse a 'delivered' update line`() {
        val line = "delivered,1243234,1999345988"
        val update = ShippingUpdate.fromString(line)

        assertEquals("delivered", update.updateType)
        assertEquals("1243234", update.shipmentId)
        assertEquals(1999345988L, update.timestamp)
        assertTrue(update.otherInfo.isEmpty())
    }

    @Test
    fun `fromString should correctly parse a 'delayed' update line with new expected delivery timestamp`() {
        val line = "delayed,1243234,1999948389,19999387834"
        val update = ShippingUpdate.fromString(line)

        assertEquals("delayed", update.updateType)
        assertEquals("1243234", update.shipmentId)
        assertEquals(1999948389L, update.timestamp)
        assertEquals(1, update.otherInfo.size)
        assertEquals("19999387834", update.otherInfo[0]) // otherInfo is the new expected delivery date timestamp
    }

    @Test
    fun `fromString should correctly parse a 'lost' update line`() {
        val line = "lost,1243244,1000034873"
        val update = ShippingUpdate.fromString(line)

        assertEquals("lost", update.updateType)
        assertEquals("1243244", update.shipmentId)
        assertEquals(1000034873L, update.timestamp)
        assertTrue(update.otherInfo.isEmpty())
    }

    @Test
    fun `fromString should correctly parse a 'canceled' update line`() {
        // Correcting the typo from source 26 to be a valid Long for the test
        val correctedLine = "canceled,1243334,190993487"
        val update = ShippingUpdate.fromString(correctedLine)

        assertEquals("canceled", update.updateType)
        assertEquals("1243334", update.shipmentId)
        assertEquals(190993487L, update.timestamp)
        assertTrue(update.otherInfo.isEmpty())
    }

    @Test
    fun `fromString should correctly parse a 'noteadded' update line with note content`() {
        val line = "noteadded,1243234,190000495,packaging was damaged slightly during shipping"
        val update = ShippingUpdate.fromString(line)

        assertEquals("noteadded", update.updateType)
        assertEquals("1243234", update.shipmentId)
        assertEquals(190000495L, update.timestamp)
        assertEquals(1, update.otherInfo.size)
        assertEquals("packaging was damaged slightly during shipping", update.otherInfo[0])
    }

    @Test
    fun `fromString should handle leading and trailing whitespace in parts`() {
        val line = "  created  ,  1243234  ,  1999283774  "
        val update = ShippingUpdate.fromString(line)

        assertEquals("created", update.updateType)
        assertEquals("1243234", update.shipmentId)
        assertEquals(1999283774L, update.timestamp)
        assertTrue(update.otherInfo.isEmpty())
    }

    @Test
    fun `fromString should throw IllegalArgumentException for malformed line missing timestamp`() {
        val line = "created,1243234"
        val exception = assertFailsWith<IllegalArgumentException> {
            ShippingUpdate.fromString(line)
        }
        assertContains(exception.message ?: "", "Invalid update line format")
    }

    @Test
    fun `fromString should throw IllegalArgumentException for malformed line missing shipment ID`() {
        val line = "created"
        val exception = assertFailsWith<IllegalArgumentException> {
            ShippingUpdate.fromString(line)
        }
        assertContains(exception.message ?: "", "Invalid update line format")
    }

    @Test
    fun `fromString should throw NumberFormatException for invalid timestamp`() {
        val line = "created,1243234,not_a_number"
        assertFailsWith<NumberFormatException> {
            ShippingUpdate.fromString(line)
        }
    }

    @Test
    fun `fromString should handle multiple otherInfo parts`() {
        val line = "custom,ID001,123456789,part1,part2,part3"
        val update = ShippingUpdate.fromString(line)

        assertEquals("custom", update.updateType)
        assertEquals("ID001", update.shipmentId)
        assertEquals(123456789L, update.timestamp)
        assertEquals(3, update.otherInfo.size)
        assertEquals("part1", update.otherInfo[0])
        assertEquals("part2", update.otherInfo[1])
        assertEquals("part3", update.otherInfo[2])
    }

    @Test
    fun `fromString should handle empty otherInfo when commas are present at end`() {
        val line = "test,123,456," // Line ends with a comma
        val update = ShippingUpdate.fromString(line)

        assertEquals("test", update.updateType)
        assertEquals("123", update.shipmentId)
        assertEquals(456L, update.timestamp)
        assertEquals(1, update.otherInfo.size)
        assertEquals("", update.otherInfo[0]) // An empty string for the last part
    }
}