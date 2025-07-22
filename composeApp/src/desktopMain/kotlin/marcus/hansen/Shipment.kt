package marcus.hansen

open class Shipment(
    val id: String,
    val type: ShipmentType = ShipmentType.STANDARD,
    val createdTimestamp: Long = System.currentTimeMillis()
) {
    var status: String = "Unknown"
    var expectedDeliveryDateTimestamp: Long? = null
    var currentLocation: String? = null
    val notes: MutableList<String> = mutableListOf()
    val updateHistory: MutableList<ShippingUpdate> = mutableListOf()
    val ruleViolations: MutableList<String> = mutableListOf()

    private val observers: MutableList<ShipmentObserver> = mutableListOf()

    fun addNote(note: String) {
        notes.add(note)
    }

    fun addUpdate(update: ShippingUpdate) {
        updateHistory.add(update)
    }

    fun addObserver(observer: ShipmentObserver) {
        observers.add(observer)
    }

    fun removeObserver(observer: ShipmentObserver) {
        observers.remove(observer)
    }

    fun notifyObservers() {
        observers.forEach { it.update(this) }
    }

    fun getImmutableNotes(): List<String> = notes.toList()
    fun getImmutableUpdateHistory(): List<ShippingUpdate> = updateHistory.toList()
}