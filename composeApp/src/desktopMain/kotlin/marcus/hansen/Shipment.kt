package marcus.hansen

class Shipment(val id: String) {
    // Attributes with their desired visibility for setters
    var status: String = "Unknown"
        internal set // Setter is internal
    var expectedDeliveryDateTimestamp: Long? = null
        internal set
    var currentLocation: String? = null
        internal set
    val notes: MutableList<String> = mutableListOf()
    val updateHistory: MutableList<ShippingUpdate> = mutableListOf()

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

    // getters return immutable versions of the lists
    fun getImmutableNotes(): List<String> = notes.toList()
    fun getImmutableUpdateHistory(): List<ShippingUpdate> = updateHistory.toList()
}