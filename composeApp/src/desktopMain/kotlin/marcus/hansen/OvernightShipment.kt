package marcus.hansen

class OvernightShipment(id: String, createdTimestamp: Long) : Shipment(id, ShipmentType.OVERNIGHT, createdTimestamp)