package marcus.hansen

open class Shipment(open val id: String) {
    open var status: String = "Unknown"
        internal set
    open var expectedDeliveryDateTimestamp: Long? = null
        internal set
    open var currentLocation: String? = null
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

    open fun addObserver(observer: ShipmentObserver) {
        observers.add(observer)
    }

    open fun removeObserver(observer: ShipmentObserver) {
        observers.remove(observer)
    }

    open fun notifyObservers() {
        observers.forEach { it.update(this) }
    }

    open fun getImmutableNotes(): List<String> = notes.toList()
    open fun getImmutableUpdateHistory(): List<ShippingUpdate> = updateHistory.toList()
}