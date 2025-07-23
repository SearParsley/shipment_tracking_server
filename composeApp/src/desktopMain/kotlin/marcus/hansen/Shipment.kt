package marcus.hansen

open class Shipment (
    val id: String,
    val type: ShipmentType = ShipmentType.STANDARD,
    val createdTimestamp: Long = System.currentTimeMillis()
) : Subject {
    var status: String = "Unknown"
    var expectedDeliveryDateTimestamp: Long? = null
    var currentLocation: String? = null
    val notes: MutableList<String> = mutableListOf()
    val updateHistory: MutableList<ShippingUpdate> = mutableListOf()
    val ruleViolations: MutableList<String> = mutableListOf()

    private val observers: MutableList<Observer> = mutableListOf()

    fun addNote(note: String) {
        notes.add(note)
    }

    fun addUpdate(update: ShippingUpdate) {
        updateHistory.add(update)
    }

    override fun addObserver(observer: Observer) {
        observers.add(observer)
    }

    override fun removeObserver(observer: Observer) {
        observers.remove(observer)
    }

    override fun notifyObservers() {
        observers.forEach { it.update(this) }
    }

    fun getImmutableNotes(): List<String> = notes.toList()
    fun getImmutableUpdateHistory(): List<ShippingUpdate> = updateHistory.toList()
}